package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.BackupFormDTO;
import com.epam.dlab.backendapi.service.BackupService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.EnvBackupDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/infrastructure/backup")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class BackupResource {

    @Inject
    private BackupService backupService;

    @POST
    public Response backup(@Auth UserInfo userInfo, @Valid BackupFormDTO backupFormDTO) {
        log.debug("Creating backup for user {} with parameters {}", userInfo.getName(), backupFormDTO);
        final EnvBackupDTO dto = RequestBuilder.newBackupCreate(backupFormDTO);
        backupService.createBackup(userInfo.getName(), dto);
        return Response.ok().build();
    }

}
