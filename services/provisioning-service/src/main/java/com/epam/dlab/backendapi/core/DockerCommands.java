/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.core;

import com.epam.dlab.backendapi.core.docker.command.ImagesDockerCommand;
import com.epam.dlab.backendapi.core.docker.command.UnixCommand;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static com.epam.dlab.backendapi.core.Constants.JSON_EXTENSION;

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

    ObjectMapper MAPPER = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);

    static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    static String extractUUID(String fileName) {
        return fileName.replace(JSON_EXTENSION, "");
    }
}
