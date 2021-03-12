<<<<<<< HEAD:services/self-service/src/main/java/com/epam/datalab/backendapi/service/impl/DynamicChangeProperties.java
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

package com.epam.datalab.backendapi.service.impl;
=======
package com.epam.datalab.properties;
>>>>>>> develop:services/datalab-webapp-common/src/main/java/com/epam/datalab/properties/DynamicChangeProperties.java

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.exceptions.DynamicChangePropertiesException;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.inject.Inject;
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
public class DynamicChangeProperties implements ChangePropertiesConst {
    private final RESTService externalSelfService;

    @Inject
    public DynamicChangeProperties(@Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService externalSelfService) {
        this.externalSelfService = externalSelfService;
    }

    public String getProperties(String path, String name) {
        return readFileAsString(path, name);
    }

    public void overwriteProperties(String path, String name, String ymlString) {
        writeFileFromString(ymlString, name, path);
    }

    public Map<String, String> getPropertiesWithExternal(String endpoint, UserInfo userInfo, String url) {
        Map<String, String> properties = new HashMap<>();
        if (endpoint.equals(ChangePropertiesConst.LOCAL_ENDPOINT_NAME)) {
            properties.put(ChangePropertiesConst.SELF_SERVICE, getProperties(ChangePropertiesConst.SELF_SERVICE_PROP_PATH, ChangePropertiesConst.SELF_SERVICE));
            properties.put(ChangePropertiesConst.PROVISIONING_SERVICE, getProperties(ChangePropertiesConst.PROVISIONING_SERVICE_PROP_PATH, ChangePropertiesConst.PROVISIONING_SERVICE));
            properties.put(ChangePropertiesConst.BILLING_SERVICE, getProperties(ChangePropertiesConst.BILLING_SERVICE_PROP_PATH, ChangePropertiesConst.BILLING_SERVICE));
        } else {
            log.info("Trying to read properties, for external endpoint : {} , for user: {}",
                    endpoint, userInfo.getSimpleName());
            properties.put(ChangePropertiesConst.SELF_SERVICE, getProperties(ChangePropertiesConst.SELF_SERVICE_PROP_PATH, ChangePropertiesConst.SELF_SERVICE));
            properties.put(ChangePropertiesConst.PROVISIONING_SERVICE,
                    externalSelfService.get(url + "/provisioning-service", userInfo.getAccessToken(), String.class));
            properties.put(ChangePropertiesConst.BILLING_SERVICE,
                    externalSelfService.get(url + "/billing", userInfo.getAccessToken(), String.class));
        }
        return properties;
    }

    public void overwritePropertiesWithExternal(String path, String name, YmlDTO ymlDTO, UserInfo userInfo,
                                                String url) {
        log.info("Trying to write {}, for external endpoint : {} , for user: {}",
                name, ymlDTO.getEndpointName(), userInfo.getSimpleName());
        if (ymlDTO.getEndpointName().equals(ChangePropertiesConst.LOCAL_ENDPOINT_NAME)
                || name.equals(SELF_SERVICE)
                || name.equals(GKE_SELF_SERVICE)) {
            writeFileFromString(ymlDTO.getYmlString(), name, path);
        } else {
            url += findMethodName(name);
            externalSelfService.post(url, ymlDTO.getYmlString(), userInfo.getAccessToken(), String.class);
        }
    }

