/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.epam.dlab.backendapi.service.UserRolesService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
@Path("roles")
@RolesAllowed("roles")
public class UserRolesResource {

	@Inject
	private UserRolesService userRolesService;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRoles(@Auth UserInfo userInfo) {
		log.debug("Getting all roles for admin {}...", userInfo.getName());
		return Response.ok(userRolesService.getUserRoles()).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response upsertRole(@Auth UserInfo userInfo, UserRoleDto dto) {
		log.debug("Upserting role {} on behalf of admin {}...", dto, userInfo.getName());
		userRolesService.createOrUpdateRole(dto);
		return Response.ok().build();
	}

	@DELETE
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	public Response removeRole(@Auth UserInfo userInfo, String roleId) {
		userRolesService.removeRoleIfExists(roleId, userInfo.getName());
		return Response.ok().build();
	}
}
