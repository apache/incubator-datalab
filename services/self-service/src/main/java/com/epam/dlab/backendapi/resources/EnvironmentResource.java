/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("environment")
@Slf4j
@RolesAllowed("environment/*")
@Api(value = "Environment service", authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
public class EnvironmentResource {

	private EnvironmentService environmentService;

	@Inject
	public EnvironmentResource(EnvironmentService environmentService) {
		this.environmentService = environmentService;
	}

	@GET
	@Path("user/active")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Fetches active users")
	@ApiResponses({@ApiResponse(code = 404, message = "Active users not found"),
			@ApiResponse(code = 200, message = "Active users were fetched successfully")})
	public Response getUsersWithActiveEnv(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		log.debug("User {} requested information about active environments", userInfo.getName());
		return Response.ok(environmentService.getActiveUsers()).build();
	}

	@GET
	@Path("all")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Fetches user resources")
	public Response getAllEnv(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		log.debug("Admin {} requested information about all user's environment", userInfo.getName());
		return Response.ok(environmentService.getAllEnv()).build();
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("terminate")
	@ApiOperation("Terminates user's environment including EDGE, notebooks, clusters")
	@ApiResponses(@ApiResponse(code = 200, message = "User's environment terminated successfully"))
	public Response terminateEnv(@ApiParam(hidden = true) @Auth UserInfo userInfo,
								 @ApiParam(value = "User's name", required = true) @NotEmpty String user) {
		log.info("User {} is terminating {} environment", userInfo.getName(), user);
		environmentService.terminateEnvironment(user);
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("stop")
	@ApiOperation("Stops user's environment including EDGE, notebooks, Spark clusters")
	@ApiResponses(@ApiResponse(code = 200, message = "User's environment stopped successfully"))
	public Response stopEnv(@ApiParam(hidden = true) @Auth UserInfo userInfo,
							@ApiParam(value = "User's name", required = true) @NotEmpty String user) {
		log.info("User {} is stopping {} environment", userInfo.getName(), user);
		environmentService.stopEnvironment(user);
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("stop/edge")
	@ApiOperation("Stops user's EDGE node")
	public Response stopEdge(@ApiParam(hidden = true) @Auth UserInfo userInfo, @NotEmpty String user) {
		log.info("Admin {} is stopping edge of user {}", userInfo.getName(), user);
		environmentService.stopEdge(user);
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("stop/{exploratoryName}")
	public Response stopNotebook(@Auth UserInfo userInfo, @NotEmpty String user,
								 @PathParam("exploratoryName") String exploratoryName) {
		log.info("Admin {} is stopping notebook {} of user {}", userInfo.getName(), exploratoryName, user);
		environmentService.stopExploratory(user, exploratoryName);
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("stop/{exploratoryName}/{computationalName}")
	public Response stopCluster(@Auth UserInfo userInfo, @NotEmpty String user,
								@PathParam("exploratoryName") String exploratoryName,
								@PathParam("computationalName") String computationalName) {
		log.info("Admin {} is stopping computational resource {} affiliated with exploratory {} of user {}",
				userInfo.getName(), computationalName, exploratoryName, user);
		environmentService.stopComputational(user, exploratoryName, computationalName);
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("terminate/{exploratoryName}")
	public Response terminateNotebook(@Auth UserInfo userInfo, @NotEmpty String user,
									  @PathParam("exploratoryName") String exploratoryName) {
		log.info("Admin {} is terminating notebook {} of user {}", userInfo.getName(), exploratoryName, user);
		environmentService.terminateExploratory(user, exploratoryName);
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("terminate/{exploratoryName}/{computationalName}")
	public Response terminateCluster(@Auth UserInfo userInfo, @NotEmpty String user,
									 @PathParam("exploratoryName") String exploratoryName,
									 @PathParam("computationalName") String computationalName) {
		log.info("Admin {} is terminating computational resource {} affiliated with exploratory {} of user {}",
				userInfo.getName(), computationalName, exploratoryName, user);
		environmentService.terminateComputational(user, exploratoryName, computationalName);
		return Response.ok().build();
	}
}
