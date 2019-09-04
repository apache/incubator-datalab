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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.handlers.ExploratoryGitCredsCallbackHandler;
import com.epam.dlab.backendapi.service.impl.DockerService;
import com.epam.dlab.dto.exploratory.ExploratoryBaseDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/exploratory")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class GitExploratoryResource extends DockerService implements DockerCommands {

	@Path("/git_creds")
	@POST
	public String gitCredsUpdate(@Auth UserInfo ui, ExploratoryGitCredsUpdateDTO dto) throws JsonProcessingException {
		return action(ui.getName(), dto, DockerAction.GIT_CREDS);
	}

	private String action(String username, ExploratoryBaseDTO<?> dto, DockerAction action) throws JsonProcessingException {
		log.debug("{} exploratory environment", action);
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
		return new ExploratoryGitCredsCallbackHandler(selfService, action, uuid, dto.getCloudSettings().getIamUser(), dto.getExploratoryName());
	}

	private String nameContainer(String user, DockerAction action, String name) {
		return nameContainer(user, action.toString(), "exploratory", name);
	}

	public String getResourceType() {
		return Directories.NOTEBOOK_LOG_DIRECTORY;
	}
}
