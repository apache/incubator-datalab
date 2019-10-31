package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.dto.base.project.ProjectResult;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class ProjectCallbackHandler extends ResourceCallbackHandler<ProjectResult> {


	private final String callbackUri;
	private final String projectName;
	private final Class<? extends EdgeInfo> clazz;
	private final String endpointName;

	public ProjectCallbackHandler(RESTService selfService, String user,
								  String uuid, DockerAction action, String callbackUri, String projectName,
								  Class<? extends EdgeInfo> clazz, String endpointName) {
		super(selfService, user, uuid, action);
		this.callbackUri = callbackUri;
		this.projectName = projectName;
		this.clazz = clazz;
		this.endpointName = endpointName;
	}

	@Override
	protected String getCallbackURI() {
		return callbackUri;
	}

	@Override
	protected ProjectResult parseOutResponse(JsonNode resultNode, ProjectResult baseStatus) {
		baseStatus.setProjectName(projectName);
		baseStatus.setEndpointName(endpointName);
		if (resultNode != null && getAction() == DockerAction.CREATE
				&& UserInstanceStatus.of(baseStatus.getStatus()) != UserInstanceStatus.FAILED) {
			try {
				final EdgeInfo projectEdgeInfo = mapper.readValue(resultNode.toString(), clazz);
				baseStatus.setEdgeInfo(projectEdgeInfo);
			} catch (IOException e) {
				throw new DlabException("Cannot parse the EDGE info in JSON: " + e.getLocalizedMessage(), e);
			}
		}

		return baseStatus;
	}
}
