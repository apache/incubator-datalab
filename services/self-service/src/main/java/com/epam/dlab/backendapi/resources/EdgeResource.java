/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
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
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.rest.contracts.EdgeAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides the REST API to manage(start/stop) edge node
 */
@Path("/infrastructure/edge")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "EDGE service", authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
@Slf4j
public class EdgeResource implements EdgeAPI {

	private final EdgeService edgeService;

	@Inject
	public EdgeResource(EdgeService edgeService) {
		this.edgeService = edgeService;
	}

	/**
	 * Starts EDGE node for user.
	 *
	 * @param userInfo user info.
	 * @return Request Id.
	 */
	@POST
	@Path("/start")
	@ApiOperation("Starts EDGE")
	public String start(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		return edgeService.start(userInfo);
	}

	/**
	 * Stop EDGE node for user.
	 *
	 * @param userInfo user info.
	 * @return Request Id.
	 */
	@POST
	@Path("/stop")
	@ApiOperation("Stops EDGE")
	public String stop(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		return edgeService.stop(userInfo);
	}
}