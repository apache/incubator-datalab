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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

@FunctionalInterface
public interface DockerCommands {
    String GET_IMAGES = new ImagesDockerCommand()
            .pipe(UnixCommand.awk("{print $1\":\"$2}"))
            .pipe(UnixCommand.sort())
            .pipe(UnixCommand.uniq())
            .pipe(UnixCommand.grep("datalab"))
            .pipe(UnixCommand.grep("none", "-v"))
            .pipe(UnixCommand.grep("base", "-v"))
            .pipe(UnixCommand.grep("ssn", "-v"))
            .pipe(UnixCommand.grep("edge", "-v"))
            .pipe(UnixCommand.grep("project", "-v"))
            .toCMD();

    String GET_RUNNING_CONTAINERS_FOR_USER = "docker ps --format \"{{.Names}}\" -f name=%s";

    ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    static String extractUUID(String fileName) {
        int beginIndex = fileName.lastIndexOf('_');
        int endIndex = fileName.lastIndexOf('.');
        beginIndex = beginIndex < 0 ? 0 : beginIndex + 1;
        if (endIndex < 0) {
            endIndex = fileName.length();
        }
        if (beginIndex > endIndex) {
            beginIndex = endIndex;
        }
        return fileName.substring(beginIndex, endIndex);
    }

    default String nameContainer(String... names) {
        return String.join("_", names) + "_" + System.currentTimeMillis();
    }

    String getResourceType();
}
