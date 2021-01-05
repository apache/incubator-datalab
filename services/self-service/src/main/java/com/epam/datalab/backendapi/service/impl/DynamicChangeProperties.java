package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.backendapi.annotation.Audit;
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

import static com.epam.datalab.backendapi.domain.AuditActionEnum.RECONFIGURE;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.EDGE_NODE;

@Slf4j
public class DynamicChangeProperties {

    private static final String SELF_SERVICE = "self-service.yml";
    private static final String SELF_SERVICE_PROP_PATH = "/opt/datalab/conf/self-service.yml";
    private static final String SELF_SERVICE_SUPERVISORCTL_RUN_NAME = " ui ";
    private static final String PROVISIONING_SERVICE = "provisioning.yml";
    private static final String PROVISIONING_SERVICE_PROP_PATH = "/opt/datalab/conf/provisioning.yml";
    private static final String PROVISIONING_SERVICE_SUPERVISORCTL_RUN_NAME = " provserv ";
    private static final String BILLING_SERVICE = "billing.yml";
    private static final String BILLING_SERVICE_PROP_PATH = "/opt/datalab/conf/billing.yml";
    private static final String BILLING_SERVICE_SUPERVISORCTL_RUN_NAME = " billing ";
    private static final String SECRET_REGEX = "((.*)[sS]ecret(.*)|password): (.*)";
    private static final String SECRET_REPLACEMENT_FORMAT = " ***********";
    private static final String SUPERVISORCTL_RESTART_SH_COMMAND = "sudo supervisorctl restart";
    private static final String CHANGE_CHMOD_SH_COMMAND_FORMAT = "sudo chmod %s %s";
    private static final String DEFAULT_CHMOD = "644";
    private static final String WRITE_CHMOD = "777";

    private static final String LICENCE =
            "# *****************************************************************************\n" +
                    "#\n" +
                    "#  Licensed to the Apache Software Foundation (ASF) under one\n" +
                    "#  or more contributor license agreements.  See the NOTICE file\n" +
                    "#  distributed with this work for additional information\n" +
                    "#  regarding copyright ownership.  The ASF licenses this file\n" +
                    "#  to you under the Apache License, Version 2.0 (the\n" +
                    "#  \"License\"); you may not use this file except in compliance\n" +
                    "#  with the License.  You may obtain a copy of the License at\n" +
                    "#\n" +
                    "#  http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "#\n" +
                    "#  Unless required by applicable law or agreed to in writing,\n" +
                    "#  software distributed under the License is distributed on an\n" +
                    "#  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
                    "#  KIND, either express or implied.  See the License for the\n" +
                    "#  specific language governing permissions and limitations\n" +
                    "#  under the License.\n" +
                    "#\n" +
                    "# ******************************************************************************";

    private static final int DEFAULT_VALUE_PLACE = 1;
    private static final int DEFAULT_NAME_PLACE = 0;

    @Audit(action = RECONFIGURE, type = EDGE_NODE)
    public static String getSelfServiceProperties() {
        return readFileAsString(SELF_SERVICE_PROP_PATH, SELF_SERVICE);
    }

    @Audit(action = RECONFIGURE, type = EDGE_NODE)
    public static String getProvisioningServiceProperties() {
        return readFileAsString(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE);
    }

    @Audit(action = RECONFIGURE, type = EDGE_NODE)
    public static String getBillingServiceProperties() {
        return readFileAsString(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE);
    }

    @Audit(action = RECONFIGURE, type = EDGE_NODE)
    public static void overwriteSelfServiceProperties(String ymlString) {
        writeFileFromString(ymlString, SELF_SERVICE, SELF_SERVICE_PROP_PATH);
    }

    @Audit(action = RECONFIGURE, type = EDGE_NODE)
    public static void overwriteProvisioningServiceProperties(String ymlString) {
        writeFileFromString(ymlString, PROVISIONING_SERVICE, PROVISIONING_SERVICE_PROP_PATH);
    }

    @Audit(action = RECONFIGURE, type = EDGE_NODE)
    public static void overwriteBillingServiceProperties(String ymlString) {
        writeFileFromString(ymlString, BILLING_SERVICE, BILLING_SERVICE_PROP_PATH);
    }

    public static void restart(boolean billing, boolean provserv, boolean ui) {
        try {
            String shCommand = buildSHREstartCommand(billing, provserv, ui);
            log.info("Tying to restart ui: {},  provserv: {}, billing: {}, with command: {}", ui,
                    provserv, billing, shCommand);
            Runtime.getRuntime().exec(shCommand).waitFor();


        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    private static String buildSHREstartCommand(boolean billing, boolean provserv, boolean ui) {
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

    private static String hideSecretsAndRemoveLicence(String currentConf) {
        Matcher m = Pattern.compile(SECRET_REGEX).matcher(currentConf);
        List<String> secrets = new ArrayList<>();

        String confWithReplacedSecretConf = removeLicence(currentConf);

        while (m.find()) {

            secrets.add(m.group().split(":")[DEFAULT_VALUE_PLACE]);

        }
        for (String secret : secrets) {
            confWithReplacedSecretConf = confWithReplacedSecretConf.replace(secret, SECRET_REPLACEMENT_FORMAT);
        }
        return confWithReplacedSecretConf;
    }

    private static String removeLicence(String conf) {
        return conf.substring(LICENCE.length());
    }

    private static void writeFileFromString(String newPropFile, String serviceName, String servicePath) {
        try {
            String oldFile = FileUtils.readFileToString(new File(servicePath), Charset.defaultCharset());
            changeCHMODE(serviceName, DEFAULT_CHMOD, WRITE_CHMOD);
            BufferedWriter writer = new BufferedWriter(new FileWriter(servicePath));
            log.info("Trying to overwrite {}, file for path {} :", serviceName, servicePath);
            writer.write(addLicence());
            writer.write(checkAndReplaceSecretIfEmpty(newPropFile, oldFile));
            log.info("{} overwritten successfully", serviceName);
            writer.close();
            changeCHMODE(serviceName, WRITE_CHMOD, DEFAULT_CHMOD);
        } catch (IOException e) {
            log.error("Failed during overwriting {}", serviceName);
            throw new DynamicChangePropertiesException(String.format("Failed during overwriting %s", serviceName));
        }

    }

    private static void changeCHMODE(String serviceName, String fromMode, String toMode) throws IOException {
        try {
            String command = String.format(CHANGE_CHMOD_SH_COMMAND_FORMAT, toMode, serviceName);
            log.info("Trying to change chmod for file {} {}->{}", serviceName, fromMode, toMode);
            log.info("Execute command: {}", command);
            Runtime.getRuntime().exec(command).waitFor();
        } catch (InterruptedException e) {
            log.error("Failed change chmod for file {} {}->{}", serviceName, fromMode, toMode);
        }
    }

    private static String addLicence() {
        return LICENCE + "\n\n";
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
