package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.BackupService;
import com.epam.dlab.dto.backup.EnvBackupStatusDTO;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("infrastructure/backup")
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class BackupCallback {

	@Inject
	private BackupService backupService;

	@Inject
	private RequestId requestId;

	@Context
	private UriInfo uriInfo;

	@POST
	@Path("/status")
	public Response status(EnvBackupStatusDTO dto) {
		requestId.remove(dto.getRequestId());
		log.debug("Updating status of backup status to {}", dto);
		backupService.updateStatus(dto.getEnvBackupDTO(), dto.getUser(),
				dto.getEnvBackupStatus().withErrorMessage(dto.getErrorMessage()));
		return Response.created(uriInfo.getRequestUri()).build();
	}
}
