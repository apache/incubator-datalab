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

import static com.epam.dlab.rest.contracts.ApiCallbacks.EDGE;
import static com.epam.dlab.rest.contracts.ApiCallbacks.KEY_LOADER;
import static com.epam.dlab.rest.contracts.ApiCallbacks.STATUS_URI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.ICommandExecutor;
import com.epam.dlab.backendapi.core.commands.CommandBuilder;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.core.response.handlers.EdgeCallbackHandler;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.dto.keyload.UploadFileDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.EdgeAPI;
import com.google.inject.Inject;

import io.dropwizard.auth.Auth;

@Path(EdgeAPI.EDGE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EdgeResource implements DockerCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdgeResource.class);

    private static final String KEY_EXTENTION = ".pub";

    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;
    @Inject
    private FolderListenerExecutor folderListenerExecutor;
    @Inject
    private ICommandExecutor commandExecutor;
    @Inject
    private CommandBuilder commandBuilder;
    @Inject
    private RESTService selfService;

    @POST
    @Path("/create")
    public String create(@Auth UserInfo ui, UploadFileDTO dto) throws IOException, InterruptedException {
        LOGGER.debug("Load key for user {}", ui.getName());
        saveKeyToFile(dto);
        return action(ui.getName(), dto.getEdge(), KEY_LOADER, DockerAction.CREATE);
    }

    @POST
    @Path("/start")
    public String start(@Auth UserInfo ui, ResourceSysBaseDTO<?> dto) throws IOException, InterruptedException {
    	return action(ui.getName(), dto, EDGE + STATUS_URI, DockerAction.START);
    }

    @POST
    @Path("/stop")
    public String stop(@Auth UserInfo ui, ResourceSysBaseDTO<?> dto) throws IOException, InterruptedException {
    	return action(ui.getName(), dto, EDGE + STATUS_URI, DockerAction.STOP);
    }

    protected String action(String username, ResourceSysBaseDTO<?> dto, String callbackURI, DockerAction action) throws IOException, InterruptedException {
    	LOGGER.debug("{} EDGE node for user {}: {}", action, username, dto);
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
        		configuration.getKeyLoaderPollTimeout(),
                getFileHandlerCallback(action, uuid, dto.getAwsIamUser(), callbackURI));
        RunDockerCommand runDockerCommand = new RunDockerCommand()
                .withInteractive()
                .withName(nameContainer(dto.getEdgeUserName(), action))
                .withVolumeForRootKeys(configuration.getKeyDirectory())
                .withVolumeForResponse(configuration.getKeyLoaderDirectory())
                .withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
                .withResource(getResourceType())
                .withRequestId(uuid)
                .withConfKeyName(configuration.getAdminKey())
                .withImage(configuration.getEdgeImage())
                .withAction(action);

        commandExecutor.executeAsync(username, uuid, commandBuilder.buildCommand(runDockerCommand, dto));
        return uuid;
    }

    private FileHandlerCallback getFileHandlerCallback(DockerAction action, String uuid, String user, String callbackURI) {
        return new EdgeCallbackHandler(selfService, action, uuid, user, callbackURI);
    }

    private String nameContainer(String user, DockerAction action) {
        return nameContainer(user, action.toString(), getResourceType());
    }
    
    @Override
    public String getResourceType() {
        return Directories.EDGE_LOG_DIRECTORY;
    }

    private void saveKeyToFile(UploadFileDTO dto) throws IOException {
    	java.nio.file.Path keyFilePath = Paths.get(configuration.getKeyDirectory(), dto.getEdge().getEdgeUserName() + KEY_EXTENTION).toAbsolutePath();
    	LOGGER.debug("Saving key to {}", keyFilePath.toString());
    	try {
    		com.google.common.io.Files.createParentDirs(new File(keyFilePath.toString()));
		} catch (IOException e) {
			throw new DlabException("Can't create key folder " + keyFilePath + ": " + e.getLocalizedMessage(), e);
		}
        Files.write(keyFilePath, dto.getContent().getBytes());
    }
}
