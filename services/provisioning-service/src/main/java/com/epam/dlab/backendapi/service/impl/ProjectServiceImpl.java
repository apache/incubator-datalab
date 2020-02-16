package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.commands.CommandBuilder;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.ICommandExecutor;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
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

import java.util.Objects;

@Slf4j
public class ProjectServiceImpl implements ProjectService {
	private static final String PROJECT_IMAGE = "docker.dlab-project";
	private static final String EDGE_IMAGE = "docker.dlab-edge";
	private static final String CALLBACK_URI = "/api/project/status";
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
	public String create(UserInfo userInfo, ProjectCreateDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.CREATE, dto.getName(), "project", PROJECT_IMAGE,
				dto.getEndpoint());
	}

	@Override
	public String terminate(UserInfo userInfo, ProjectActionDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.TERMINATE, dto.getName(), "project", PROJECT_IMAGE,
				dto.getEndpoint());
	}

	@Override
	public String start(UserInfo userInfo, ProjectActionDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.START, dto.getName(), "edge", EDGE_IMAGE, dto.getEndpoint());
	}

	@Override
	public String stop(UserInfo userInfo, ProjectActionDTO dto) {
		return executeDocker(userInfo, dto, DockerAction.STOP, dto.getName(), "edge", EDGE_IMAGE, dto.getEndpoint());
	}

	private String executeDocker(UserInfo userInfo, ResourceBaseDTO dto, DockerAction action, String projectName,
								 String resourceType, String image, String endpoint) {
		String uuid = DockerCommands.generateUUID();

		folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
				configuration.getKeyLoaderPollTimeout(),
				new ProjectCallbackHandler(selfService, userInfo.getName(), uuid,
						action, CALLBACK_URI, projectName, getEdgeClass(), endpoint));

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
		if (configuration.getCloudProvider() == CloudProvider.AZURE &&
				Objects.nonNull(configuration.getCloudConfiguration().getAzureAuthFile()) &&
				!configuration.getCloudConfiguration().getAzureAuthFile().isEmpty()) {
			runDockerCommand.withVolumeFoAzureAuthFile(configuration.getCloudConfiguration().getAzureAuthFile());
		}

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