    public void restart(RestartForm restartForm) {
        try {
            boolean billing = restartForm.isBilling();
            boolean provserv = restartForm.isProvserv();
            boolean ui = restartForm.isUi();
            String shCommand = buildSHRestartCommand(billing, provserv, ui);
            log.info("Tying to restart ui: {}, provserv: {}, billing: {}, with command: {}", ui,
                    provserv, billing, shCommand);
            Runtime.getRuntime().exec(shCommand).waitFor();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    public void restartForExternal(RestartForm restartForm, UserInfo userInfo, String url) {
        if (restartForm.getEndpoint().equals(LOCAL_ENDPOINT_NAME)) {
            restart(restartForm);
        } else {
            log.info("External request for endpoint {}, for user {}", restartForm.getEndpoint(), userInfo.getSimpleName());

            externalSelfService.post(url, userInfo.getAccessToken(), restartForm, Void.class);
        }
    }

    private String findMethodName(String name) {
        switch (name) {
            case "self-service.yml": {
                return "self-service";
            }
            case "provisioning.yml": {
                return "provisioning-service";
            }
            case "billing.yml": {
                return "billing";
            }
            default:
                return "";
        }
    }


    private String buildSHRestartCommand(boolean billing, boolean provserv, boolean ui) {
        StringBuilder stringBuilder = new StringBuilder(ChangePropertiesConst.SUPERVISORCTL_RESTART_SH_COMMAND);
        if (billing) stringBuilder.append(ChangePropertiesConst.BILLING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (provserv) stringBuilder.append(ChangePropertiesConst.PROVISIONING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (ui) stringBuilder.append(ChangePropertiesConst.SELF_SERVICE_SUPERVISORCTL_RUN_NAME);
        return stringBuilder.toString();
    }

    private String readFileAsString(String selfServicePropPath, String serviceName) {
        try {
            log.info("Trying to read self-service.yml, file from path {} :", selfServicePropPath);
            String currentConf = FileUtils.readFileToString(new File(selfServicePropPath), Charset.defaultCharset());
            return hideSecretsAndRemoveLicence(currentConf);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new DynamicChangePropertiesException(String.format("Failed while read file %s", serviceName));
        }
    }

    private String hideSecretsAndRemoveLicence(String currentConf) {
        Matcher m = Pattern.compile(ChangePropertiesConst.SECRET_REGEX).matcher(currentConf);
        List<String> secrets = new ArrayList<>();
        String confWithReplacedSecretConf = removeLicence(currentConf);
        while (m.find()) {
            String secret = m.group().split(":")[ChangePropertiesConst.DEFAULT_VALUE_PLACE];
            if (!(secret.isEmpty() || secret.trim().isEmpty()))
                secrets.add(secret);
        }
        for (String secret : secrets) {
            confWithReplacedSecretConf = confWithReplacedSecretConf.replace(secret, ChangePropertiesConst.SECRET_REPLACEMENT_FORMAT);
        }
        return confWithReplacedSecretConf;
    }

    private String removeLicence(String conf) {
        return conf.split(ChangePropertiesConst.LICENCE_REGEX)[conf.split(ChangePropertiesConst.LICENCE_REGEX).length - 1];
    }

    private void writeFileFromString(String newPropFile, String serviceName, String servicePath) {
        try {
            String oldFile = FileUtils.readFileToString(new File(servicePath), Charset.defaultCharset());
            changeCHMODE(serviceName, servicePath, ChangePropertiesConst.DEFAULT_CHMOD, ChangePropertiesConst.WRITE_CHMOD);
            BufferedWriter writer = new BufferedWriter(new FileWriter(servicePath));
            log.info("Trying to overwrite {}, file for path {} :", serviceName, servicePath);
            writer.write(addLicence());
            writer.write(checkAndReplaceSecretIfEmpty(newPropFile, oldFile));
            log.info("{} overwritten successfully", serviceName);
            writer.close();
            changeCHMODE(serviceName, servicePath, ChangePropertiesConst.WRITE_CHMOD, ChangePropertiesConst.DEFAULT_CHMOD);
        } catch (IOException e) {
            log.error("Failed during overwriting {}", serviceName);
            throw new DynamicChangePropertiesException(String.format("Failed during overwriting %s", serviceName));
        }
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
        Map<String, String> emptySecrets = findEmptySecret(newPropFile);
        return emptySecrets.isEmpty() ? newPropFile : replaceEmptySecret(newPropFile, oldProf, emptySecrets);
    }

    private String replaceEmptySecret(String newPropFile, String oldProf, Map<String, String> emptySecrets) {
        String fileWithReplacedEmptySecrets = newPropFile;
        Matcher oldProfMatcher = Pattern.compile(ChangePropertiesConst.SECRET_REGEX).matcher(oldProf);
        while (oldProfMatcher.find()) {
            String[] s = oldProfMatcher.group().split(":");
            if (emptySecrets.containsKey(s[ChangePropertiesConst.DEFAULT_NAME_PLACE])) {
                fileWithReplacedEmptySecrets = fileWithReplacedEmptySecrets.replace(emptySecrets.get(s[ChangePropertiesConst.DEFAULT_NAME_PLACE]), oldProfMatcher.group());
            }
        }
        return fileWithReplacedEmptySecrets;
    }

    private Map<String, String> findEmptySecret(String newPropFile) {
        Matcher newPropFileMatcher = Pattern.compile(ChangePropertiesConst.SECRET_REGEX).matcher(newPropFile);
        Map<String, String> emptySecrets = new HashMap<>();
        while (newPropFileMatcher.find()) {
            String[] s = newPropFileMatcher.group().split(":");
            if (s[ChangePropertiesConst.DEFAULT_VALUE_PLACE].equals(ChangePropertiesConst.SECRET_REPLACEMENT_FORMAT)) {
                emptySecrets.put(s[ChangePropertiesConst.DEFAULT_NAME_PLACE], newPropFileMatcher.group());
            }
        }
        return emptySecrets;
    }

    public void restartForExternalForGKE(UserInfo userInfo, RestartForm restartForm) {
        throw new NotImplementedException();
    }
}
