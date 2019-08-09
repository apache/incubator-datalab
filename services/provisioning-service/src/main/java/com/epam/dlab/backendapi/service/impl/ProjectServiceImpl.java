package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.commands.*;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.core.response.handlers.ProjectCallbackHandler;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.dto.aws.edge.EdgeInfoAws;
import com.epam.dlab.dto.azure.edge.EdgeInfoAzure;
import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import com.epam.dlab.dto.project.ProjectActionDTO;
import com.epam.dlab.dto.project.ProjectCreateDTO;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProjectServiceImpl implements ProjectService {
	private static final String PROJECT_IMAGE = "docker.dlab-project";
	private static final String EDGE_IMAGE = "docker.dlab-edge";
	private static final String CALLBACK_URI = "/api/project/status";
	@Inject
	protected RESTService selfService;
	@Inject
	protected SystemUserInfoService systemUserInfoService;
	@Inject
	private ProvisioningServiceApplicationConfiguration configuration;
	@Inject
	private FolderListenerExecutor folderListenerExecutor;
	@Inject
	private ICommandExecutor commandExecutor;
	@Inject
	private CommandBuilder commandBuilder;

	@Override
	public String create(UserInfo userInfo, ProjectCreateDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.CREATE, dto.getName(), "project", PROJECT_IMAGE);
	}

	@Override
	public String terminate(UserInfo userInfo, ProjectActionDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.TERMINATE, dto.getName(), "project", PROJECT_IMAGE);
	}

	@Override
	public String start(UserInfo userInfo, ProjectActionDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.START, dto.getName(), "edge", EDGE_IMAGE);
	}

	@Override
	public String stop(UserInfo userInfo, ProjectActionDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.STOP, dto.getName(), "edge", EDGE_IMAGE);
	}

	private String executeDocker(UserInfo userInfo, ResourceBaseDTO dto, DockerAction action, String projectName,
								 String resourceType, String image) {
		String uuid = DockerCommands.generateUUID();

		folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
				configuration.getKeyLoaderPollTimeout(),
				new ProjectCallbackHandler(systemUserInfoService, selfService, userInfo.getName(), uuid,
						action, CALLBACK_URI, projectName, getEdgeClass()));

		RunDockerCommand runDockerCommand = new RunDockerCommand()
				.withInteractive()
				.withName(String.join("_", userInfo.getSimpleName(), projectName, resourceType, action.toString(),
						Long.toString(System.currentTimeMillis())))
				.withVolumeForRootKeys(configuration.getKeyDirectory())
				.withVolumeForResponse(configuration.getKeyLoaderDirectory())
				.withVolumeForLog(configuration.getDockerLogDirectory(), resourceType)
				.withResource(resourceType)
				.withRequestId(uuid)
				.withConfKeyName(configuration.getAdminKey())
				.withImage(image)
				.withAction(action);

		try {
			commandExecutor.executeAsync(userInfo.getName(), uuid, commandBuilder.buildCommand(runDockerCommand, dto));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return uuid;
	}

	private <T> Class<T> getEdgeClass() {
		if (configuration.getCloudProvider() == CloudProvider.AWS) {
			return (Class<T>) EdgeInfoAws.class;
		} else if (configuration.getCloudProvider() == CloudProvider.AZURE) {
			return (Class<T>) EdgeInfoAzure.class;
		} else if (configuration.getCloudProvider() == CloudProvider.GCP) {
			return (Class<T>) EdgeInfoGcp.class;
		}
		throw new IllegalArgumentException();
	}
}
