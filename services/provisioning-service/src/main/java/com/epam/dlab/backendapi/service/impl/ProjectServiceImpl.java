package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.commands.*;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.core.response.handlers.ProjectCallbackHandler;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.dto.ProjectCreateDTO;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProjectServiceImpl implements ProjectService {
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
		String uuid = DockerCommands.generateUUID();

		folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
				configuration.getKeyLoaderPollTimeout(),
				new ProjectCallbackHandler(systemUserInfoService, selfService, userInfo.getName(), uuid,
						DockerAction.CREATE, "/api/project/status"));

		RunDockerCommand runDockerCommand = new RunDockerCommand()
				.withInteractive()
				.withName(String.join("_", dto.getName(), "project"))
				.withVolumeForRootKeys(configuration.getKeyDirectory())
				.withVolumeForResponse(configuration.getKeyLoaderDirectory())
				.withVolumeForLog(configuration.getDockerLogDirectory(), "project")
				.withResource("project")
				.withRequestId(uuid)
				.withConfKeyName(configuration.getAdminKey())
				.withImage("docker.dlab-project")
				.withAction(DockerAction.CREATE);

		try {
			commandExecutor.executeAsync(userInfo.getName(), uuid, commandBuilder.buildCommand(runDockerCommand, dto));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return uuid;
	}
}
