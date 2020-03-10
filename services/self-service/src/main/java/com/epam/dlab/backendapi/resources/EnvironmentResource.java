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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("environment")
@Slf4j
@RolesAllowed("environment/*")
public class EnvironmentResource {

	private EnvironmentService environmentService;

	@Inject
	public EnvironmentResource(EnvironmentService environmentService) {
		this.environmentService = environmentService;
	}

	@GET
	@Path("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsersWithActiveEnv(@Auth UserInfo userInfo) {
		log.debug("User {} requested information about active environments", userInfo.getName());
		return Response.ok(environmentService.getUsers()).build();
	}

	@GET
	@Path("all")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllEnv(@Auth UserInfo userInfo) {
		log.debug("Admin {} requested information about all user's environment", userInfo.getName());
		return Response.ok(environmentService.getAllEnv()).build();
	}
}
