package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.annotation.Audit;
import com.epam.datalab.backendapi.dao.EndpointDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.resources.dto.YmlDTO;
import com.epam.datalab.exceptions.DynamicChangePropertiesException;
import com.epam.datalab.rest.client.RESTService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

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
import java.util.stream.Collectors;

import static com.epam.datalab.backendapi.domain.AuditActionEnum.RECONFIGURE;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.EDGE_NODE;

@Slf4j
public class DynamicChangeProperties {


    private static final String SELF_SERVICE_SUPERVISORCTL_RUN_NAME = " ui ";
    private static final String PROVISIONING_SERVICE_SUPERVISORCTL_RUN_NAME = " provserv ";
    private static final String BILLING_SERVICE_SUPERVISORCTL_RUN_NAME = " billing ";
    private static final String SECRET_REGEX = "((.*)[sS]ecret(.*)|password): (.*)";
    private static final String SECRET_REPLACEMENT_FORMAT = " ***********";
    private static final String SUPERVISORCTL_RESTART_SH_COMMAND = "sudo supervisorctl restart";
    private static final String CHANGE_CHMOD_SH_COMMAND_FORMAT = "sudo chmod %s %s";
    private static final String DEFAULT_CHMOD = "644";
    private static final String WRITE_CHMOD = "777";

    private static final String LICENCE_REGEX = "# \\*{50,}";
    private static final String LICENCE =
            "# *****************************************************************************\n" +
                    "#\n" +
                    "# Licensed to the Apache Software Foundation (ASF) under one\n" +
                    "# or more contributor license agreements. See the NOTICE file\n" +
                    "# distributed with this work for additional information\n" +
                    "# regarding copyright ownership. The ASF licenses this file\n" +
                    "# to you under the Apache License, Version 2.0 (the\n" +
                    "# \"License\"); you may not use this file except in compliance\n" +
                    "# with the License. You may obtain a copy of the License at\n" +
                    "#\n" +
                    "# http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "#\n" +
                    "# Unless required by applicable law or agreed to in writing,\n" +
                    "# software distributed under the License is distributed on an\n" +
                    "# \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                    "# KIND, either express or implied. See the License for the\n" +
                    "# specific language governing permissions and limitations\n" +
                    "# under the License.\n" +
                    "#\n" +
                    "# ******************************************************************************";

    private static final int DEFAULT_VALUE_PLACE = 1;
    private static final int DEFAULT_NAME_PLACE = 0;

    @Inject
    private static RESTService externalSelfService;
    @Inject
    private static EndpointDAO endpointDAO;

    @Audit(action = RECONFIGURE, type = EDGE_NODE)
    public static String getProperties(String path, String name) {
        return readFileAsString(path, name);
    }

    @Audit(action = RECONFIGURE, type = EDGE_NODE)
    public static void overwriteProperties(String path, String name, String ymlString) {
        writeFileFromString(ymlString, name, path);
    }

    @Audit(action = RECONFIGURE, type = EDGE_NODE)
    public static Map<String, String> getPropertiesWithExternal(String path, String name, UserInfo userInfo) {
        List<EndpointDTO> externalEndpoints = endpointDAO.getEndpointsWithStatus("ACTIVE");
        Map<String, String> endpoints = externalEndpoints.stream()
                .filter(endpointDTO -> !endpointDTO.getName().equals("Local"))
                .collect(Collectors.toMap(EndpointDTO::getName,
                        dto -> readFileAsString(path, name, dto, userInfo)));
        endpoints.put("Local", getProperties(path, name));
        return endpoints;

    }

    @Audit(action = RECONFIGURE, type = EDGE_NODE)
    public static void overwritePropertiesWithExternal(String path, String name, Map<String, YmlDTO> ymlDTOS,
                                                       UserInfo userInfo) {

        List<EndpointDTO> allEndpoints = endpointDAO.getEndpointsWithStatus("ACTIVE");
        Map<EndpointDTO, String> endpointsToChange = allEndpoints.stream()
                .filter(e -> ymlDTOS.containsKey(e.getName()))
                .filter(e -> !e.getName().equals("Local"))
                .collect(Collectors.toMap(e -> e, e -> ymlDTOS.get(e.getName()).getYmlString()));

        endpointsToChange.forEach((endpoint, ymlString) -> {
            log.info("Trying to write {}, for external endpoint : {} , for user: {}",
                    name, endpoint.getName(), userInfo.getSimpleName());
            String url = endpoint.getUrl() + "/api/configuration/" + findMethodName(name);
            externalSelfService.post(url, ymlString, userInfo.getAccessToken(), String.class);
        });
        if (ymlDTOS.containsKey("Local")) {
            overwriteProperties(path, name, ymlDTOS.get("Local").getYmlString());
        }
    }

