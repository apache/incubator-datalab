package com.epam.datalab.backendapi.service.impl;

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
public class DynamicChangeProperties {

    private static final String SELF_SERVICE = "self-service.yml";
    private static final String SELF_SERVICE_PROP_PATH = "services/billing-azure/self-service.yml";
    private static final String PROVISIONING_SERVICE = "provisioning.yml";
    private static final String PROVISIONING_SERVICE_PROP_PATH = "services/provisioning-service/provisioning.yml";
    private static final String SECRET_REGEX = "(.*)[sS]ecret(.*): (.*)";
    private static final String SECRET_REPLACEMENT_FORMAT = " ***********";
    private static final String SH_COMMAND = "sudo supervisorctl restart ";

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

    public String getSelfServiceProperties() {
        return readFileAsString(SELF_SERVICE_PROP_PATH, SELF_SERVICE);
    }

    public String getProvisioningServiceProperties() {
        return readFileAsString(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE);
    }

    public void overwriteSelfServiceProperties(String newPropFile) {
        writeFileFromString(newPropFile, SELF_SERVICE, SELF_SERVICE_PROP_PATH);
    }

    public void overwriteProvisioningServiceProperties(String newPropFile) {
        writeFileFromString(newPropFile, PROVISIONING_SERVICE, PROVISIONING_SERVICE_PROP_PATH);
    }

    public void restart(boolean restartSelfService, boolean restartProvisioning) {
        StringBuilder stringBuilder = new StringBuilder(SH_COMMAND);
        if (restartSelfService) {
            stringBuilder.append(" self-service");
        }
        if (restartProvisioning) {
            stringBuilder.append(" provisioning");
        }
        try {
            Process process = Runtime.getRuntime()
                    .exec(String.format("sh %s", stringBuilder.toString()));
            //        String homeDirectory = System.getProperty("user.home");
//            StreamGobbler streamGobbler =
//                    new StreamGobbler(process.getInputStream(), System.out::println);
//            Executors.newSingleThreadExecutor().submit(streamGobbler);
//            int exitCode = process.waitFor();
//            assert exitCode == 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        // run ssh with restart supervisorctl
    }

    private String readFileAsString(String selfServicePropPath, String serviceName) {
        try {
            log.trace("Trying to read self-service.yml, file from path {} :", selfServicePropPath);
            String currentConf = FileUtils.readFileToString(new File(selfServicePropPath), Charset.defaultCharset());
            return hideSecretsAndRemoveLicence(currentConf);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new DynamicChangePropertiesException(String.format("Failed while read file %s", serviceName));
        }
    }

    private String hideSecretsAndRemoveLicence(String currentConf) {
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

    private String removeLicence(String conf) {
        return conf.substring(LICENCE.length() + 7);
    }

    private void writeFileFromString(String newPropFile, String serviceName, String servicePath) {
        try {
            String oldFile = FileUtils.readFileToString(new File(servicePath), Charset.defaultCharset());
            BufferedWriter writer = new BufferedWriter(new FileWriter(servicePath));
            log.trace("Trying to overwrite {}, file for path {} :", serviceName, servicePath);
            writer.write(LICENCE);
            writer.write(checkAndReplaceSecretIfEmpty(newPropFile, oldFile));
            log.info("{} overwritten successfully", serviceName);
            writer.close();
        } catch (IOException e) {
            log.error("Failed during overwriting {}", serviceName);
            throw new DynamicChangePropertiesException(String.format("Failed during overwriting %s", serviceName));
        }
    }

    private String checkAndReplaceSecretIfEmpty(String newPropFile, String oldProf) {
        Map<String, String> emptySecrets = findEmptySecret(newPropFile);
        return emptySecrets.isEmpty() ? newPropFile : replaceEmptySecret(newPropFile, oldProf, emptySecrets);

    }

    private String replaceEmptySecret(String newPropFile, String oldProf, Map<String, String> emptySecrets) {
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

    private Map<String, String> findEmptySecret(String newPropFile) {
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
