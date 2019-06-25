package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.project.CreateProjectResult;
import com.epam.dlab.dto.base.project.ProjectEdgeInfo;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class ProjectCallbackHandler extends ResourceCallbackHandler<CreateProjectResult> {


	private final String callbackUri;

	public ProjectCallbackHandler(SystemUserInfoService systemUserInfoService, RESTService selfService, String user,
								  String uuid, DockerAction action, String callbackUri) {
		super(systemUserInfoService, selfService, user, uuid, action);
		this.callbackUri = callbackUri;
	}

	@Override
	protected String getCallbackURI() {
		return callbackUri;
	}

	@Override
	protected CreateProjectResult parseOutResponse(JsonNode resultNode, CreateProjectResult baseStatus) {
		if (resultNode != null && getAction() == DockerAction.CREATE
				&& UserInstanceStatus.of(baseStatus.getStatus()) != UserInstanceStatus.FAILED) {
			try {
				final ProjectEdgeInfo projectEdgeInfo = mapper.readValue(resultNode.toString(), ProjectEdgeInfo.class);
				baseStatus.setEdgeInfo(projectEdgeInfo);
				baseStatus.setProjectName(resultNode.get("project_name").asText());
			} catch (IOException e) {
				throw new DlabException("Cannot parse the EDGE info in JSON: " + e.getLocalizedMessage(), e);
			}
		}

		return baseStatus;
	}
}
