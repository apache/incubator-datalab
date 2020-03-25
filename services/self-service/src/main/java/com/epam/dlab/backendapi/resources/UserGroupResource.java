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
import com.epam.dlab.backendapi.resources.dto.GroupDTO;
import com.epam.dlab.backendapi.resources.dto.UpdateRoleGroupDto;
import com.epam.dlab.backendapi.resources.dto.UpdateUserGroupDto;
import com.epam.dlab.backendapi.service.UserGroupService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Slf4j
@Path("group")
@RolesAllowed("/roleManagement")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserGroupResource {

	private final UserGroupService userGroupService;

	@Inject
	public UserGroupResource(UserGroupService userGroupService) {
		this.userGroupService = userGroupService;
	}


	@POST
	public Response createGroup(@Auth UserInfo userInfo,
								@Valid GroupDTO dto) {
		log.debug("Creating new group {}", dto.getName());
		userGroupService.createGroup(dto.getName(), dto.getRoleIds(), dto.getUsers());
		return Response.ok().build();
	}

	@PUT
	public Response updateGroup(@Auth UserInfo userInfo, @Valid GroupDTO dto) {
		log.debug("Updating group {}", dto.getName());
		userGroupService.updateGroup(dto.getName(), dto.getRoleIds(), dto.getUsers());
		return Response.ok().build();
	}

	@GET
	public Response getGroups(@Auth UserInfo userInfo) {
		log.debug("Getting all groups for admin {}...", userInfo.getName());
		return Response.ok(userGroupService.getAggregatedRolesByGroup(userInfo)).build();
	}

	@PUT
	@Path("role")
	public Response updateRolesForGroup(@Auth UserInfo userInfo, @Valid UpdateRoleGroupDto updateRoleGroupDto) {
		log.info("Admin {} is trying to add new group {} to roles {}", userInfo.getName(),
				updateRoleGroupDto.getGroup(), updateRoleGroupDto.getRoleIds());
		userGroupService.updateRolesForGroup(updateRoleGroupDto.getGroup(), updateRoleGroupDto.getRoleIds());
		return Response.ok().build();
	}

	@DELETE
	@Path("role")
	public Response deleteGroupFromRole(@Auth UserInfo userInfo,
										@QueryParam("group") @NotEmpty Set<String> groups,
										@QueryParam("roleId") @NotEmpty Set<String> roleIds) {
		log.info("Admin {} is trying to delete groups {} from roles {}", userInfo.getName(), groups, roleIds);
		userGroupService.removeGroupFromRole(groups, roleIds);
		return Response.ok().build();
	}

	@DELETE
	@Path("{id}")
	public Response deleteGroup(@Auth UserInfo userInfo,
								@PathParam("id") String group) {
		log.info("Admin {} is trying to delete group {} from application", userInfo.getName(), group);
		userGroupService.removeGroup(group);
		return Response.ok().build();
	}

	@PUT
	@Path("user")
	public Response addUserToGroup(@Auth UserInfo userInfo,
								   @Valid UpdateUserGroupDto updateUserGroupDto) {
		log.info("Admin {} is trying to add new users {} to group {}", userInfo.getName(),
				updateUserGroupDto.getUsers(), updateUserGroupDto.getGroup());
		userGroupService.addUsersToGroup(updateUserGroupDto.getGroup(), updateUserGroupDto.getUsers());
		return Response.ok().build();
	}

	@DELETE
	@Path("user")
	public Response deleteUserFromGroup(@Auth UserInfo userInfo,
										@QueryParam("user") @NotEmpty String user,
										@QueryParam("group") @NotEmpty String group) {
		log.info("Admin {} is trying to delete user {} from group {}", userInfo.getName(), user, group);
		userGroupService.removeUserFromGroup(group, user);
		return Response.ok().build();
	}
}
