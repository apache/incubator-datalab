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
import com.epam.dlab.backendapi.resources.dto.SystemInfoDto;
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.service.SystemInfoService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
@Path("sysinfo")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("sysinfo")
@Api(value = "System information resource", authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
public class SystemInfoResource {

	private SystemInfoService systemInfoService;

	@Inject
	public SystemInfoResource(SystemInfoService systemInfoService) {
		this.systemInfoService = systemInfoService;
	}


	@GET
	@ApiOperation("Returns information about current system load")
	@ApiResponses(@ApiResponse(code = 200, message = "System information (CPU, RAM etc. )"))
	public Response getSystemInfo(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		log.debug("Getting system info for user {}...", userInfo.getName());
		final SystemInfoDto systemInfoDto = systemInfoService.getSystemInfo();
		return Response.ok(systemInfoDto).build();
	}
}
