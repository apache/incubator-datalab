package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.dto.project.ProjectActionDTO;
import com.epam.dlab.dto.project.ProjectCreateDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("infrastructure/project")
public class ProjectResource {
	private final ProjectService projectService;

	@Inject
	public ProjectResource(ProjectService projectService) {
		this.projectService = projectService;
	}

	@Path("/create")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createProject(@Auth UserInfo userInfo, ProjectCreateDTO dto) {
		return Response.ok(projectService.create(userInfo, dto)).build();
	}

	@Path("/terminate")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response terminateProject(@Auth UserInfo userInfo, ProjectActionDTO dto) {
		return Response.ok(projectService.terminate(userInfo, dto)).build();
	}

	@Path("/start")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response startProject(@Auth UserInfo userInfo, ProjectActionDTO dto) {
		return Response.ok(projectService.start(userInfo, dto)).build();
	}

	@Path("/stop")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response stopProject(@Auth UserInfo userInfo, ProjectActionDTO dto) {
		return Response.ok(projectService.stop(userInfo, dto)).build();
	}
}
