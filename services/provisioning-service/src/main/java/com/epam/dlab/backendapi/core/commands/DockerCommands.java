/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.backendapi.core.commands;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

public interface DockerCommands {
    String GET_IMAGES = new ImagesDockerCommand()
            .pipe(UnixCommand.awk("{print $1\":\"$2}"))
            .pipe(UnixCommand.sort())
            .pipe(UnixCommand.uniq())
            .pipe(UnixCommand.grep("dlab"))
            .pipe(UnixCommand.grep("none", "-v"))
            .pipe(UnixCommand.grep("base", "-v"))
            .pipe(UnixCommand.grep("ssn", "-v"))
            .pipe(UnixCommand.grep("edge", "-v"))
            .toCMD();

    ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);

    static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    static String extractUUID(String fileName) {
        Integer beginIndex = fileName.lastIndexOf('_');
        Integer endIndex = fileName.lastIndexOf('.');
        beginIndex = beginIndex < 0 ? 0 : beginIndex + 1;
        if(endIndex < 0) endIndex = fileName.length();
        if (beginIndex > endIndex) beginIndex = endIndex;
        return fileName.substring(beginIndex, endIndex);
    }

    default String nameContainer(String... names) {
        return String.join("_", names) + "_" + System.currentTimeMillis();
    }

    String getResourceType();
}
