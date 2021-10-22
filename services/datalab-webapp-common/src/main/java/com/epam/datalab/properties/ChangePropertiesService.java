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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class ChangePropertiesService implements ChangePropertiesConst {

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
        try {
            changeCHMODE(serviceName, servicePath, DEFAULT_CHMOD, WRITE_CHMOD);
            String oldFile = readFile(serviceName, servicePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(servicePath));
            log.info("Trying to overwrite {}, file for path {} :", serviceName, servicePath);
            writer.write(addLicence());
            writer.write(checkAndReplaceSecretIfEmpty(newPropFile, oldFile));
            log.info("{} overwritten successfully", serviceName);
            writer.close();
            changeCHMODE(serviceName, servicePath, WRITE_CHMOD, DEFAULT_CHMOD);
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
            return buildAnswer(restartForm, billing, provserv);
        } catch (IOException | InterruptedException e) {
            log.error("Restart failed : {}", e.getMessage());
            return buildAnswer(restartForm, false, false);
        }
    }

    private RestartAnswer buildAnswer(RestartForm restartForm, boolean billing, boolean provserv) {
        return RestartAnswer.builder()
                .billingSuccess(billing)
                .provservSuccess(provserv)
                .endpoint(restartForm.getEndpoint())
                .build();
    }


    private String hideSecretsAndRemoveLicence(String currentConf) {
        Matcher passMatcher = Pattern.compile(SECRET_REGEX).matcher(currentConf);
        Matcher userMatcher = Pattern.compile(USER_REGEX).matcher(currentConf);
        List<String> secretsAndUsers = new ArrayList<>();
        final String[] confWithReplacedSecretConf = {removeLicence(currentConf)};
        while (passMatcher.find()) {
            String[] secret = passMatcher.group().split(":");
            if (!secret[DEFAULT_NAME_PLACE].isEmpty())
                secretsAndUsers.add(secret[DEFAULT_NAME_PLACE] + ":" + secret[DEFAULT_VALUE_PLACE]);
        }
        while (userMatcher.find()) {
            String[] user = userMatcher.group().split(":");
            if (!user[DEFAULT_NAME_PLACE].isEmpty())
                secretsAndUsers.add(user[DEFAULT_NAME_PLACE] + ":" + user[DEFAULT_VALUE_PLACE]);
        }
        secretsAndUsers.forEach(x -> {
            String toReplace = x.split(":")[DEFAULT_NAME_PLACE] + ":" + SECRET_REPLACEMENT_FORMAT;
            if (x.split(":")[DEFAULT_VALUE_PLACE].length() <= 1) {
                if (x.split(":")[DEFAULT_VALUE_PLACE].startsWith("\r")) {
                    toReplace = toReplace + "\r";
                }
            }
            confWithReplacedSecretConf[0] = confWithReplacedSecretConf[0].replace(x, toReplace);
        });
        return confWithReplacedSecretConf[0];
    }

    private String removeLicence(String conf) {
        return conf.split(LICENCE_REGEX)[conf.split(LICENCE_REGEX).length - 1];
    }


    private void changeCHMODE(String serviceName, String path, String fromMode, String toMode) throws IOException {
        try {
            String command = String.format(CHANGE_CHMOD_SH_COMMAND_FORMAT, toMode, path);
            log.info("Trying to change chmod for file {} {}->{}", serviceName, fromMode, toMode);
            log.info("Execute command: {}", command);
            Runtime.getRuntime().exec(command).waitFor();
        } catch (InterruptedException e) {
            log.error("Failed change chmod for file {} {}->{}", serviceName, fromMode, toMode);
        }
    }

    private String addLicence() {
        return LICENCE;
    }

    private String checkAndReplaceSecretIfEmpty(String newPropFile, String oldProf) {
        Map<String, Queue<String>> emptySecretsAndUserNames = findEmptySecretAndNames(newPropFile);
        return emptySecretsAndUserNames.isEmpty() ? newPropFile : replaceOldSecret(newPropFile, oldProf, emptySecretsAndUserNames);
    }

    private String replaceOldSecret(String newPropFile, String oldProf, Map<String, Queue<String>> emptySecrets) {
        String fileWithReplacedEmptySecrets = newPropFile;
        Matcher oldPassMatcher = Pattern.compile(SECRET_REGEX).matcher(oldProf);
        Matcher oldUserMatcher = Pattern.compile(USER_REGEX).matcher(oldProf);

        while (oldPassMatcher.find()) {
            String[] s = oldPassMatcher.group().split(":");
            if (emptySecrets.containsKey(s[DEFAULT_NAME_PLACE])) {

                String poll = emptySecrets.get(s[DEFAULT_NAME_PLACE]).poll();
                if (poll != null) {
                    poll = poll.replace("*", "\\*");
                    String old = oldPassMatcher.group();
                    old = old.replace("$", "\\$");
                    old = old.replaceFirst("\\{", "\\{");
                    old = old.replaceFirst("}", "\\}");
                    if (old.endsWith("\r"))
                        old = old.substring(0, old.length() - 1);
                    fileWithReplacedEmptySecrets = fileWithReplacedEmptySecrets.replaceFirst(poll, old);
                }
            }
        }
        while (oldUserMatcher.find()) {
            String[] s = oldUserMatcher.group().split(":");
            if (emptySecrets.containsKey(s[DEFAULT_NAME_PLACE])) {
                String poll = emptySecrets.get(s[DEFAULT_NAME_PLACE]).poll();
                if (poll != null) {
                    poll = poll.replace("*", "\\*");
                    String old = oldUserMatcher.group();
                    old = old.replace("$", "\\$");
                    old = old.replace("{", "\\}");
                    old = old.replace("}", "\\}");
                    if (old.endsWith("\r"))
                        old = old.substring(0, old.length() - 1);
                    fileWithReplacedEmptySecrets = fileWithReplacedEmptySecrets.replaceFirst(poll, old);
                }
            }
        }
        return fileWithReplacedEmptySecrets;
    }

    private Map<String, Queue<String>> findEmptySecretAndNames(String newPropFile) {
        Matcher passMatcher = Pattern.compile(SECRET_REGEX).matcher(newPropFile);
        Matcher userNameMatcher = Pattern.compile(USER_REGEX).matcher(newPropFile);
        Map<String, Queue<String>> emptySecrets = new HashMap<>();
        while (passMatcher.find()) {
            String[] s = passMatcher.group().split(":");
            if (s[DEFAULT_VALUE_PLACE].equals(SECRET_REPLACEMENT_FORMAT)) {
                if (emptySecrets.get(s[DEFAULT_NAME_PLACE]) == null) {
                    Queue<String> values = new ArrayDeque<>();
                    values.add(passMatcher.group());
                    emptySecrets.put(s[DEFAULT_NAME_PLACE], values);
                } else {
                    Queue<String> values = emptySecrets.get(s[DEFAULT_NAME_PLACE]);
                    values.add(passMatcher.group());
                }
            }
        }

        while (userNameMatcher.find()) {
            String[] s = userNameMatcher.group().split(":");
            if (s[DEFAULT_VALUE_PLACE].equals(SECRET_REPLACEMENT_FORMAT)) {
                if (emptySecrets.get(s[DEFAULT_NAME_PLACE]) == null) {
                    Queue<String> values = new ArrayDeque<>();
                    values.add(userNameMatcher.group());
                    emptySecrets.put(s[DEFAULT_NAME_PLACE], values);
                } else {
                    Queue<String> values = emptySecrets.get(s[DEFAULT_NAME_PLACE]);
                    values.add(userNameMatcher.group());
                }
            }
        }
        return emptySecrets;
    }

    private String buildSHRestartCommand(boolean billing, boolean provserv, boolean ui) {
        StringBuilder stringBuilder = new StringBuilder(SUPERVISORCTL_RESTART_SH_COMMAND);
        if (billing) stringBuilder.append(BILLING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (provserv) stringBuilder.append(PROVISIONING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (ui) stringBuilder.append(SELF_SERVICE_SUPERVISORCTL_RUN_NAME);
        return stringBuilder.toString();
    }

}