    private static String findMethodName(String name) {
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

    public static void restart(boolean billing, boolean provserv, boolean ui) {
        try {
            String shCommand = buildSHRestartCommand(billing, provserv, ui);
            log.info("Tying to restart ui: {}, provserv: {}, billing: {}, with command: {}", ui,
                    provserv, billing, shCommand);
            Runtime.getRuntime().exec(shCommand).waitFor();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    private static String buildSHRestartCommand(boolean billing, boolean provserv, boolean ui) {
        StringBuilder stringBuilder = new StringBuilder(SUPERVISORCTL_RESTART_SH_COMMAND);
        if (billing) stringBuilder.append(BILLING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (provserv) stringBuilder.append(PROVISIONING_SERVICE_SUPERVISORCTL_RUN_NAME);
        if (ui) stringBuilder.append(SELF_SERVICE_SUPERVISORCTL_RUN_NAME);
        return stringBuilder.toString();
    }

    private static String readFileAsString(String selfServicePropPath, String serviceName) {
        try {
            log.info("Trying to read self-service.yml, file from path {} :", selfServicePropPath);
            String currentConf = FileUtils.readFileToString(new File(selfServicePropPath), Charset.defaultCharset());
            return hideSecretsAndRemoveLicence(currentConf);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new DynamicChangePropertiesException(String.format("Failed while read file %s", serviceName));
        }
    }

    private static String readFileAsString(String selfServicePropPath, String serviceName, EndpointDTO endpoint, UserInfo userInfo) {
        log.info("Trying to read self-service.yml, for external endpoint : {} , for user: {}",
                endpoint, userInfo.getSimpleName());
        String currentConf = externalSelfService.get(endpoint.getUrl() + "/api/admin/self-service",
                userInfo.getAccessToken(), String.class);
        return hideSecretsAndRemoveLicence(currentConf);
    }

    private static String hideSecretsAndRemoveLicence(String currentConf) {
        Matcher m = Pattern.compile(SECRET_REGEX).matcher(currentConf);
        List<String> secrets = new ArrayList<>();
        String confWithReplacedSecretConf = removeLicence(currentConf);
        while (m.find()) {
            String secret = m.group().split(":")[DEFAULT_VALUE_PLACE];
            if (!(secret.isEmpty() || secret.trim().isEmpty()))
                secrets.add(secret);
        }
        for (String secret : secrets) {
            confWithReplacedSecretConf = confWithReplacedSecretConf.replace(secret, SECRET_REPLACEMENT_FORMAT);
        }
        return confWithReplacedSecretConf;
    }

    private static String removeLicence(String conf) {
        return conf.split(LICENCE_REGEX)[conf.split(LICENCE_REGEX).length - 1];
    }

    private static void writeFileFromString(String newPropFile, String serviceName, String servicePath) {
        try {
            String oldFile = FileUtils.readFileToString(new File(servicePath), Charset.defaultCharset());
            changeCHMODE(serviceName, servicePath, DEFAULT_CHMOD, WRITE_CHMOD);
            BufferedWriter writer = new BufferedWriter(new FileWriter(servicePath));
            log.info("Trying to overwrite {}, file for path {} :", serviceName, servicePath);
            writer.write(addLicence());
            writer.write(checkAndReplaceSecretIfEmpty(newPropFile, oldFile));
            log.info("{} overwritten successfully", serviceName);
            writer.close();
            changeCHMODE(serviceName, servicePath, WRITE_CHMOD, DEFAULT_CHMOD);
        } catch (IOException e) {
            log.error("Failed during overwriting {}", serviceName);
            throw new DynamicChangePropertiesException(String.format("Failed during overwriting %s", serviceName));
        }
    }

    private static void changeCHMODE(String serviceName, String path, String fromMode, String toMode) throws IOException {
        try {
            String command = String.format(CHANGE_CHMOD_SH_COMMAND_FORMAT, toMode, path);
            log.info("Trying to change chmod for file {} {}->{}", serviceName, fromMode, toMode);
            log.info("Execute command: {}", command);
            Runtime.getRuntime().exec(command).waitFor();
        } catch (InterruptedException e) {
            log.error("Failed change chmod for file {} {}->{}", serviceName, fromMode, toMode);
        }
    }

    private static String addLicence() {
        return LICENCE;
    }

    private static String checkAndReplaceSecretIfEmpty(String newPropFile, String oldProf) {
        Map<String, String> emptySecrets = findEmptySecret(newPropFile);
        return emptySecrets.isEmpty() ? newPropFile : replaceEmptySecret(newPropFile, oldProf, emptySecrets);
    }

    private static String replaceEmptySecret(String newPropFile, String oldProf, Map<String, String> emptySecrets) {
        String fileWithReplacedEmptySecrets = newPropFile;
        Matcher oldProfMatcher = Pattern.compile(SECRET_REGEX).matcher(oldProf);
        while (oldProfMatcher.find()) {
            String[] s = oldProfMatcher.group().split(":");
            if (emptySecrets.containsKey(s[DEFAULT_NAME_PLACE])) {
                fileWithReplacedEmptySecrets = fileWithReplacedEmptySecrets.replace(emptySecrets.get(s[DEFAULT_NAME_PLACE]), oldProfMatcher.group());
            }
        }
        return fileWithReplacedEmptySecrets;
    }

    private static Map<String, String> findEmptySecret(String newPropFile) {
        Matcher newPropFileMatcher = Pattern.compile(SECRET_REGEX).matcher(newPropFile);
        Map<String, String> emptySecrets = new HashMap<>();
        while (newPropFileMatcher.find()) {
            String[] s = newPropFileMatcher.group().split(":");
            if (s[DEFAULT_VALUE_PLACE].equals(SECRET_REPLACEMENT_FORMAT)) {
                emptySecrets.put(s[DEFAULT_NAME_PLACE], newPropFileMatcher.group());
            }
        }
        return emptySecrets;
    }
}
