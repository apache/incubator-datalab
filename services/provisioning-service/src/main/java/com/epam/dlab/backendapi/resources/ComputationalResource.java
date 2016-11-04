/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.CommandBuilder;
import com.epam.dlab.backendapi.core.CommandExecutor;
import com.epam.dlab.backendapi.core.DockerCommands;
import com.epam.dlab.backendapi.core.docker.command.RunDockerCommand;
import com.epam.dlab.dto.computational.ComputationalCreateDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/computational")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ComputationalResource implements DockerCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputationalResource.class);

    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;
    @Inject
    private CommandExecutor commandExecuter;

    @Inject
    private CommandBuilder commandBuilder;

    @Path("/create")
    @POST
    public String create(ComputationalCreateDTO dto) throws IOException, InterruptedException {
        LOGGER.debug("create computational resources cluster");
        String uuid = DockerCommands.generateUUID();
        try {
            commandExecuter.executeAsync(
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withRequestId(uuid)
                                    .withEc2Role(configuration.getEmrEC2RoleDefault())
                                    .withServiceRole(configuration.getEmrServiceRoleDefault())
                                    .withCredsKeyName(configuration.getAdminKey())
                                    .withActionCreate(configuration.getEmrImage()),
                            dto
                    )
            );
        } catch (Throwable t) {
            throw new DlabException("Could not create computational resource cluster", t);
        }
        return uuid;
    }

    @Path("/terminate")
    @POST
    public String terminate(ComputationalTerminateDTO dto) throws IOException, InterruptedException {
        LOGGER.debug("terminate computational resources cluster");
        String uuid = DockerCommands.generateUUID();
        try {
            commandExecuter.executeAsync(
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withRequestId(uuid)
                                    .withCredsKeyName(configuration.getAdminKey())
                                    .withActionTerminate(configuration.getEmrImage()),
                            dto
                    )
            );
        } catch (Throwable t) {
            throw new DlabException("Could not terminate computational resources cluster", t);
        }
        return uuid;
    }
}
