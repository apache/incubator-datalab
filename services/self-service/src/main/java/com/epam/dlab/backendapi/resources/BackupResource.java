package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.BackupFormDTO;
import com.epam.dlab.backendapi.service.BackupService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
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
	public Response createBackup(@Auth UserInfo userInfo, @Valid BackupFormDTO backupFormDTO) {
		log.debug("Creating backup for user {} with parameters {}", userInfo.getName(), backupFormDTO);
		final EnvBackupDTO dto = requestBuilder.newBackupCreate(backupFormDTO, UUID.randomUUID().toString());
		final String uuid = backupService.createBackup(dto, userInfo);
		requestId.put(userInfo.getName(), uuid);
		return Response.accepted(uuid).build();
	}


	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBackups(@Auth UserInfo userInfo) {
		log.debug("Getting backups for user {}", userInfo.getName());
		return Response.ok(backupService.getBackups(userInfo.getName())).build();
	}

	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getBackup(@Auth UserInfo userInfo, @PathParam("id") String id) {
		log.debug("Getting backup with id {} for user {}", id, userInfo.getName());
		return Response.ok(backupService.getBackup(userInfo.getName(), id)).build();
	}

}
