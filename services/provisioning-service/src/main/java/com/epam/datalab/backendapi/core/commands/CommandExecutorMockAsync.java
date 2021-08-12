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

package com.epam.datalab.backendapi.core.commands;

import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.exploratory.LibInstallDTO;
import com.epam.datalab.dto.exploratory.LibStatus;
import com.epam.datalab.dto.status.EnvResource;
import com.epam.datalab.dto.status.EnvResourceList;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.util.SecurityUtils;
import com.epam.datalab.util.ServiceUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class CommandExecutorMockAsync implements Supplier<Boolean> {
    private static final String JSON_FILE_ENDING = ".json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private String user;
    private String uuid;
    private String command;

    private CommandParserMock parser = new CommandParserMock();
    private String responseFileName;

    private CloudProvider cloudProvider;

    public CommandExecutorMockAsync(String user, String uuid, String command, CloudProvider cloudProvider) {
        this.user = user;
        this.uuid = uuid;
        this.command = command;
        this.cloudProvider = cloudProvider;
    }

    @Override
    public Boolean get() {
        try {
            run();
        } catch (Exception e) {
            log.error("Command with UUID {} fails: {}", uuid, e.getLocalizedMessage(), e);
            return false;
        }
        return true;
    }


    /**
     * Return parser of command line.
     */
    public CommandParserMock getParser() {
        return parser;
    }

    /**
     * Return variables for substitution into Json response file.
     */
    public Map<String, String> getVariables() {
        return parser.getVariables();
    }

    /**
     * Response file name.
     */
    public String getResponseFileName() {
        return responseFileName;
    }

    public void run() {
        log.debug("Run OS command for user {} with UUID {}: {}", user, uuid, SecurityUtils.hideCreds(command));

        responseFileName = null;
        parser = new CommandParserMock(command, uuid);
        log.debug("Parser is {}", SecurityUtils.hideCreds(parser.toString()));
        DockerAction action = DockerAction.of(parser.getAction());
        log.debug("Action is {}", action);

        if (parser.isDockerCommand()) {
            if (action == null) {
                throw new DatalabException("Docker action not defined");
            }

            sleep(500);

            try {
                switch (action) {
                    case DESCRIBE:
                        describe();
                        break;
                    case CREATE:
                    case RECREATE:
                    case START:
                    case STOP:
                    case TERMINATE:
                    case GIT_CREDS:
                    case CREATE_IMAGE:
                    case RECONFIGURE_SPARK:
                    case CHECK_INACTIVITY:
                        action(user, action);
                        break;
                    case CONFIGURE:
                    case REUPLOAD_KEY:
                        sleep(1000);
                        action(user, action);
                        break;
                    case STATUS:
                        parser.getVariables().put("list_resources", getResponseStatus(true));
                        action(user, action);
                        break;
                    case LIB_LIST:
                        action(user, action);
                        copyFile(String.format("mock_response/%s/notebook_lib_list_pkgs.json",
                                cloudProvider.getName()),
                                String.join("_", "notebook", uuid, "all_pkgs") +
                                        JSON_FILE_ENDING, parser.getResponsePath());
                        break;
                    case LIB_INSTALL:
                        parser.getVariables().put("lib_install", getResponseLibInstall(true));
                        action(user, action);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                String msg = "Cannot execute command for user " + user + " with UUID " + uuid + ". " +
                        e.getLocalizedMessage();
                log.error(msg, e);
                throw new DatalabException(msg, e);
            }
        } else {
            final String scriptName = StringUtils.substringBefore(Paths.get(parser.getCommand()).getFileName()
                    .toString(), ".");
            String templateFileName = "mock_response/" + cloudProvider.getName() + '/' + scriptName + JSON_FILE_ENDING;
            responseFileName = getAbsolutePath(parser.getResponsePath(), scriptName + user + "_" +
                    parser.getRequestId() + JSON_FILE_ENDING);
            setResponse(templateFileName, responseFileName);
        }

    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.error("InterruptedException occurred: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private static void copyFile(String sourceFilePath, String destinationFileName, String destinationFolder) throws
            IOException {
        File to = new File(getAbsolutePath(destinationFolder, destinationFileName));

        try (InputStream inputStream =
                     CommandExecutorMockAsync.class.getClassLoader().getResourceAsStream(sourceFilePath);
             OutputStream outputStream = new FileOutputStream(to)) {
            ByteStreams.copy(inputStream, outputStream);
        }

        log.debug("File {} copied to {}", sourceFilePath, to);
    }

    /**
     * Return absolute path to the file or folder.
     *
     * @param first part of path.
     * @param more  next path components.
     */
    private static String getAbsolutePath(String first, String... more) {
        return Paths.get(first, more).toAbsolutePath().toString();
    }

    /**
     * Tests the directory exists.
     *
     * @param dir the name of directory.
     * @return <b>true</b> if the directory exists otherwise return <b>false</b>.
     */
    private boolean dirExists(String dir) {
        File file = new File(dir);
        return (file.exists() && file.isDirectory());
    }

    /**
     * Find and return the directory "infrastructure-provisioning/src".
     *
     * @throws FileNotFoundException may be thrown
     */
    private String findTemplatesDir() throws FileNotFoundException {
        String dir = System.getProperty("docker.dir");

        if (dir != null) {
            dir = getAbsolutePath(dir);
            if (dirExists(dir)) {
                return dir;
            }
            throw new FileNotFoundException("Directory \"" + dir + "\" not found. " +
                    "Please set JVM argument -Ddocker.dir to the " +
                    "\".../infrastructure-provisioning/src/general/files/" + cloudProvider.getName() + "\" directory");
        }
        dir = getAbsolutePath(
                ".",
                "../../infrastructure-provisioning/src/general/files/" + cloudProvider.getName());
        if (dirExists(dir)) {
            return dir;
        }
        dir = getAbsolutePath(
                ServiceUtils.getUserDir(),
                "../../infrastructure-provisioning/src/general/files/" + cloudProvider.getName());
        if (dirExists(dir)) {
            return dir;
        }
        throw new FileNotFoundException("Directory \"" + dir + "\" not found. " +
                "Please set the value docker.dir property to the " +
                "\".../infrastructure-provisioning/src/general/files/" + cloudProvider.getName() + "\" directory");
    }

    /**
     * Describe action.
     */
    private void describe() {
        String templateFileName;
        try {
            templateFileName = getAbsolutePath(findTemplatesDir(), parser.getImageType() + "_description.json");
        } catch (FileNotFoundException e) {
            throw new DatalabException("Cannot describe image " + parser.getImageType() + ". " + e.getLocalizedMessage(),
                    e);
        }
        responseFileName = getAbsolutePath(parser.getResponsePath(), parser.getRequestId() + JSON_FILE_ENDING);

        log.debug("Create response file from {} to {}", templateFileName, responseFileName);
        File fileResponse = new File(responseFileName);
        File fileTemplate = new File(templateFileName);
        try {
            if (!fileTemplate.exists()) {
                throw new FileNotFoundException("File \"" + fileTemplate + "\" not found.");
            }
            if (!fileTemplate.canRead()) {
                throw new IOException("Cannot read file \"" + fileTemplate + "\".");
            }
            Files.createParentDirs(fileResponse);
            Files.copy(fileTemplate, fileResponse);
        } catch (IOException e) {
            throw new DatalabException("Can't create response file " + responseFileName + ": " + e.getLocalizedMessage(),
                    e);
        }
    }

    /**
     * Perform docker action.
     *
     * @param user   the name of user.
     * @param action docker action.
     */
    private void action(String user, DockerAction action) {
        String resourceType = parser.getResourceType();

        String prefixFileName = (Lists.newArrayList("project", "edge", "odahu", "dataengine", "dataengine-service")
                .contains(resourceType) ? resourceType : "notebook") + "_";
        String templateFileName = "mock_response/" + cloudProvider.getName() + '/' + prefixFileName +
                action.toString() + JSON_FILE_ENDING;
        responseFileName = getAbsolutePath(parser.getResponsePath(), prefixFileName + user + "_" +
                parser.getRequestId() + JSON_FILE_ENDING);
        setResponse(templateFileName, responseFileName);
    }

    /**
     * Return the section of resource statuses for docker action status.
     */
    private String getResponseStatus(boolean noUpdate) {
        if (noUpdate) {
            return "{}";
        }
        EnvResourceList resourceList;
        try {
            JsonNode json = MAPPER.readTree(parser.getJson());
            json = json.get("edge_list_resources");
            resourceList = MAPPER.readValue(json.toString(), EnvResourceList.class);
        } catch (IOException e) {
            throw new DatalabException("Can't parse json content: " + e.getLocalizedMessage(), e);
        }

        if (resourceList.getHostList() != null) {
            for (EnvResource host : resourceList.getHostList()) {
                host.setStatus(UserInstanceStatus.RUNNING.toString());
            }
        }
        if (resourceList.getClusterList() != null) {
            for (EnvResource host : resourceList.getClusterList()) {
                host.setStatus(UserInstanceStatus.RUNNING.toString());
            }
        }

        try {
            return MAPPER.writeValueAsString(resourceList);
        } catch (JsonProcessingException e) {
            throw new DatalabException("Can't generate json content: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Return the section of resource statuses for docker action status.
     */
    private String getResponseLibInstall(boolean isSuccess) {
        List<LibInstallDTO> list;
        try {
            JsonNode json = MAPPER.readTree(parser.getJson());
            json = json.get("libs");
            list = MAPPER.readValue(json.toString(), new TypeReference<List<LibInstallDTO>>() {
            });
        } catch (IOException e) {
            throw new DatalabException("Can't parse json content: " + e.getLocalizedMessage(), e);
        }

        for (LibInstallDTO lib : list) {
            if (isSuccess) {
                lib.setStatus(LibStatus.INSTALLED.toString());
            } else {
                lib.setStatus(LibStatus.INSTALLATION_ERROR.toString());
                lib.setErrorMessage("Mock error message");
            }
        }

        try {
            return MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new DatalabException("Can't generate json content: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Write response file.
     *
     * @param sourceFileName template file name.
     * @param targetFileName response file name.
     */
    private void setResponse(String sourceFileName, String targetFileName) {
        String content;
        URL url = Resources.getResource(sourceFileName);
        try {
            content = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new DatalabException("Can't read resource " + sourceFileName + ": " + e.getLocalizedMessage(), e);
        }

        for (String key : parser.getVariables().keySet()) {
            String value = parser.getVariables().get(key);
            content = content.replace("${" + key.toUpperCase() + "}", value);
        }

        File fileResponse = new File(responseFileName);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(fileResponse))) {
            Files.createParentDirs(fileResponse);
            out.write(content);
        } catch (IOException e) {
            throw new DatalabException("Can't write response file " + targetFileName + ": " + e.getLocalizedMessage(), e);
        }
        log.debug("Create response file from {} to {}", sourceFileName, targetFileName);
    }
}
