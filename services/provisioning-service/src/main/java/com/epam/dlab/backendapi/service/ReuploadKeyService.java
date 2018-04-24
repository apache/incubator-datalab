package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.dto.ReuploadFileDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReuploadKeyService extends DockerService implements DockerCommands {

	public String reuploadKeyAction(String userName, ReuploadFileDTO dto, DockerAction action)
			throws JsonProcessingException {
		log.debug("{} for edge user {}", action, dto.getEdgeUserName());
		String uuid = DockerCommands.generateUUID();

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

		String command = commandBuilder.buildCommand(runDockerCommand, dto);
		log.trace("Docker command:  {}", command);
		commandExecutor.executeAsync(userName, uuid, command);
		return uuid;
	}

	@Override
	public String getResourceType() {
		return "edge";
	}

}
