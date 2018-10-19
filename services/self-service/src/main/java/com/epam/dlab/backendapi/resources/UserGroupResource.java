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
import com.epam.dlab.backendapi.resources.dto.CreateGroupDto;
import com.epam.dlab.backendapi.resources.dto.UpdateRoleGroupDto;
import com.epam.dlab.backendapi.resources.dto.UpdateUserGroupDto;
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.service.UserGroupService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

@Slf4j
@Path("group")
@RolesAllowed("/roleManagement")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "User's groups resource", authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
public class UserGroupResource {

	private final UserGroupService userGroupService;

	@Inject
	public UserGroupResource(UserGroupService userGroupService) {
		this.userGroupService = userGroupService;
	}


	@POST
	@ApiOperation("Creates group with roles assigned to it")
	@ApiResponses({
			@ApiResponse(code = 200, message = "Group successfully created"),
			@ApiResponse(code = 404, message = "User role not found")
	})
	public Response createGroup(@ApiParam(hidden = true) @Auth UserInfo userInfo,
								@Valid @ApiParam CreateGroupDto dto) {
		log.debug("Creating new group {}", dto.getName());
		userGroupService.createGroup(dto.getName(), dto.getRoleIds(), dto.getUsers());
		return Response.ok().build();
	}

	@GET
	@ApiOperation("List groups with roles assigned to it")
	@ApiResponses(@ApiResponse(code = 200, message = "Groups present in application"))
	public Response getGroups(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		log.debug("Getting all groups for admin {}...", userInfo.getName());
		return Response.ok(userGroupService.getAggregatedRolesByGroup()).build();
	}

	@PUT
	@Path("role")
	@ApiOperation("Overrides roles for group")
	@ApiResponses({
			@ApiResponse(code = 404, message = "User role not found"),
			@ApiResponse(code = 400, message = "Validation exception occurred"),
			@ApiResponse(code = 200, message = "Group is successfully added to role")}
	)
	public Response updateRolesForGroup(@ApiParam(hidden = true) @Auth UserInfo userInfo,
										@Valid @ApiParam UpdateRoleGroupDto updateRoleGroupDto) {
		log.info("Admin {} is trying to add new group {} to roles {}", userInfo.getName(),
				updateRoleGroupDto.getGroup(), updateRoleGroupDto.getRoleIds());
		userGroupService.updateRolesForGroup(updateRoleGroupDto.getGroup(), updateRoleGroupDto.getRoleIds());
		return Response.ok().build();
	}

	@DELETE
	@Path("role")
	@ApiOperation("Removes user groups from existing roles")
	@ApiResponses({
			@ApiResponse(code = 404, message = "User role not found"),
			@ApiResponse(code = 400, message = "Validation exception occurred"),
			@ApiResponse(code = 200, message = "Group successfully removed from role")}
	)
	public Response deleteGroupFromRole(@ApiParam(hidden = true) @Auth UserInfo userInfo,
										@ApiParam(required = true) @QueryParam("group") @NotEmpty Set<String> groups,
										@ApiParam(required = true) @QueryParam("roleId") @NotEmpty Set<String> roleIds) {
		log.info("Admin {} is trying to delete groups {} from roles {}", userInfo.getName(), groups, roleIds);
		userGroupService.removeGroupFromRole(groups, roleIds);
		return Response.ok().build();
	}

	@DELETE
	@Path("{id}")
	@ApiOperation("Removes user group from roles that are assigned to it")
	@ApiResponses({
			@ApiResponse(code = 400, message = "Validation exception occurred"),
			@ApiResponse(code = 200, message = "Group successfully removed")}
	)
	public Response deleteGroup(@ApiParam(hidden = true) @Auth UserInfo userInfo,
								@ApiParam @PathParam("id") String group) {
		log.info("Admin {} is trying to delete group {} from application", userInfo.getName(), group);
		userGroupService.removeGroup(group);
		return Response.ok().build();
	}

	@PUT
	@Path("user")
	@ApiOperation("Adds new users to user group")
	@ApiResponses({
			@ApiResponse(code = 404, message = "User role not found"),
			@ApiResponse(code = 400, message = "Validation exception occurred"),
			@ApiResponse(code = 200, message = "User successfully added to role")}
	)
	public Response addUserToGroup(@ApiParam(hidden = true) @Auth UserInfo userInfo,
								   @ApiParam(required = true) @Valid UpdateUserGroupDto updateUserGroupDto) {
		log.info("Admin {} is trying to add new users {} to group {}", userInfo.getName(),
				updateUserGroupDto.getUsers(), updateUserGroupDto.getGroup());
		userGroupService.addUsersToGroup(updateUserGroupDto.getGroup(), updateUserGroupDto.getUsers());
		return Response.ok().build();
	}

	@DELETE
	@Path("user")
	@ApiOperation("Removes users from existing group")
	@ApiResponses({
			@ApiResponse(code = 404, message = "Group not found"),
			@ApiResponse(code = 400, message = "Validation exception occurred"),
			@ApiResponse(code = 200, message = "User successfully removed from group")}
	)
	public Response deleteUserFromGroup(@ApiParam(hidden = true) @Auth UserInfo userInfo,
										@ApiParam(required = true) @QueryParam("user") @NotEmpty String user,
										@ApiParam(required = true) @QueryParam("group") @NotEmpty String group) {
		log.info("Admin {} is trying to delete user {} from group {}", userInfo.getName(), user, group);
		userGroupService.removeUserFromGroup(group, user);
		return Response.ok().build();
	}
}
