/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.properties;

import com.epam.datalab.exceptions.DynamicChangePropertiesException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ChangePropertiesService {

    public String readFileAsString(String selfServicePropPath, String serviceName) {
        try {
            log.info("Trying to read {}, file from path {} :", serviceName, selfServicePropPath);
            String currentConf = FileUtils.readFileToString(new File(selfServicePropPath), Charset.defaultCharset());
            return hideSecretsAndRemoveLicence(currentConf);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new DynamicChangePropertiesException(String.format("Failed while read file %s", serviceName));
        }
    }


    public void writeFileFromString(String newPropFile, String serviceName, String servicePath) {
        String oldFile = readFile(serviceName, servicePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(servicePath))) {
            try {
                //            changeCHMODE(serviceName, servicePath, ChangePropertiesConst.DEFAULT_CHMOD, ChangePropertiesConst.WRITE_CHMOD);
                log.info("Trying to overwrite {}, file for path {} :", serviceName, servicePath);
                writer.write(addLicence());
                writer.write(checkAndReplaceSecretIfEmpty(newPropFile, oldFile));
                log.info("{} overwritten successfully", serviceName);
                writer.close();
                //            changeCHMODE(serviceName, servicePath, ChangePropertiesConst.WRITE_CHMOD, ChangePropertiesConst.DEFAULT_CHMOD);
            } catch (Exception e) {
                log.error("Failed during overwriting {}", serviceName);
                writer.write(oldFile);
                throw new DynamicChangePropertiesException(String.format("Failed during overwriting %s", serviceName));
            }
        } catch (IOException e) {
            log.error("Failed to create writer with path {}", servicePath);
            throw new DynamicChangePropertiesException(String.format("Failed during overwriting %s", serviceName));
        }
    }

    private String readFile(String serviceName, String servicePath) {
        String oldFile;
        try {
            oldFile = FileUtils.readFileToString(new File(servicePath), Charset.defaultCharset());
        } catch (IOException e) {
            log.error("Failed to read with path {}", servicePath);
            throw new DynamicChangePropertiesException(String.format("Failed during overwriting %s", serviceName));

        }
        return oldFile;
    }

    public RestartAnswer restart(RestartForm restartForm) {
        try {
            boolean billing = restartForm.isBilling();
            boolean provserv = restartForm.isProvserv();
            boolean ui = restartForm.isUi();
            String shCommand = buildSHRestartCommand(billing, provserv, ui);
            log.info("Tying to restart ui: {}, provserv: {}, billing: {}, with command: {}", ui,
                    provserv, billing, shCommand);
            Runtime.getRuntime().exec(shCommand).waitFor();
            return RestartAnswer.builder()
                    .billingSuccess(billing)
                    .provservSuccess(provserv)
                    .endpoint(restartForm.getEndpoint())
                    .build();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            return RestartAnswer.builder()
                    .billingSuccess(false)
                    .provservSuccess(false)
                    .endpoint(restartForm.getEndpoint())
                    .build();
        }
    }


    private String hideSecretsAndRemoveLicence(String currentConf) {
        Matcher passMatcher = Pattern.compile(ChangePropertiesConst.SECRET_REGEX).matcher(currentConf);
        Matcher userMatcher = Pattern.compile(ChangePropertiesConst.USER_REGEX).matcher(currentConf);
        List<String> secretsAndUsers = new ArrayList<>();
        String confWithReplacedSecretConf = removeLicence(currentConf);
        while (passMatcher.find()) {
            String secret = passMatcher.group().split(":")[ChangePropertiesConst.DEFAULT_VALUE_PLACE];
            if (!(secret.isEmpty() || secret.trim().isEmpty()))
                secretsAndUsers.add(secret);
        }
        while (userMatcher.find()) {
            String user = userMatcher.group().split(":")[ChangePropertiesConst.DEFAULT_VALUE_PLACE];
            if (!(user.isEmpty() || user.trim().isEmpty()))
                secretsAndUsers.add(user);
        }
        for (String secretOrUser : secretsAndUsers) {
            int start = confWithReplacedSecretConf.indexOf(secretOrUser);
            int end = confWithReplacedSecretConf.indexOf(":", start);
            boolean isTure;
            try {
                String s = confWithReplacedSecretConf.substring(start, end);
                isTure = s.equals(secretOrUser);
            } catch (StringIndexOutOfBoundsException e) {
                isTure = true;
            }
            if (isTure)
                confWithReplacedSecretConf = confWithReplacedSecretConf.replace(secretOrUser, ChangePropertiesConst.SECRET_REPLACEMENT_FORMAT);
        }
        return confWithReplacedSecretConf;
    }

    private String removeLicence(String conf) {
        return conf.split(ChangePropertiesConst.LICENCE_REGEX)[conf.split(ChangePropertiesConst.LICENCE_REGEX).length - 1];
    }


    private void changeCHMODE(String serviceName, String path, String fromMode, String toMode) throws IOException {
        try {
            String command = String.format(ChangePropertiesConst.CHANGE_CHMOD_SH_COMMAND_FORMAT, toMode, path);
            log.info("Trying to change chmod for file {} {}->{}", serviceName, fromMode, toMode);
            log.info("Execute command: {}", command);
            Runtime.getRuntime().exec(command).waitFor();
        } catch (InterruptedException e) {
            log.error("Failed change chmod for file {} {}->{}", serviceName, fromMode, toMode);
        }
    }

    private String addLicence() {
        return ChangePropertiesConst.LICENCE;
    }

    private String checkAndReplaceSecretIfEmpty(String newPropFile, String oldProf) {
        Map<String, String> emptySecretsAndUserNames = findEmptySecretAndNames(newPropFile);
        return emptySecretsAndUserNames.isEmpty() ? newPropFile : replaceEmptySecret(newPropFile, oldProf, emptySecretsAndUserNames);
    }

    private String replaceEmptySecret(String newPropFile, String oldProf, Map<String, String> emptySecrets) {
        String fileWithReplacedEmptySecrets = newPropFile;
        Matcher oldPassMatcher = Pattern.compile(ChangePropertiesConst.SECRET_REGEX).matcher(oldProf);
        Matcher oldUserMatcher = Pattern.compile(ChangePropertiesConst.USER_REGEX).matcher(oldProf);

        while (oldPassMatcher.find()) {
            String[] s = oldPassMatcher.group().split(":");
            if (emptySecrets.containsKey(s[ChangePropertiesConst.DEFAULT_NAME_PLACE])) {
                fileWithReplacedEmptySecrets = fileWithReplacedEmptySecrets.replace(emptySecrets.get(s[ChangePropertiesConst.DEFAULT_NAME_PLACE]), oldPassMatcher.group());
            }
        }
        while (oldUserMatcher.find()) {
            String[] s = oldUserMatcher.group().split(":");
            if (emptySecrets.containsKey(s[ChangePropertiesConst.DEFAULT_NAME_PLACE])) {
                fileWithReplacedEmptySecrets = fileWithReplacedEmptySecrets.replace(emptySecrets.get(s[ChangePropertiesConst.DEFAULT_NAME_PLACE]), oldUserMatcher.group());
            }
        }
        return fileWithReplacedEmptySecrets;
    }

    private Map<String, String> findEmptySecretAndNames(String newPropFile) {
        Matcher passMatcher = Pattern.compile(ChangePropertiesConst.SECRET_REGEX).matcher(newPropFile);
        Matcher userNameMatcher = Pattern.compile(ChangePropertiesConst.USER_REGEX).matcher(newPropFile);
        Map<String, String> emptySecrets = new HashMap<>();
        while (passMatcher.find()) {
            String[] s = passMatcher.group().split(":");
            if (s[ChangePropertiesConst.DEFAULT_VALUE_PLACE].equals(ChangePropertiesConst.SECRET_REPLACEMENT_FORMAT)) {
                emptySecrets.put(s[ChangePropertiesConst.DEFAULT_NAME_PLACE], passMatcher.group());
            }
        }

        while (userNameMatcher.find()) {
            String[] s = userNameMatcher.group().split(":");
            if (s[ChangePropertiesConst.DEFAULT_VALUE_PLACE].equals(ChangePropertiesConst.SECRET_REPLACEMENT_FORMAT)) {
                emptySecrets.put(s[ChangePropertiesConst.DEFAULT_NAME_PLACE], userNameMatcher.group());
            }
        }
        return emptySecrets;
    }

    private String buildSHRestartCommand(boolean billing, boolean provserv, boolean ui) {
        StringBuilder stringBuilder = new StringBuilder(ChangePropertiesConst.SUPERVISORCTL_RESTART_SH_COMMAND);
        if (billing) stringBuilder.append(ChangePropertiesConst.BILLING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (provserv) stringBuilder.append(ChangePropertiesConst.PROVISIONING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (ui) stringBuilder.append(ChangePropertiesConst.SELF_SERVICE_SUPERVISORCTL_RUN_NAME);
        return stringBuilder.toString();
    }

}
