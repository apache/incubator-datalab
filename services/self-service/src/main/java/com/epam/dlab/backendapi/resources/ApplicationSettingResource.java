/*
 *
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
 *
 */
package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.service.ApplicationSettingService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
@Path("/settings")
@RolesAllowed("/api/settings")
@Api(value = "Application settings service", authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
public class ApplicationSettingResource {


	private final ApplicationSettingService settingService;

	@Inject
	public ApplicationSettingResource(ApplicationSettingService settingService) {
		this.settingService = settingService;
	}

	@PUT
	@Path("budget/{maxBudgetAllowed}")
	@ApiOperation("Updates max budget allowed application setting")
	@ApiResponses(@ApiResponse(code = 204, message = "Setting is updated"))
	public Response setMaxBudget(@ApiParam(hidden = true) @Auth UserInfo userInfo,
								 @ApiParam @PathParam("maxBudgetAllowed") @Min(1) Long maxBudget) {
		settingService.setMaxBudget(maxBudget);
		return Response.noContent().build();
	}

	@DELETE
	@Path("budget")
	@ApiOperation("Removes max budget allowed application setting")
	@ApiResponses(@ApiResponse(code = 204, message = "Setting is removed"))
	public Response removeAllowedBudget(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		log.debug("User {} is removing max budget application setting", userInfo.getName());
		settingService.removeMaxBudget();
		return Response.noContent().build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Gets application settings")
	@ApiResponses(@ApiResponse(code = 200, message = "Application settings value"))
	public Response getSettings(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		return Response.ok(settingService.getSettings()).build();

	}
}
