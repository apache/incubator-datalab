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
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.dto.project.ProjectActionDTO;
import com.epam.datalab.dto.project.ProjectCreateDTO;
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

	@Path("/recreate")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response recreateProject(@Auth UserInfo userInfo, ProjectCreateDTO dto) {
		return Response.ok(projectService.recreate(userInfo, dto)).build();
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
