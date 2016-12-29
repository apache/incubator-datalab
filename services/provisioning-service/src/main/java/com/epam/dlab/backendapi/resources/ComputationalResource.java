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
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.ICommandExecutor;
import com.epam.dlab.backendapi.core.commands.*;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.core.response.handlers.ComputationalCallbackHandler;
import com.epam.dlab.dto.computational.ComputationalBaseDTO;
import com.epam.dlab.dto.computational.ComputationalCreateDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static com.epam.dlab.backendapi.core.commands.DockerAction.CREATE;
import static com.epam.dlab.backendapi.core.commands.DockerAction.TERMINATE;

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
    private ICommandExecutor commandExecuter;
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
                getFileHandlerCallback(CREATE, uuid, dto));
        try {
            long timeout = configuration.getResourceStatusPollTimeout().toSeconds();
            commandExecuter.executeAsync(
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withName(nameContainer(dto.getEdgeUserName(), CREATE, dto.getComputationalName()))
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
                                    .withResource(getResourceType())
                                    .withRequestId(uuid)
                                    .withEc2Role(configuration.getEmrEC2RoleDefault())
                                    .withEmrTimeout(Long.toString(timeout))
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
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallback(TERMINATE, uuid, dto));
        try {
            commandExecuter.executeAsync(
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withName(nameContainer(dto.getEdgeUserName(), TERMINATE, dto.getComputationalName()))
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
                                    .withResource(getResourceType())
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

    private FileHandlerCallback getFileHandlerCallback(DockerAction action, String originalUuid, ComputationalBaseDTO dto) {
        return new ComputationalCallbackHandler(selfService, action, originalUuid, dto.getIamUserName(), dto.getExploratoryName(), dto.getComputationalName());
    }

    private String nameContainer(String user, DockerAction action, String name) {
        return nameContainer(user, action.toString(), "computational", name);
    }

    public String getResourceType() {
        return Directories.EMR_LOG_DIRECTORY;
    }
}
