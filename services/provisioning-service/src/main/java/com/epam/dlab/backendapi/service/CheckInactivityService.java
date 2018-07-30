/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.handlers.CheckInactivityClustersCallbackHandler;
import com.epam.dlab.dto.computational.CheckInactivityClusterCallbackDTO;
import com.epam.dlab.dto.computational.ComputationalCheckInactivityDTO;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Singleton
public class CheckInactivityService extends DockerService implements DockerCommands {

	private static final String CHECK_INACTIVITY_CLUSTERS_ACTION = "check_inactivity_clusters";

	public void checkClusterAction(String userName, List<ComputationalCheckInactivityDTO> instances,
								   DockerAction action) {
		log.debug("Admin {} is checking inactivity for clusters...", userName);
		log.debug("Obtained {} instances: {}", instances.size(), instances);

		long count = instances
				.stream()
				.map(dto -> buildCallbackDTO(getUuid(), dto))
				.peek(callbackDto -> startCallbackListener(userName, callbackDto))
				.peek(callbackDto ->
						runDockerCmd(userName, callbackDto.getId(), buildRunDockerCommand(callbackDto, action),
								buildDockerCommandDTO(callbackDto)))
				.count();
		log.debug("Executed {} Docker commands", count);
	}

	private String getUuid() {
		return DockerCommands.generateUUID();
	}

	private void runDockerCmd(String userName, String uuid, RunDockerCommand runDockerCommand,
							  CheckInactivityClusterCallbackDTO callbackDto) {
		try {
			final String command = commandBuilder.buildCommand(runDockerCommand, callbackDto);
			log.trace("Docker command: {}", command);
			commandExecutor.executeAsync(userName, uuid, command);
		} catch (Exception e) {
			log.error("Exception occured during reuploading key: {} for command {}", e.getLocalizedMessage(),
					runDockerCommand.toCMD());
		}
	}

	private void startCallbackListener(String userName, CheckInactivityClusterCallbackDTO dto) {
		folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
				configuration.getKeyLoaderPollTimeout(),
				new CheckInactivityClustersCallbackHandler(
						selfService, ApiCallbacks.CHECK_INACTIVITY_CLUSTERS_URI, userName, dto));
	}

	@Override
	public String getResourceType() {
		return Directories.EDGE_LOG_DIRECTORY;
	}

	private RunDockerCommand buildRunDockerCommand(CheckInactivityClusterCallbackDTO callbackDto,
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

	private CheckInactivityClusterCallbackDTO buildCallbackDTO(String uuid, ComputationalCheckInactivityDTO dto) {
		return new CheckInactivityClusterCallbackDTO()
				.withId(uuid)
				.withEdgeUserName(dto.getEdgeUserName())
				.withServiceBaseName(dto.getServiceBaseName())
				.withConfOsFamily(dto.getConfOsFamily())
				.withExploratoryId(dto.getExploratoryId())
				.withExploratoryName(dto.getExploratoryName())
				.withComputationalName(dto.getComputationalName())
				.withType(dto.getType());
	}

	private CheckInactivityClusterCallbackDTO buildDockerCommandDTO(CheckInactivityClusterCallbackDTO dto) {
		return new CheckInactivityClusterCallbackDTO()
				.withEdgeUserName(dto.getEdgeUserName())
				.withServiceBaseName(dto.getServiceBaseName())
				.withConfOsFamily(dto.getConfOsFamily());
	}

	private String getContainerName(CheckInactivityClusterCallbackDTO callbackDto) {
		return nameContainer(callbackDto.getEdgeUserName(), CHECK_INACTIVITY_CLUSTERS_ACTION,
				callbackDto.getExploratoryName(),
				callbackDto.getComputationalName());
	}
}
