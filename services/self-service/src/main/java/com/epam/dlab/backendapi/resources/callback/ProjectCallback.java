package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.project.ProjectResult;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Objects;

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
	public Response updateProjectStatus(@Auth UserInfo userInfo, ProjectResult projectResult) {
		requestId.checkAndRemove(projectResult.getRequestId());
		final String projectName = projectResult.getProjectName();
		final UserInstanceStatus status = UserInstanceStatus.of(projectResult.getStatus());
		if (UserInstanceStatus.RUNNING == status && Objects.nonNull(projectResult.getEdgeInfo())) {
			projectDAO.updateEdgeInfoAndStatus(projectName, projectResult.getEdgeInfo(), ProjectDTO.Status.ACTIVE);
		} else {
			projectDAO.updateStatus(projectName, ProjectDTO.Status.from(status));
		}
		return Response.ok().build();
	}
}
