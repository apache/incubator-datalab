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
import com.epam.dlab.backendapi.core.response.handlers.ResourcesStatusCallbackHandler;
import com.epam.dlab.dto.UserEnvironmentResources;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.process.model.ProcessInfo;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.epam.dlab.backendapi.core.commands.DockerAction.STATUS;
import static java.util.stream.Collectors.toList;

@Slf4j
public abstract class InfrastructureService implements DockerCommands {
	@Inject
	private RESTService selfService;
	@Inject
	private ProvisioningServiceApplicationConfiguration configuration;
	@Inject
	private FolderListenerExecutor folderListenerExecutor;
	@Inject
	private ICommandExecutor commandExecutor;
	@Inject
	private CommandBuilder commandBuilder;

	private static final String CONTAINER_NAME_REGEX_FORMAT = "%s_[^_\\W]+_%s(|_%s)_\\d+";

	public String action(String username, UserEnvironmentResources dto, String iamUser, DockerAction dockerAction) {
		log.trace("Request the status of resources for user {}: {}", username, dto);
		String uuid = DockerCommands.generateUUID();
		folderListenerExecutor.start(configuration.getImagesDirectory(),
				configuration.getRequestEnvStatusTimeout(),
				getFileHandlerCallback(dockerAction, uuid, iamUser));
		try {

			removeResourcesWithRunningContainers(username, dto);

			if (!(dto.getResourceList().getHostList().isEmpty() && dto.getResourceList().getClusterList().isEmpty())) {
				log.trace("Request the status of resources for user {} after filtering: {}", username, dto);
				commandExecutor.executeAsync(
						username,
						uuid,
						commandBuilder.buildCommand(
								new RunDockerCommand()
										.withInteractive()
										.withName(nameContainer(dto.getEdgeUserName(), STATUS, "resources"))
										.withVolumeForRootKeys(configuration.getKeyDirectory())
										.withVolumeForResponse(configuration.getImagesDirectory())
										.withVolumeForLog(configuration.getDockerLogDirectory(), Directories
												.EDGE_LOG_DIRECTORY)
										.withResource(getResourceType())
										.withRequestId(uuid)
										.withConfKeyName(configuration.getAdminKey())
										.withActionStatus(configuration.getEdgeImage()),
								dto
						)
				);
			} else {
				log.debug("Skipping calling status command. Resource lists are empty");
			}
		} catch (Exception e) {
			throw new DlabException("Docker's command \"" + getResourceType() + "\" is fail: " + e.getLocalizedMessage
					(), e);
		}
		return uuid;
	}

	private void removeResourcesWithRunningContainers(String username, UserEnvironmentResources dto)
			throws Exception {

		final ProcessInfo processInfo = commandExecutor.executeSync(username, DockerCommands.generateUUID(),
				String.format(DockerCommands
						.GET_RUNNING_CONTAINERS_FOR_USER, dto.getEdgeUserName()));
		final String processInfoStdOut = processInfo.getStdOut();

		if (StringUtils.isNoneEmpty(processInfoStdOut)) {
			final List<String> runningContainerNames = Arrays.asList(processInfoStdOut.split("\n"));
			log.info("Running containers for users: {}", runningContainerNames);
			final List<EnvResource> hostList = filter(dto.getEdgeUserName(), runningContainerNames, dto
					.getResourceList()
					.getHostList());
			final List<EnvResource> clusterList = filter(dto.getEdgeUserName(), runningContainerNames, dto
					.getResourceList()
					.getClusterList());

			dto.getResourceList().setHostList(hostList);
			dto.getResourceList().setClusterList(clusterList);

		}
	}

	private List<EnvResource> filter(String username, List<String> runningContainerNames, List<EnvResource> hostList) {
		return hostList
				.stream()
				.filter(envResource -> hasNotCorrespondingRunningContainer(username, runningContainerNames,
						envResource))
				.map(envResource -> new EnvResource().withId(envResource.getId()).withStatus(envResource.getStatus()))
				.collect(toList());
	}

	private boolean hasNotCorrespondingRunningContainer(String username, List<String> runningContainerNames,
														EnvResource
																envResource) {
		final String regex = String.format(CONTAINER_NAME_REGEX_FORMAT, username, envResource
				.getResourceType().name().toLowerCase(), Optional.ofNullable(envResource.getName()).orElse(""));
		return runningContainerNames.stream().noneMatch(container -> container.matches(regex));
	}

	protected FileHandlerCallback getFileHandlerCallback(DockerAction action, String uuid, String user) {
		return new ResourcesStatusCallbackHandler(selfService, action, uuid, user);
	}

	private String nameContainer(String user, DockerAction action, String name) {
		return nameContainer(user, action.toString(), name);
	}

	public String getResourceType() {
		return "status";
	}
}
