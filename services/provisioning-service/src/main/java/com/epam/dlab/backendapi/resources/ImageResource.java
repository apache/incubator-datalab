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
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.handlers.ImageCreateCallbackHandler;
import com.epam.dlab.backendapi.service.impl.DockerService;
import com.epam.dlab.dto.exploratory.ExploratoryImageDTO;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(ExploratoryAPI.EXPLORATORY_IMAGE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ImageResource extends DockerService implements DockerCommands {

	@POST
	public Response createImage(@Auth UserInfo ui, ExploratoryImageDTO image) throws JsonProcessingException {
		final String uuid = DockerCommands.generateUUID();

		folderListenerExecutor.start(configuration.getImagesDirectory(), configuration.getResourceStatusPollTimeout(),
				new ImageCreateCallbackHandler(selfService, uuid, DockerAction.CREATE_IMAGE, image));
		String command = commandBuilder.buildCommand(getDockerCommand(DockerAction.CREATE_IMAGE, uuid, image), image);
		commandExecutor.executeAsync(ui.getName(), uuid, command);
		log.debug("Docker command: " + command);
		return Response.accepted(uuid).build();
	}


	@Override
	public String getResourceType() {
		return Directories.NOTEBOOK_LOG_DIRECTORY;
	}

	private RunDockerCommand getDockerCommand(DockerAction action, String uuid, ExploratoryImageDTO image) {
		return new RunDockerCommand()
				.withInteractive()
				.withVolumeForRootKeys(configuration.getKeyDirectory())
				.withVolumeForResponse(configuration.getImagesDirectory())
				.withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
				.withRequestId(uuid)
				.withConfKeyName(configuration.getAdminKey())
				.withAction(action)
				.withResource(getResourceType())
				.withImage(image.getNotebookImage())
				.withName(nameContainer(image.getEdgeUserName(), action.toString(), image.getImageName()));
	}
}
