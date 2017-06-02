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

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.MetadataHolder;
import com.epam.dlab.backendapi.core.commands.CommandBuilder;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.ICommandExecutor;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.core.response.handlers.LibListCallbackHandler;
import com.epam.dlab.dto.exploratory.ExploratoryActionDTO;
import com.epam.dlab.dto.imagemetadata.ImageMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ImageType;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;

import io.dropwizard.auth.Auth;

@Path("/docker")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DockerResource implements DockerCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerResource.class);

    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;
    @Inject
    private MetadataHolder metadataHolder;
    @Inject
    private ICommandExecutor commandExecutor;
    @Inject
    private FolderListenerExecutor folderListenerExecutor;
    @Inject
    private CommandBuilder commandBuilder;
    @Inject
    private RESTService selfService;

    @GET
    @Path("{type}")
    public Set<ImageMetadataDTO> getDockerImages(@Auth UserInfo ui, @PathParam("type") String type) throws
            IOException, InterruptedException {
        LOGGER.debug("docker statuses asked for {}", type);
        return metadataHolder
                .getMetadata(ImageType.valueOf(type.toUpperCase()));
    }

    @Path("/run")
    @POST
    public String run(@Auth UserInfo ui, String image) throws IOException, InterruptedException {
        LOGGER.debug("run docker image {}", image);
        String uuid = DockerCommands.generateUUID();
        commandExecutor.executeAsync(
                ui.getName(),
                uuid,
                new RunDockerCommand()
                        .withName(nameContainer("image", "runner"))
                        .withVolumeForRootKeys(configuration.getKeyDirectory())
                        .withVolumeForResponse(configuration.getImagesDirectory())
                        .withRequestId(uuid)
                        .withDryRun()
                        .withActionRun(image)
                        .toCMD()
        );
        return uuid;
    }

    @Path("/lib_list")
    @POST
    public String getLibList(@Auth UserInfo ui, ExploratoryActionDTO<?> dto) throws IOException, InterruptedException {
        LOGGER.debug("Listing of libs for user {} with condition {}", ui.getName(), dto);
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
        		new LibListCallbackHandler(selfService, DockerAction.LIB_LIST, uuid, dto.getAwsIamUser(), dto.getNotebookImage()));

        RunDockerCommand runDockerCommand = new RunDockerCommand()
                .withInteractive()
                .withName(nameContainer(dto.getEdgeUserName(), DockerAction.LIB_LIST.toString(), dto.getExploratoryName()))
                .withVolumeForRootKeys(configuration.getKeyDirectory())
                .withVolumeForResponse(configuration.getImagesDirectory())
                .withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
                .withResource(getResourceType())
                .withRequestId(uuid)
                .withConfKeyName(configuration.getAdminKey())
                .withImage(dto.getNotebookImage())
                .withAction(DockerAction.LIB_LIST);

        commandExecutor.executeAsync(ui.getName(), uuid, commandBuilder.buildCommand(runDockerCommand, dto));
        return uuid;
    }

    public String getResourceType() {
        return Directories.NOTEBOOK_LOG_DIRECTORY;
    }
}
