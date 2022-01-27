/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.domain.BudgetDTO;
import com.epam.datalab.backendapi.domain.CreateProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectEndpointDTO;
import com.epam.datalab.backendapi.domain.UpdateProjectBudgetDTO;
import com.epam.datalab.backendapi.domain.UpdateProjectDTO;
import com.epam.datalab.backendapi.resources.dto.ProjectActionFormDTO;
import com.epam.datalab.backendapi.service.AccessKeyService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.rest.dto.ErrorDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
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
    @ApiResponse(responseCode = "201", description = "Project is successfully created",
            headers =
            @Header(required = true, name = "Location", description = "URI of created project resource",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
            MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ErrorDTO.class)))
    @ApiResponse(responseCode = "409", description = "Project with passed name already exist in system",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorDTO.class)))
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("/api/project/create")
    public Response createProject(@Parameter(hidden = true) @Auth UserInfo userInfo,
                                  @Valid CreateProjectDTO projectDTO) {
        log.info("Trying to create project: {}", projectDTO);
        List<ProjectEndpointDTO> projectEndpointDTOS = projectDTO.getEndpoints()
                .stream()
                .map(e -> new ProjectEndpointDTO(e, UserInstanceStatus.CREATING, null))
                .collect(Collectors.toList());
        ProjectDTO project = new ProjectDTO(projectDTO.getName(), projectDTO.getGroups(), projectDTO.getKey(), projectDTO.getTag(),
                new BudgetDTO(), projectEndpointDTOS, projectDTO.isSharedImageEnabled());
        projectService.create(userInfo, project, projectDTO.getName());
        final URI uri = uriInfo.getRequestUriBuilder().path(projectDTO.getName()).build();
        return Response
                .ok()
                .location(uri)
                .build();
    }

    @Operation(summary = "Recreate project edge", tags = "project")
    @ApiResponse(responseCode = "202", description = "Project edge is recreating")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
            MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ErrorDTO.class)))
    @Path("recreate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("/api/project")
    public Response recreateProject(@Parameter(hidden = true) @Auth UserInfo userInfo,
                                    @NotNull @Valid ProjectActionFormDTO startProjectDto) {
        log.info("Trying to recreate project: {}", startProjectDto);

        startProjectDto.getEndpoints()
                .forEach(endpoint -> projectService.recreate(userInfo, endpoint, startProjectDto.getProjectName()));
        return Response
                .accepted()
                .build();
    }

    @Operation(summary = "Start project", tags = "project")
    @ApiResponse(responseCode = "202", description = "Project is starting")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
            MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ErrorDTO.class)))
    @Path("start")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("/api/project")
    public Response startProject(@Parameter(hidden = true) @Auth UserInfo userInfo,
                                 @NotNull @Valid ProjectActionFormDTO startProjectDto) {
        log.info("Trying to start project: {}", startProjectDto);
        projectService.start(userInfo, startProjectDto.getEndpoints(), startProjectDto.getProjectName());
        return Response
                .accepted()
                .build();
    }

    @Operation(summary = "Stop project", tags = "project")
    @ApiResponse(responseCode = "202", description = "Project is stopping")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
            MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ErrorDTO.class)))
    @Path("stop")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("/api/project")
    public Response stopProject(@Parameter(hidden = true) @Auth UserInfo userInfo,
                                @NotNull @Valid ProjectActionFormDTO stopProjectDTO) {
        log.info("Trying to stop project: {}", stopProjectDTO);
        projectService.stopWithResources(userInfo, stopProjectDTO.getEndpoints(), stopProjectDTO.getProjectName());
        return Response
                .accepted()
                .build();
    }


    @Operation(summary = "Get project info", tags = "project")
    @ApiResponse(responseCode = "200", description = "Return information about project",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema =
            @Schema(implementation = ProjectDTO.class)))
    @ApiResponse(responseCode = "404", description = "Project with passed name not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorDTO.class)))
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
    @ApiResponse(responseCode = "200", description = "Return information about projects",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema =
            @Schema(implementation = ProjectDTO.class)))
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("/api/project")
    public Response getProjects(@Parameter(hidden = true) @Auth UserInfo userInfo) {
        return Response
                .ok(projectService.getProjects(userInfo))
                .build();
    }

    @Operation(summary = "Get projects assigned to user", tags = "project")
    @ApiResponse(responseCode = "200", description = "Return information about projects",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema =
            @Schema(implementation = ProjectDTO.class)))
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
    @ApiResponse(responseCode = "200", description = "Project is successfully updated")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
            MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ErrorDTO.class)))
    @ApiResponse(responseCode = "404", description = "Project with passed name not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorDTO.class)))
    @PUT
    @RolesAllowed("/api/project")
    public Response updateProject(@Parameter(hidden = true) @Auth UserInfo userInfo, UpdateProjectDTO projectDTO) {
        projectService.update(userInfo, projectDTO, projectDTO.getName());
        return Response.ok().build();
    }

    @Operation(summary = "Remove project", tags = "project")
    @ApiResponse(responseCode = "200", description = "Project is successfully removed")
    @ApiResponse(responseCode = "404", description = "Project with passed name not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorDTO.class)))
    @POST
    @Path("terminate")
    @RolesAllowed("/api/project")
    public Response removeProjectEndpoint(@Parameter(hidden = true) @Auth UserInfo userInfo,
                                          @NotNull @Valid ProjectActionFormDTO projectActionDTO) {
        projectService.terminateEndpoint(userInfo, projectActionDTO.getEndpoints(), projectActionDTO.getProjectName());
        return Response.ok().build();
    }

    @Operation(summary = "Updates project budget", tags = "project")
    @ApiResponse(responseCode = "200", description = "Project budget is successfully updated")
    @ApiResponse(responseCode = "404", description = "Project with specified name not found")
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorDTO.class)))
    @PUT
    @Path("/budget")
    @RolesAllowed("/api/project")
    public Response updateBudget(
            @Parameter(hidden = true) @Auth UserInfo userInfo,
            @Parameter(description = "Update project budgets list") List<UpdateProjectBudgetDTO> dtos) {
        projectService.updateBudget(userInfo, dtos);
        return Response.ok().build();
    }

    @Operation(summary = "Generate keys for project", tags = "project")
    @ApiResponse(responseCode = "200", description = "Keys are successfully generated")
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
