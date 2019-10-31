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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.handlers.ComputationalCallbackHandler;
import com.epam.dlab.backendapi.core.response.handlers.ComputationalConfigure;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.computational.ComputationalClusterConfigDTO;
import com.epam.dlab.dto.computational.ComputationalStartDTO;
import com.epam.dlab.dto.computational.ComputationalStopDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static com.epam.dlab.backendapi.core.commands.DockerAction.*;

@Singleton
public class SparkClusterService extends DockerService implements DockerCommands {

	private static final DataEngineType SPARK_ENGINE = DataEngineType.SPARK_STANDALONE;

	@Inject
	private ComputationalConfigure computationalConfigure;

	public String create(UserInfo ui, ComputationalBase<?> dto) {
		return action(ui, dto, CREATE);
	}

	public String terminate(UserInfo ui, ComputationalTerminateDTO dto) {
		return action(ui, dto, TERMINATE);
	}

	public String stop(UserInfo ui, ComputationalStopDTO dto) {
		return action(ui, dto, STOP);
	}

	public String start(UserInfo ui, ComputationalStartDTO dto) {
		return action(ui, dto, START);
	}

	public String updateConfig(UserInfo ui, ComputationalClusterConfigDTO clusterConfigDTO) {
		String uuid = DockerCommands.generateUUID();
		folderListenerExecutor.start(configuration.getImagesDirectory(),
				configuration.getResourceStatusPollTimeout(),
				getFileHandlerCallback(RECONFIGURE_SPARK, uuid, clusterConfigDTO));
		runReconfigureSparkDockerCommand(ui, clusterConfigDTO, uuid);
		return uuid;
	}

	private void runReconfigureSparkDockerCommand(UserInfo ui, ComputationalClusterConfigDTO clusterConfigDTO,
												  String uuid) {
		try {
			final RunDockerCommand dockerCommand = new RunDockerCommand()
					.withInteractive()
					.withName(nameContainer(clusterConfigDTO.getEdgeUserName(), RECONFIGURE_SPARK,
							clusterConfigDTO.getExploratoryName(),
							clusterConfigDTO.getComputationalName()))
					.withVolumeForRootKeys(configuration.getKeyDirectory())
					.withVolumeForResponse(configuration.getImagesDirectory())
					.withVolumeForLog(configuration.getDockerLogDirectory(), SPARK_ENGINE.getName())
					.withResource(SPARK_ENGINE.getName())
					.withRequestId(uuid)
					.withConfKeyName(configuration.getAdminKey())
					.withImage(DataEngineType.getDockerImageName(SPARK_ENGINE))
					.withAction(RECONFIGURE_SPARK);

			commandExecutor.executeAsync(ui.getName(), uuid, commandBuilder.buildCommand(dockerCommand,
					clusterConfigDTO));
		} catch (JsonProcessingException e) {
			throw new DlabException("Could not" + RECONFIGURE_SPARK.toString() + "computational resources cluster", e);
		}
	}

	private String action(UserInfo ui, ComputationalBase<?> dto, DockerAction action) {
		String uuid = DockerCommands.generateUUID();
		folderListenerExecutor.start(configuration.getImagesDirectory(),
				configuration.getResourceStatusPollTimeout(),
				getFileHandlerCallback(action, uuid, dto));
		try {
			final RunDockerCommand dockerCommand = new RunDockerCommand()
					.withInteractive()
					.withName(nameContainer(dto.getEdgeUserName(), action, dto.getExploratoryName(),
							dto.getComputationalName()))
					.withVolumeForRootKeys(configuration.getKeyDirectory())
					.withVolumeForResponse(configuration.getImagesDirectory())
					.withVolumeForLog(configuration.getDockerLogDirectory(), SPARK_ENGINE.getName())
					.withResource(SPARK_ENGINE.getName())
					.withRequestId(uuid)
					.withConfKeyName(configuration.getAdminKey())
					.withImage(DataEngineType.getDockerImageName(SPARK_ENGINE))
					.withAction(action);

			commandExecutor.executeAsync(ui.getName(), uuid, commandBuilder.buildCommand(dockerCommand, dto));
		} catch (JsonProcessingException e) {
			throw new DlabException("Could not" + action.toString() + "computational resources cluster", e);
		}

		return uuid;
	}

	private FileHandlerCallback getFileHandlerCallback(DockerAction action, String uuid, ComputationalBase<?> dto) {
		return new ComputationalCallbackHandler(computationalConfigure, selfService, action, uuid, dto);
	}

	private String nameContainer(String user, DockerAction action, String exploratoryName, String name) {
		return nameContainer(user, action.toString(), "computational", exploratoryName, name);
	}

	@Override
	public String getResourceType() {
		return Directories.DATA_ENGINE_LOG_DIRECTORY;
	}
}
