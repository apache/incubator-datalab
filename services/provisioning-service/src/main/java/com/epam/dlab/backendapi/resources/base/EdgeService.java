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

package com.epam.dlab.backendapi.resources.base;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.*;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.KeyAPI;
import com.epam.dlab.util.UsernameUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EdgeService implements DockerCommands {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Inject
	protected RESTService selfService;
	@Inject
	private ProvisioningServiceApplicationConfiguration configuration;
	@Inject
	private FolderListenerExecutor folderListenerExecutor;
	@Inject
	private ICommandExecutor commandExecutor;
	@Inject
	private CommandBuilder commandBuilder;

	@Override
	public String getResourceType() {
		return Directories.EDGE_LOG_DIRECTORY;
	}

	protected String action(String username, ResourceSysBaseDTO<?> dto, String iamUser, String callbackURI,
							DockerAction action) throws JsonProcessingException {
		logger.debug("{} EDGE node for user {}: {}", action, username, dto);
		String uuid = DockerCommands.generateUUID();

		folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
				configuration.getKeyLoaderPollTimeout(),
				getFileHandlerCallback(action, uuid, iamUser, callbackURI));

		RunDockerCommand runDockerCommand = new RunDockerCommand()
				.withInteractive()
				.withName(nameContainer(dto.getEdgeUserName(), action))
				.withVolumeForRootKeys(getKeyDirectory())
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

	protected abstract FileHandlerCallback getFileHandlerCallback(DockerAction action,
																  String uuid, String user, String callbackURI);

	private String nameContainer(String user, DockerAction action) {
		return nameContainer(user, action.toString(), getResourceType());
	}

	protected String getKeyDirectory() {
		return configuration.getKeyDirectory();
	}

	protected String getKeyFilename(String edgeUserName) {
		return UsernameUtils.replaceWhitespaces(edgeUserName) + KeyAPI.KEY_EXTENTION;
	}

}
