package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.project.CreateProjectResult;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/project/status")
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ProjectCallback {

	private final ProjectDAO projectDAO;
	private final RequestId requestId;

	@Inject
	public ProjectCallback(ProjectDAO projectDAO, RequestId requestId) {
		this.projectDAO = projectDAO;
		this.requestId = requestId;
	}


	@POST
	public Response updateProjectStatus(@Auth UserInfo userInfo, CreateProjectResult projectResult) {
		requestId.checkAndRemove(projectResult.getRequestId());
		if (UserInstanceStatus.of(projectResult.getStatus()) == UserInstanceStatus.FAILED) {
			projectDAO.updateStatus(projectResult.getProjectName(), ProjectDTO.Status.FAILED);
		} else {
			projectDAO.updateEdgeInfoAndStatus(projectResult.getProjectName(), projectResult.getEdgeInfo(),
					ProjectDTO.Status.CREATED);
		}
		return Response.ok().build();
	}
}
