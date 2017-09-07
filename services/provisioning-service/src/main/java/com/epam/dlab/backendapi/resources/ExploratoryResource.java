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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.*;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.core.response.handlers.ExploratoryCallbackHandler;
import com.epam.dlab.backendapi.core.response.handlers.ExploratoryGitCredsCallbackHandler;
import com.epam.dlab.backendapi.core.response.handlers.LibInstallCallbackHandler;
import com.epam.dlab.dto.exploratory.ExploratoryBaseDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.epam.dlab.dto.exploratory.ExploratoryLibInstallDTO;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/exploratory")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExploratoryResource implements DockerCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExploratoryResource.class);

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

    @Path("/lib_install")
    @POST
    public String libInstall(@Auth UserInfo ui, ExploratoryLibInstallDTO dto) throws IOException, InterruptedException {
        return action(ui.getName(), dto, DockerAction.LIB_INSTALL);
    }

    @Path("/git_creds")
    @POST
    public String gitCredsUpdate(@Auth UserInfo ui, ExploratoryGitCredsUpdateDTO dto) throws IOException, InterruptedException {
        return action(ui.getName(), dto, DockerAction.GIT_CREDS);
    }

    private String action(String username, ExploratoryBaseDTO<?> dto, DockerAction action) throws IOException, InterruptedException {
        LOGGER.debug("{} exploratory environment", action);
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallback(action, uuid, dto));

        RunDockerCommand runDockerCommand = new RunDockerCommand()
                .withInteractive()
                .withName(nameContainer(dto.getEdgeUserName(), action, dto.getExploratoryName()))
                .withVolumeForRootKeys(configuration.getKeyDirectory())
                .withVolumeForResponse(configuration.getImagesDirectory())
                .withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
                .withResource(getResourceType())
                .withRequestId(uuid)
                .withConfKeyName(configuration.getAdminKey())
                .withImage(dto.getNotebookImage())
                .withAction(action);

        commandExecutor.executeAsync(username, uuid, commandBuilder.buildCommand(runDockerCommand, dto));
        return uuid;
    }

	private FileHandlerCallback getFileHandlerCallback(DockerAction action, String uuid, ExploratoryBaseDTO<?> dto) {
    	switch (action) {
    		case LIB_INSTALL:
        		return new LibInstallCallbackHandler(selfService, action, uuid, dto.getAwsIamUser(), (ExploratoryLibInstallDTO) dto);
    		case GIT_CREDS:
    			return new ExploratoryGitCredsCallbackHandler(selfService, action, uuid, dto.getAwsIamUser(), dto.getExploratoryName());
    		default:
    			break;
    	}
		return new ExploratoryCallbackHandler(selfService, action, uuid, dto.getAwsIamUser(), dto.getExploratoryName());
    }

    private String nameContainer(String user, DockerAction action, String name) {
        return nameContainer(user, action.toString(), "exploratory", name);
    }

    public String getResourceType() {
        return Directories.NOTEBOOK_LOG_DIRECTORY;
    }
}
