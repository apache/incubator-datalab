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

package com.epam.datalab.backendapi.core.docker.command;

import com.epam.datalab.backendapi.core.commands.ImagesDockerCommand;
import com.epam.datalab.backendapi.core.commands.UnixCommand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImagesDockerCommandTest {

    String GET_IMAGES = "docker images | awk '{print $1\":\"$2}' | sort | uniq | grep \"datalab\" | grep -v \"none\" | grep -v \"edge\"";

    @Test
    public void testBuildGetImagesCommand() {
        String getImagesCommand = new ImagesDockerCommand()
                .pipe(UnixCommand.awk("{print $1\":\"$2}"))
                .pipe(UnixCommand.sort())
                .pipe(UnixCommand.uniq())
                .pipe(UnixCommand.grep("datalab"))
                .pipe(UnixCommand.grep("none", "-v"))
                .pipe(UnixCommand.grep("edge", "-v"))
                .toCMD();
        assertEquals(GET_IMAGES, getImagesCommand);
    }
}
