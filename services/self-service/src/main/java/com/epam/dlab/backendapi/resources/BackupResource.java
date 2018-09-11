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
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.BackupFormDTO;
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.service.BackupService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;


@Slf4j
@Path("/infrastructure/backup")
@RolesAllowed("/api/infrastructure/backup")
@Api(value = "Backup service", authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
public class BackupResource {

	private final BackupService backupService;
	private final RequestBuilder requestBuilder;
	private final RequestId requestId;

	@Inject
	public BackupResource(BackupService backupService, RequestBuilder requestBuilder, RequestId requestId) {
		this.backupService = backupService;
		this.requestBuilder = requestBuilder;
		this.requestId = requestId;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation("Creates backup")
	@ApiResponses(@ApiResponse(code = 202, message = "Backup has been created"))
	public Response createBackup(@ApiParam(hidden = true) @Auth UserInfo userInfo,
								 @ApiParam(value = "Backup form DTO", required = true)
								 @Valid BackupFormDTO backupFormDTO) {
		log.debug("Creating backup for user {} with parameters {}", userInfo.getName(), backupFormDTO);
		final EnvBackupDTO dto = requestBuilder.newBackupCreate(backupFormDTO, UUID.randomUUID().toString());
		final String uuid = backupService.createBackup(dto, userInfo);
		requestId.put(userInfo.getName(), uuid);
		return Response.accepted(uuid).build();
	}


	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Fetches all backups")
	@ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid user's name"),
			@ApiResponse(code = 200, message = "Backups were fetched successfully")})
	public Response getBackups(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		log.debug("Getting backups for user {}", userInfo.getName());
		return Response.ok(backupService.getBackups(userInfo.getName())).build();
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation("Fetches backup by ID")
	@ApiResponses({@ApiResponse(code = 404, message = "Backup with ID not found"),
			@ApiResponse(code = 200, message = "Backup with ID fetched successfully")})
	public Response getBackup(@ApiParam(hidden = true) @Auth UserInfo userInfo,
							  @ApiParam(value = "Backup's ID", required = true) @PathParam("id") String id) {
		log.debug("Getting backup with id {} for user {}", id, userInfo.getName());
		return Response.ok(backupService.getBackup(userInfo.getName(), id)).build();
	}

}
