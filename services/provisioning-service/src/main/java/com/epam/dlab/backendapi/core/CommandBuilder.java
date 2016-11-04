/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.core;

import com.epam.dlab.backendapi.core.docker.command.RunDockerCommand;
import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.generate_json.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CommandBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandBuilder.class);

    public String buildCommand(RunDockerCommand runDockerCommand) throws JsonProcessingException {
        return buildCommand(runDockerCommand, null);
    }

    public String buildCommand(RunDockerCommand runDockerCommand, ResourceBaseDTO resourceBaseDTO) throws JsonProcessingException {
        StringBuilder builder = new StringBuilder();
        if (resourceBaseDTO != null) {
            builder.append("echo -e '");
            try {
                builder.append(new JsonGenerator().generateJson(resourceBaseDTO));
            } catch (JsonProcessingException e) {
                LOGGER.error("ERROR generating json from dockerRunParameters: " + e.getMessage());
                throw e;
            }
            builder.append('\'');
            builder.append(" | ");
        }
        builder.append(runDockerCommand.toCMD());
        return builder.toString();
    }
}
