package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.handlers.ReuploadKeyCallbackHandler;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyDTO;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
@Singleton
public class ReuploadKeyService extends DockerService implements DockerCommands {

	public String reuploadKeyAction(String userName, ReuploadKeyDTO dto, DockerAction action)
			throws JsonProcessingException {
		log.debug("{} for edge user {}", action, dto.getEdgeUserName());
		String uuid = DockerCommands.generateUUID();
		folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
				configuration.getKeyLoaderPollTimeout(),
				new ReuploadKeyCallbackHandler(selfService, ApiCallbacks.REUPLOAD_KEY_URI, userName, dto));

		RunDockerCommand runDockerCommand = new RunDockerCommand()
				.withInteractive()
				.withName("reupload_key")
				.withVolumeForRootKeys(configuration.getKeyDirectory())
				.withVolumeForResponse(configuration.getKeyLoaderDirectory())
				.withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
				.withResource(getResourceType())
				.withRequestId(uuid)
				.withConfKeyName(configuration.getAdminKey())
				.withImage(configuration.getEdgeImage())
				.withAction(action);

		String command;
		for (String resourceName : dto.getRunningResources()) {
			ReuploadKeyDTO newDto = new ReuploadKeyDTO();
			newDto.withEdgeUserName(dto.getEdgeUserName());
			newDto.withServiceBaseName(dto.getServiceBaseName());
			newDto.withConfOsFamily(dto.getConfOsFamily());
			newDto.withRunningResources(Collections.singletonList(resourceName));
			command = commandBuilder.buildCommand(runDockerCommand, newDto).replaceAll("([\\[\\]])*", "");
			log.trace("Docker command:  {}", command);
			commandExecutor.executeAsync(userName, uuid, command);
		}
		return uuid;
	}

	@Override
	public String getResourceType() {
		return Directories.EDGE_LOG_DIRECTORY;
	}

}
