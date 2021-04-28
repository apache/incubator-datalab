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

import com.epam.datalab.exceptions.DatalabException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

/**
 * Parse command for emulate commands of Docker.
 */
public class CommandParserMock {
    private ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private String command;
    private String action;
    private String resourceType;
    private String imageType;
    private String requestId;
    private String responsePath;
    private String name;
    private String json;
    private Map<String, String> envMap = new HashMap<>();
    private Map<String, String> varMap = new HashMap<>();
    private List<String> otherArgs = new ArrayList<>();
    private Map<String, String> variables = new HashMap<>();
    private boolean dockerCommand;

    public CommandParserMock() {
    }

    public CommandParserMock(String command, String uuid) {
        parse(command, uuid);
    }


    /**
     * Return the name of docker command.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Return the name of docker action.
     */
    public String getAction() {
        return action;
    }

    /**
     * Return the type of resource.
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Return the image type.
     */
    public String getImageType() {
        return imageType;
    }

    /**
     * Return the request id.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Return the path for response files.
     */
    public String getResponsePath() {
        return responsePath;
    }

    /**
     * Return name of docker container.
     */
    public String getName() {
        return name;
    }

    /**
     * Return content of Json if present otherwise <b>null</b>.
     */
    public String getJson() {
        return json;
    }

    /**
     * Return map of environment variables.
     */
    public Map<String, String> getVariables() {
        return variables;
    }

    /**
     * Add argument to list.
     *
     * @param args list of arguments.
     * @param arg  argument.
     */
    private void addArgToList(List<String> args, String arg) {
        if (arg == null) {
            return;
        }
        if (arg.length() > 1) {
            if (arg.startsWith("'") && arg.endsWith("'")) {
                arg = arg.substring(1, arg.length() - 1);
            }
            if (arg.startsWith("\"") && arg.endsWith("\"")) {
                arg = arg.substring(1, arg.length() - 1);
            }
        }
        arg = arg.trim();
        if (arg.isEmpty()) {
            return;
        }

        args.add(arg);
    }

    /**
     * Extract arguments from command line.
     *
     * @param cmd command line.
     * @return List of arguments.
     */
    private List<String> extractArgs(String cmd) {
        boolean isQuote = false;
        boolean isDoubleQuote = false;
        List<String> args = new ArrayList<>();
        int pos = 0;

        for (int i = 0; i < cmd.length(); i++) {
            final char c = cmd.charAt(i);
            if (c == '\'') {
                isQuote = !isQuote;
                continue;
            }
            if (c == '"') {
                isDoubleQuote = !isDoubleQuote;
                continue;
            }

            if (!isQuote && !isDoubleQuote && c == ' ') {
                addArgToList(args, cmd.substring(pos, i));
                pos = i + 1;
            }
        }
        if (!isQuote && !isDoubleQuote) {
            addArgToList(args, cmd.substring(pos));
        }

        return args;
    }

    /**
     * Return the value of argument.
     *
     * @param args    list of arguments.
     * @param index   index of named arguments
     * @param argName name of argument.
     */
    private String getArgValue(List<String> args, int index, String argName) {
        if (!args.get(index).equals(argName)) {
            return null;
        }
        args.remove(index);
        if (index < args.size()) {
            String value = args.get(index);
            args.remove(index);
            return value;
        }
        throw new DatalabException("Argument \"" + argName + "\" detected but not have value");
    }

    /**
     * Return pair name/value separated.
     *
     * @param argName   name of argument.
     * @param value     value.
     * @param separator separator.
     */
    private Pair<String, String> getPair(String argName, String value, String separator) {
        String[] array = value.split(separator);
        if (array.length == 2) {
            return new ImmutablePair<>(array[0], array[1]);
        } else if (array.length == 3) {
            return new ImmutablePair<>(array[1], array[2]);
        }
        throw new DatalabException("Invalid value for \"" + argName + "\": " + value);
    }

