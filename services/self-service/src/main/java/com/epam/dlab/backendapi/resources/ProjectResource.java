package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.CreateProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.domain.UpdateProjectBudgetDTO;
import com.epam.dlab.backendapi.domain.UpdateProjectDTO;
import com.epam.dlab.backendapi.resources.dto.ProjectActionFormDTO;
import com.epam.dlab.backendapi.service.AccessKeyService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.rest.dto.ErrorDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("project")
public class ProjectResource {
	private final ProjectService projectService;
	private final AccessKeyService keyService;
	@Context
	private UriInfo uriInfo;

	@Inject
	public ProjectResource(ProjectService projectService, AccessKeyService keyService) {
		this.projectService = projectService;
		this.keyService = keyService;
	}


	@Operation(summary = "Create project", tags = "project")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Project is successfully created",
					headers =
					@Header(required = true, name = "Location", description = "URI of created project resource",
							schema = @Schema(type = "string"))),
			@ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
					MediaType.APPLICATION_JSON,
					schema = @Schema(implementation = ErrorDTO.class))),
			@ApiResponse(responseCode = "409", description = "Project with passed name already exist in system",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ErrorDTO.class)))
	})
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed("/api/project/create")
	public Response createProject(@Parameter(hidden = true) @Auth UserInfo userInfo,
								  @Valid CreateProjectDTO projectDTO) {
		ProjectDTO project = new ProjectDTO(projectDTO.getName(), projectDTO.getGroups(),
				projectDTO.getKey(), projectDTO.getTag(), null,
				projectDTO.getEndpoints().stream().map(e -> new ProjectEndpointDTO(e, UserInstanceStatus.CREATING,
						null)).collect(Collectors.toList()), projectDTO.isSharedImageEnabled());
		projectService.create(userInfo, project, projectDTO.getName());
		final URI uri = uriInfo.getRequestUriBuilder().path(projectDTO.getName()).build();
		return Response
				.ok()
				.location(uri)
				.build();
	}

	@Operation(summary = "Start project", tags = "project")
	@ApiResponses({
			@ApiResponse(responseCode = "202", description = "Project is starting"),
			@ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
					MediaType.APPLICATION_JSON,
					schema = @Schema(implementation = ErrorDTO.class)))
	})
	@Path("start")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed("/api/project")
	public Response startProject(@Parameter(hidden = true) @Auth UserInfo userInfo,
								 @NotNull @Valid ProjectActionFormDTO startProjectDto) {
		projectService.start(userInfo, startProjectDto.getEndpoints(), startProjectDto.getProjectName());
		return Response
				.accepted()
				.build();
	}

	@Operation(summary = "Stop project", tags = "project")
	@ApiResponses({
			@ApiResponse(responseCode = "202", description = "Project is stopping"),
			@ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
					MediaType.APPLICATION_JSON,
					schema = @Schema(implementation = ErrorDTO.class)))
	})
	@Path("stop")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed("/api/project")
	public Response stopProject(@Parameter(hidden = true) @Auth UserInfo userInfo,
								@NotNull @Valid ProjectActionFormDTO stopProjectDTO) {
		projectService.stopWithResources(userInfo, stopProjectDTO.getEndpoints(), stopProjectDTO.getProjectName());
		return Response
				.accepted()
				.build();
	}

	@Operation(summary = "Get project info", tags = "project")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Return information about project",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema =
					@Schema(implementation = ProjectDTO.class))),
			@ApiResponse(responseCode = "404", description = "Project with passed name not found",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ErrorDTO.class)))
	})
	@GET
	@Path("{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("/api/project")
	public Response getProject(@Parameter(hidden = true) @Auth UserInfo userInfo,
							   @Parameter(description = "Project name")
							   @PathParam("name") String name) {
		return Response
				.ok(projectService.get(name))
				.build();
	}

	@Operation(summary = "Get available projects", tags = "project")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Return information about projects",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema =
					@Schema(implementation = ProjectDTO.class))),
	})
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("/api/project")
	public Response getProjects(@Parameter(hidden = true) @Auth UserInfo userInfo) {
		return Response
				.ok(projectService.getProjects(userInfo))
				.build();
	}

	@Operation(summary = "Get projects assigned to user", tags = "project")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Return information about projects",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema =
					@Schema(implementation = ProjectDTO.class))),
	})
	@GET
	@Path("/me")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUserProjects(@Parameter(hidden = true) @Auth UserInfo userInfo,
									@QueryParam("active") @DefaultValue("false") boolean active) {
		return Response
				.ok(projectService.getUserProjects(userInfo, active))
				.build();
	}

	@Operation(summary = "Update project", tags = "project")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Project is successfully updated"),
			@ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
					MediaType.APPLICATION_JSON,
					schema = @Schema(implementation = ErrorDTO.class))),
			@ApiResponse(responseCode = "404", description = "Project with passed name not found",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ErrorDTO.class)))
	})
	@PUT
	@RolesAllowed("/api/project")
	public Response updateProject(@Parameter(hidden = true) @Auth UserInfo userInfo, UpdateProjectDTO projectDTO) {
		projectService.update(userInfo, projectDTO, projectDTO.getName());
		return Response.ok().build();
	}

	@Operation(summary = "Remove project", tags = "project")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Project is successfully removed"),
			@ApiResponse(responseCode = "404", description = "Project with passed name not found",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ErrorDTO.class)))
	})
	@POST
	@Path("terminate")
	@RolesAllowed("/api/project")
	public Response removeProjectEndpoint(@Parameter(hidden = true) @Auth UserInfo userInfo,
										  @NotNull @Valid ProjectActionFormDTO projectActionDTO) {
		projectService.terminateEndpoint(userInfo, projectActionDTO.getEndpoints(), projectActionDTO.getProjectName());
		return Response.ok().build();
	}

	@Operation(summary = "Updates project budget", tags = "project")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Project budget is successfully updated"),
			@ApiResponse(responseCode = "404", description = "Project with specified name not found"),
			@ApiResponse(responseCode = "400", description = "Validation error",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ErrorDTO.class)))
	})
	@PUT
	@Path("/budget")
	@RolesAllowed("/api/project")
	public Response updateBudget(
			@Parameter(hidden = true) @Auth UserInfo userInfo,
			@Parameter(description = "Project name")
					List<UpdateProjectBudgetDTO> dtos) {
		projectService.updateBudget(userInfo, dtos);
		return Response.ok().build();
	}

	@Operation(summary = "Generate keys for project", tags = "project")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Keys are successfully generated")
	})
	@POST
	@Path("/keys")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed("/api/project")
	public Response generate(@Parameter(hidden = true) @Auth UserInfo userInfo) {
		return Response
				.ok(keyService.generateKeys(userInfo))
				.build();
	}
}
