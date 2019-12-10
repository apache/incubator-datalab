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
package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.handlers.CheckInactivityCallbackHandler;
import com.epam.dlab.backendapi.service.CheckInactivityService;
import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.ComputationalCheckInactivityDTO;
import com.epam.dlab.dto.exploratory.ExploratoryCheckInactivityAction;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CheckInactivityServiceImpl extends DockerService implements CheckInactivityService, DockerCommands {


	@Override
	public String checkComputationalInactivity(String userName, ComputationalCheckInactivityDTO dto) {
		String uuid = DockerCommands.generateUUID();
		startComputationalCallbackListener(userName, dto, uuid);
		final RunDockerCommand dockerCommand = new RunDockerCommand()
				.withInteractive()
				.withRemove()
				.withName(nameContainer(uuid, DockerAction.CHECK_INACTIVITY.toString()))
				.withVolumeForRootKeys(configuration.getKeyDirectory())
				.withVolumeForResponse(configuration.getKeyLoaderDirectory())
				.withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
				.withResource(DataEngineType.fromDockerImageName(dto.getImage()) == DataEngineType.SPARK_STANDALONE ?
						Directories.DATA_ENGINE_LOG_DIRECTORY :
						Directories.DATA_ENGINE_SERVICE_LOG_DIRECTORY)
				.withRequestId(uuid)
				.withConfKeyName(configuration.getAdminKey())
				.withImage(dto.getNotebookImage())
				.withAction(DockerAction.CHECK_INACTIVITY);
		runDockerCmd(userName, uuid, dockerCommand, dto);
		return uuid;
	}

	@Override
	public String checkExploratoryInactivity(String userName, ExploratoryCheckInactivityAction dto) {
		String uuid = DockerCommands.generateUUID();
		startExploratoryCallbackListener(userName, dto, uuid);
		final RunDockerCommand dockerCommand = new RunDockerCommand()
				.withInteractive()
				.withRemove()
				.withName(nameContainer(uuid, DockerAction.CHECK_INACTIVITY.toString()))
				.withVolumeForRootKeys(configuration.getKeyDirectory())
				.withVolumeForResponse(configuration.getKeyLoaderDirectory())
				.withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
				.withResource(Directories.NOTEBOOK_LOG_DIRECTORY)
				.withRequestId(uuid)
				.withConfKeyName(configuration.getAdminKey())
				.withImage(dto.getNotebookImage())
				.withAction(DockerAction.CHECK_INACTIVITY);
		runDockerCmd(userName, uuid, dockerCommand, dto);
		return uuid;
	}

	private void startComputationalCallbackListener(String userName, ComputationalCheckInactivityDTO dto,
													String uuid) {
		final CheckInactivityCallbackHandler handler = new CheckInactivityCallbackHandler(
				selfService, ApiCallbacks.CHECK_INACTIVITY_COMPUTATIONAL_URI, userName, uuid,
				dto.getExploratoryName(), dto.getComputationalName());
		folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
				configuration.getKeyLoaderPollTimeout(), handler);
	}

	private void startExploratoryCallbackListener(String userName, ExploratoryCheckInactivityAction dto, String uuid) {
		final CheckInactivityCallbackHandler handler = new CheckInactivityCallbackHandler(
				selfService, ApiCallbacks.CHECK_INACTIVITY_EXPLORATORY_URI, userName, uuid,
				dto.getExploratoryName());
		folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
				configuration.getKeyLoaderPollTimeout(), handler);
	}

	private void runDockerCmd(String userName, String uuid, RunDockerCommand dockerCmd, ResourceBaseDTO<?> dto) {
		try {
			final String command = commandBuilder.buildCommand(dockerCmd, dto);
			log.trace("Docker command: {}", command);
			commandExecutor.executeAsync(userName, uuid, command);
		} catch (Exception e) {
			log.error("Exception occured during reuploading key: {} for command {}", e.getLocalizedMessage(),
					dockerCmd.toCMD());
		}
	}

	@Override
	public String getResourceType() {
		return Directories.NOTEBOOK_LOG_DIRECTORY;
	}
}
