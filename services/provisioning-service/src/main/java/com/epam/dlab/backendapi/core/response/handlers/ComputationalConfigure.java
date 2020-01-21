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

package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.*;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.dto.aws.computational.SparkComputationalCreateAws;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.gcp.computational.SparkComputationalCreateGcp;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import static com.epam.dlab.backendapi.core.commands.DockerAction.CONFIGURE;

@Slf4j
@Singleton
public class ComputationalConfigure implements DockerCommands {
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

	public String configure(String uuid, ComputationalBase<?> dto) {
		switch (configuration.getCloudProvider()) {
			case AWS:
				if (dto instanceof SparkComputationalCreateAws) {
					return runConfigure(uuid, dto, DataEngineType.SPARK_STANDALONE);
				} else {
					return runConfigure(uuid, dto, DataEngineType.CLOUD_SERVICE);
				}
			case AZURE:
				return runConfigure(uuid, dto, DataEngineType.SPARK_STANDALONE);
			case GCP:
				if (dto instanceof SparkComputationalCreateGcp) {
					return runConfigure(uuid, dto, DataEngineType.SPARK_STANDALONE);
				} else {
					return runConfigure(uuid, dto, DataEngineType.CLOUD_SERVICE);
				}

			default:
				throw new IllegalStateException(String.format("Wrong configuration of cloud provider %s %s",
						configuration.getCloudProvider(), dto));
		}
	}

	private String runConfigure(String uuid, ComputationalBase<?> dto, DataEngineType dataEngineType) {
		log.debug("Configure computational resources {} for user {}: {}", dto.getComputationalName(), dto
				.getEdgeUserName(), dto);
		folderListenerExecutor.start(
				configuration.getImagesDirectory(),
				configuration.getResourceStatusPollTimeout(),
				getFileHandlerCallback(CONFIGURE, uuid, dto));
		try {
			commandExecutor.executeAsync(
					dto.getEdgeUserName(),
					uuid,
					commandBuilder.buildCommand(
							new RunDockerCommand()
									.withInteractive()
									.withName(nameContainer(dto.getEdgeUserName(), CONFIGURE,
											dto.getExploratoryName(), dto.getComputationalName()))
									.withVolumeForRootKeys(configuration.getKeyDirectory())
									.withVolumeForResponse(configuration.getImagesDirectory())
									.withVolumeForLog(configuration.getDockerLogDirectory(), dataEngineType.getName())
									.withResource(dataEngineType.getName())
									.withRequestId(uuid)
									.withConfKeyName(configuration.getAdminKey())
									.withActionConfigure(getImageConfigure(dto.getApplicationName(), dataEngineType)),
							dto
					)
			);
		} catch (Exception t) {
			throw new DlabException("Could not configure computational resource cluster", t);
		}
		return uuid;
	}

	private FileHandlerCallback getFileHandlerCallback(DockerAction action, String originalUuid, ComputationalBase<?>
			dto) {
		return new ComputationalConfigureCallbackHandler(selfService, action, originalUuid, dto);
	}

	private String nameContainer(String user, DockerAction action, String exploratoryName, String name) {
		return nameContainer(user, action.toString(), "computational", exploratoryName, name);
	}

	private String getImageConfigure(String application, DataEngineType dataEngineType) {
		String imageName = DataEngineType.getDockerImageName(dataEngineType);
		int pos = imageName.indexOf('-');
		if (pos > 0) {
			return imageName.substring(0, pos + 1) + application;
		}
		throw new DlabException("Could not describe the image name for computational resources from image " +
				imageName + " and application " + application);
	}

	public String getResourceType() {
		return Directories.DATA_ENGINE_SERVICE_LOG_DIRECTORY;
	}
}
