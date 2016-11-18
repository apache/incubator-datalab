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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.CommandBuilder;
import com.epam.dlab.backendapi.core.CommandExecutor;
import com.epam.dlab.backendapi.core.DockerCommands;
import com.epam.dlab.backendapi.core.docker.command.DockerAction;
import com.epam.dlab.backendapi.core.docker.command.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.folderlistener.FileHandlerCallback;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.resources.handler.ComputationalCallbackHandler;
import com.epam.dlab.client.restclient.RESTService;
import com.epam.dlab.dto.computational.ComputationalBaseDTO;
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
    private FolderListenerExecutor folderListenerExecutor;
    @Inject
    private CommandExecutor commandExecuter;
    @Inject
    private CommandBuilder commandBuilder;
    @Inject
    private RESTService selfService;

    @Path("/create")
    @POST
    public String create(ComputationalCreateDTO dto) throws IOException, InterruptedException {
        LOGGER.debug("create computational resources cluster");
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallback(DockerAction.CREATE, uuid, dto));
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
                                    .withCredsSecurityGroupsIds(dto.getSecurityGroupIds())
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
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallback(DockerAction.TERMINATE, uuid, dto));
        try {
            commandExecuter.executeAsync(
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withRequestId(uuid)
                                    .withEmrClusterName(dto.getClusterName())
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

    private FileHandlerCallback getFileHandlerCallback(DockerAction action, String originalUuid, ComputationalBaseDTO dto) {
        return new ComputationalCallbackHandler(selfService, action, originalUuid, dto.getIamUserName(), dto.getExploratoryName(), dto.getComputationalName());
    }

}
