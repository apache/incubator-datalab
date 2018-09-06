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
import com.epam.dlab.backendapi.resources.dto.UpdateRoleGroupDto;
import com.epam.dlab.backendapi.resources.dto.UpdateRoleUserDto;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.epam.dlab.backendapi.service.UserRolesService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
@Path("role")
@RolesAllowed("/roleManagement")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserRolesResource {

	@Inject
	private UserRolesService userRolesService;

	@GET
	public Response getRoles(@Auth UserInfo userInfo) {
		log.debug("Getting all roles for admin {}...", userInfo.getName());
		return Response.ok(userRolesService.getUserRoles()).build();
	}

	@POST
	public Response createRole(@Auth UserInfo userInfo, UserRoleDto dto) {
		log.info("Creating new role {} on behalf of admin {}...", dto, userInfo.getName());
		userRolesService.createRole(dto);
		return Response.ok().build();
	}

	@PUT
	@Path("group")
	public Response addGroupToRole(@Auth UserInfo userInfo, @Valid UpdateRoleGroupDto updateRoleGroupDto) {
		log.info("Admin {} is trying to add new groups {} to roles {}", userInfo.getName(),
				updateRoleGroupDto.getGroups(), updateRoleGroupDto.getRoleIds());
		userRolesService.addGroupToRole(updateRoleGroupDto.getGroups(), updateRoleGroupDto.getRoleIds());
		return Response.ok().build();
	}

	@DELETE
	@Path("group")
	public Response deleteGroupFromRole(@Auth UserInfo userInfo, @Valid UpdateRoleGroupDto updateRoleGroupDto) {
		log.info("Admin {} is trying to delete groups {} from roles {}", userInfo.getName(),
				updateRoleGroupDto.getGroups(), updateRoleGroupDto.getRoleIds());
		userRolesService.removeGroupFromRole(updateRoleGroupDto.getGroups(), updateRoleGroupDto.getRoleIds());
		return Response.ok().build();
	}

	@PUT
	@Path("user")
	public Response addUserToRole(@Auth UserInfo userInfo, @Valid UpdateRoleUserDto updateRoleUserDto) {
		log.info("Admin {} is trying to add new users {} to roles {}", userInfo.getName(),
				updateRoleUserDto.getUsers(), updateRoleUserDto.getRoleIds());
		userRolesService.addUserToRole(updateRoleUserDto.getUsers(), updateRoleUserDto.getRoleIds());
		return Response.ok().build();
	}

	@DELETE
	@Path("user")
	public Response deleteUserFromRole(@Auth UserInfo userInfo, @Valid UpdateRoleUserDto updateRoleUserDto) {
		log.info("Admin {} is trying to delete users {} from roles {}", userInfo.getName(),
				updateRoleUserDto.getUsers(), updateRoleUserDto.getRoleIds());
		userRolesService.removeUserFromRole(updateRoleUserDto.getUsers(), updateRoleUserDto.getRoleIds());
		return Response.ok().build();
	}
}
