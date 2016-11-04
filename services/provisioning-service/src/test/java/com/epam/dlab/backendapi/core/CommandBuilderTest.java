/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.core;

import com.epam.dlab.backendapi.core.docker.command.RunDockerCommand;
import com.epam.dlab.dto.computational.ComputationalCreateDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommandBuilderTest {

    String rootKeysVolume = "rkv";
    String responseVolume = "rv";
    String requestID = "rID";
    String toDescribe = "ubuntu";

    @Test
    public void testBuildCommand() throws JsonProcessingException {
        RunDockerCommand dockerBaseCommand = new RunDockerCommand()
                .withInteractive()
                .withAtach("STDIN")
                .withVolumeForRootKeys(rootKeysVolume)
                .withVolumeForResponse(responseVolume)
                .withRequestId(requestID)
                .withActionDescribe(toDescribe);


        ComputationalCreateDTO computationalCreateDTO = new ComputationalCreateDTO().withServiceBaseName("someName");

     /*   CreateEMRClusterParameters createEMRClusterParameters = CreateEMRClusterParameters.newCreateEMRClusterParameters()
                .confServiceBaseName("someName")
                .emrTimeout("10")
                .build();*/

        CommandBuilder commandBuilder = new CommandBuilder();
        String command = commandBuilder.buildCommand(dockerBaseCommand, computationalCreateDTO);
        System.out.println(command);

        assertEquals("echo -e '{\"conf_service_base_name\":\"someName\"}' | docker run -i -a STDIN -v rkv:/root/keys -v rv:/response -e \"request_id=rID\" ubuntu --action describe",
                command);
    }
}
