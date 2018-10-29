/*
 * **************************************************************************
 *
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ***************************************************************************
 */
package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.handlers.CheckInactivityCallbackHandler;
import com.epam.dlab.dto.computational.CheckInactivityCallbackDTO;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Singleton
public class CheckInactivityService extends DockerService implements DockerCommands {

	private static final String CHECK_INACTIVITY_CLUSTERS_ACTION = "check_inactivity_clusters";

	public String checkClusterAction(String userName, List<EnvResource> clusters, DockerAction action) {
		log.debug("Admin {} is checking inactivity for resources...", userName);
		log.debug("Obtained {} resources: {}", clusters.size(), clusters);
		String uuid = getUuid();
		CheckInactivityCallbackDTO dto = buildCallbackDTO(uuid, clusters);
		startCallbackListener(userName, dto);
		RunDockerCommand runDockerCommand = buildRunDockerCommand(dto, action);
		runDockerCmd(userName, uuid, runDockerCommand, dto);
		return uuid;
	}

	private String getUuid() {
		return DockerCommands.generateUUID();
	}

	private void runDockerCmd(String userName, String uuid, RunDockerCommand runDockerCommand,
							  CheckInactivityCallbackDTO callbackDto) {
		try {
			final String command = commandBuilder.buildCommand(runDockerCommand, callbackDto);
			log.trace("Docker command: {}", command);
			commandExecutor.executeAsync(userName, uuid, command);
		} catch (Exception e) {
			log.error("Exception occured during reuploading key: {} for command {}", e.getLocalizedMessage(),
					runDockerCommand.toCMD());
		}
	}

	private void startCallbackListener(String userName, CheckInactivityCallbackDTO dto) {
		folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
				configuration.getKeyLoaderPollTimeout(),
				new CheckInactivityCallbackHandler(
						selfService, ApiCallbacks.CHECK_INACTIVITY_CLUSTERS_URI, userName, dto.getId()));
	}

	@Override
	public String getResourceType() {
		return Directories.EDGE_LOG_DIRECTORY;
	}

	private RunDockerCommand buildRunDockerCommand(CheckInactivityCallbackDTO callbackDto,
												   DockerAction action) {
		return new RunDockerCommand()
				.withInteractive()
				.withName(getContainerName(callbackDto))
				.withVolumeForRootKeys(configuration.getKeyDirectory())
				.withVolumeForResponse(configuration.getKeyLoaderDirectory())
				.withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
				.withResource("computational")
				.withRequestId(callbackDto.getId())
				.withConfKeyName(configuration.getAdminKey())
				.withImage(configuration.getEdgeImage())
				.withAction(action);
	}

	private CheckInactivityCallbackDTO buildCallbackDTO(String uuid, List<EnvResource> clusters) {
		return new CheckInactivityCallbackDTO()
				.withId(uuid)
				.withClusters(clusters);
	}

	private String getContainerName(CheckInactivityCallbackDTO callbackDto) {
		return nameContainer(callbackDto.getId(), CHECK_INACTIVITY_CLUSTERS_ACTION);
	}
}