    /**
     * Return name of docker image.
     *
     * @param args list of arguments.
     * @throws if image name not found.
     */
    public static String getImageName(List<String> args) {
        for (String s : args) {
            if (s.startsWith("docker.datalab-")) {
                return s;
            }
        }
        throw new DatalabException("Name of docker image not found");
    }

    /**
     * Extract Json properties from Json content.
     *
     * @param jsonContent Json content.
     * @return
     */
    private Map<String, String> getJsonVariables(String jsonContent) {
        Map<String, String> vars = new HashMap<>();
        if (jsonContent == null) {
            return vars;
        }

        JsonNode json;
        try {
            json = MAPPER.readTree(jsonContent);
        } catch (IOException e) {
            throw new DatalabException("Can't parse json content: " + e.getLocalizedMessage(), e);
        }

        Iterator<String> keys = json.fieldNames();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = getTextValue(json.get(key));
            if (value != null) {
                vars.put(key, value);
            }
        }
        return vars;
    }

    /**
     * Return the value of json property or <b>null</b>.
     *
     * @param jsonNode - Json node.
     */
    private String getTextValue(JsonNode jsonNode) {
        return jsonNode != null ? jsonNode.textValue() : null;
    }

    /**
     * Parse command line.
     *
     * @param cmd command line.
     */
    public void parse(String cmd, String uuid) {
        command = null;
        action = null;
        resourceType = null;
        imageType = null;
        requestId = uuid;
        responsePath = null;
        name = null;
        json = null;

        envMap.clear();
        varMap.clear();
        otherArgs.clear();
        variables.clear();

        List<String> args = extractArgs(cmd);
        dockerCommand = args.contains("docker");
        int i = 0;
        String s;
        Pair<String, String> p;

        while (i < args.size()) {
            if ((s = getArgValue(args, i, "-v")) != null) {
                p = getPair("-v", s, ":");
                varMap.put(p.getValue(), p.getKey());
            } else if ((s = getArgValue(args, i, "-e")) != null) {
                p = getPair("-e", s, "=");
                envMap.put(p.getKey(), p.getValue());
            } else if ((s = getArgValue(args, i, "docker")) != null || (s = getArgValue(args, i, "python")) != null) {
                command = s;
            } else if ((s = getArgValue(args, i, "--action")) != null) {
                action = s;
            } else if ((s = getArgValue(args, i, "--name")) != null) {
                name = s;
            } else if ((s = getArgValue(args, i, "echo")) != null) {
                if (s.equals("-e")) {
                    if (i >= args.size()) {
                        throw new DatalabException("Argument \"echo -e\" detected but not have value");
                    }
                    s = args.get(i);
                    args.remove(i);
                }
                json = s;
            } else if ((s = getArgValue(args, i, "--result_path")) != null) {
                responsePath = s;
                varMap.put("/response", responsePath);
                args.remove(i);
            } else {
                i++;
            }
        }

        if (args.size() > 0) {
            otherArgs.addAll(args);
        }

        resourceType = envMap.get("conf_resource");
        if (isDockerCommand()) {
            imageType = getImageName(args);
            imageType = imageType.replace("docker.datalab-", "").replace(":latest", "");
        }
        responsePath = varMap.get("/response");

        variables.putAll(envMap);
        variables.putAll(getJsonVariables(json));
        variables.put("request_id", requestId);
        variables.put("instance_id", "i-" + requestId.replace("-", "").substring(0, 17));
        variables.put("cluster_id", "j-" + requestId.replace("-", "").substring(0, 13).toUpperCase());
        variables.put("notebook_id", requestId.replace("-", "").substring(17, 22));
    }

    public boolean isDockerCommand() {
        return dockerCommand;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("command", command)
                .add("action", action)
                .add("resourceType", resourceType)
                .add("imageType", imageType)
                .add("requestId", requestId)
                .add("responsePath", responsePath)
                .add("name", name)
                .add("environment", envMap)
                .add("variable", varMap)
                .add("others", otherArgs)
                .add("json", json)
                .toString();
    }
}
