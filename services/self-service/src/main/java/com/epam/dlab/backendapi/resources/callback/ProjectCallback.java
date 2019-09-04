package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.project.ProjectResult;
import com.google.inject.Inject;
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
	private final ExploratoryService exploratoryService;
	private final RequestId requestId;

	@Inject
	public ProjectCallback(ProjectDAO projectDAO, ExploratoryService exploratoryService, RequestId requestId) {
		this.projectDAO = projectDAO;
		this.exploratoryService = exploratoryService;
		this.requestId = requestId;
	}


	@POST
	public Response updateProjectStatus(ProjectResult projectResult) {
		requestId.checkAndRemove(projectResult.getRequestId());
		final String projectName = projectResult.getProjectName();
		final UserInstanceStatus status = UserInstanceStatus.of(projectResult.getStatus());
		if (UserInstanceStatus.RUNNING == status && Objects.nonNull(projectResult.getEdgeInfo())) {
			projectDAO.updateEdgeInfoAndStatus(projectName, projectResult.getEdgeInfo(), ProjectDTO.Status.ACTIVE);
		} else {
			updateExploratoriesStatusIfNeeded(status, projectResult.getProjectName());
			projectDAO.updateStatus(projectName, ProjectDTO.Status.from(status));
		}
		return Response.ok().build();
	}

	private void updateExploratoriesStatusIfNeeded(UserInstanceStatus status, String projectName) {
		if (UserInstanceStatus.TERMINATED == status) {
			exploratoryService.updateProjectExploratoryStatuses(projectName, UserInstanceStatus.TERMINATED);
		}
	}
}
