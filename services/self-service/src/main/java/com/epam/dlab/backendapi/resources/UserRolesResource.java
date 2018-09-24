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
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.service.UserRolesService;
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
@Path("role")
@RolesAllowed("/roleManagement")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "User's roles resource", authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
public class UserRolesResource {

	private final UserRolesService userRolesService;

	@Inject
	public UserRolesResource(UserRolesService userRolesService) {
		this.userRolesService = userRolesService;
	}

	@GET
	@ApiOperation("List user's roles present in application")
	@ApiResponses(value = @ApiResponse(code = 200, message = "User roles present in application"))
	public Response getRoles(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		log.debug("Getting all roles for admin {}...", userInfo.getName());
		return Response.ok(userRolesService.getUserRoles()).build();
	}

	@GET
	@Path("/group")
	@ApiOperation("List groups with roles assigned to it")
	@ApiResponses(value = @ApiResponse(code = 200, message = "Groups present in application"))
	public Response getGroups(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		log.debug("Getting all groups for admin {}...", userInfo.getName());
		return Response.ok(userRolesService.getAggregatedRolesByGroup()).build();
	}

	@POST
	@ApiOperation(value = "Creates new user role")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Validation exception occurred"),
			@ApiResponse(code = 200, message = "User role is successfully added")}
	)
	public Response createRole(@Auth UserInfo userInfo, UserRoleDto dto) {
		log.info("Creating new role {} on behalf of admin {}...", dto, userInfo.getName());
		userRolesService.createRole(dto);
		return Response.ok().build();
	}

	@PUT
	@Path("group")
	@ApiOperation("Adds new user groups to existing roles")
	@ApiResponses({
			@ApiResponse(code = 404, message = "User role not found"),
			@ApiResponse(code = 400, message = "Validation exception occurred"),
			@ApiResponse(code = 200, message = "Group is successfully added to role")}
	)
	public Response addGroupToRole(@ApiParam(hidden = true) @Auth UserInfo userInfo,
								   @Valid @ApiParam UpdateRoleGroupDto updateRoleGroupDto) {
		log.info("Admin {} is trying to add new groups {} to roles {}", userInfo.getName(),
				updateRoleGroupDto.getGroups(), updateRoleGroupDto.getRoleIds());
		userRolesService.addGroupToRole(updateRoleGroupDto.getGroups(), updateRoleGroupDto.getRoleIds());
		return Response.ok().build();
	}

	@DELETE
	@Path("group")
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
		userRolesService.removeGroupFromRole(groups, roleIds);
		return Response.ok().build();
	}

	@PUT
	@Path("user")
	@ApiOperation("Adds new users existing roles")
	@ApiResponses({
			@ApiResponse(code = 404, message = "User role not found"),
			@ApiResponse(code = 400, message = "Validation exception occurred"),
			@ApiResponse(code = 200, message = "User successfully added to role")}
	)
	public Response addUserToRole(@ApiParam(hidden = true) @Auth UserInfo userInfo,
								  @ApiParam(required = true) @Valid UpdateRoleUserDto updateRoleUserDto) {
		log.info("Admin {} is trying to add new users {} to roles {}", userInfo.getName(),
				updateRoleUserDto.getUsers(), updateRoleUserDto.getRoleIds());
		userRolesService.addUserToRole(updateRoleUserDto.getUsers(), updateRoleUserDto.getRoleIds());
		return Response.ok().build();
	}

	@DELETE
	@Path("user")
	@ApiOperation("Removes users from existing roles")
	@ApiResponses({
			@ApiResponse(code = 404, message = "User role not found"),
			@ApiResponse(code = 400, message = "Validation exception occurred"),
			@ApiResponse(code = 200, message = "User successfully removed from role")}
	)
	public Response deleteUserFromRole(@ApiParam(hidden = true) @Auth UserInfo userInfo,
									   @ApiParam(required = true) @QueryParam("user") @NotEmpty Set<String> users,
									   @ApiParam(required = true) @QueryParam("roleId") @NotEmpty Set<String> roleIds) {
		log.info("Admin {} is trying to delete users {} from roles {}", userInfo.getName(), users, roleIds);
		userRolesService.removeUserFromRole(users, roleIds);
		return Response.ok().build();
	}
}
