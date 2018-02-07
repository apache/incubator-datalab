package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.backup.EnvBackupStatus;
import com.epam.dlab.dto.backup.EnvBackupStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;

@Slf4j
public class BackupCallbackHandler implements FileHandlerCallback {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    private static final String STATUS_FIELD = "status";
    private static final String BACKUP_FILE_FIELD = "backup_file";
    private static final String ERROR_MESSAGE_FIELD = "error_message";
    private final String uuid;
    private final EnvBackupDTO dto;
    private final RESTService selfService;
    private final String callbackUrl;
    private final String user;

    public BackupCallbackHandler(RESTService selfService, String callbackUrl, String user, EnvBackupDTO dto) {
        this.selfService = selfService;
        this.uuid = dto.getId();
        this.callbackUrl = callbackUrl;
        this.user = user;
        this.dto = dto;
    }

    @Override
    public String getUUID() {
        return uuid;
    }

    @Override
    public boolean checkUUID(String uuid) {
        return this.uuid.equals(uuid);
    }

    @Override
    public boolean handle(String fileName, byte[] content) throws Exception {
        final String fileContent = new String(content);
        log.debug("Got file {} while waiting for UUID {}, backup response: {}", fileName, uuid, fileContent);

        final JsonNode jsonNode = MAPPER.readTree(fileContent);
        final EnvBackupStatus status = EnvBackupStatus.fromValue(jsonNode.get(STATUS_FIELD).textValue());
        EnvBackupStatusDTO envBackupStatusDTO;
        if (EnvBackupStatus.CREATED == status) {
            envBackupStatusDTO = buildBackupStatusDto(EnvBackupStatus.CREATED)
                    .withBackupFile(jsonNode.get(BACKUP_FILE_FIELD).textValue());
        } else {
            envBackupStatusDTO = buildBackupStatusDto(EnvBackupStatus.FAILED)
                    .withErrorMessage(jsonNode.get(ERROR_MESSAGE_FIELD).textValue());
        }
        selfServicePost(envBackupStatusDTO);
        return EnvBackupStatus.CREATED == status;
    }

    private void selfServicePost(EnvBackupStatusDTO statusDTO) {
        log.debug("Send post request to self service {} for UUID {}, object is {}", uuid, statusDTO);
        try {
            selfService.post(callbackUrl, statusDTO, Response.class);
        } catch (Exception e) {
            log.error("Send request or response error for UUID {}: {}", uuid, e.getLocalizedMessage(), e);
            throw new DlabException("Send request or response error for UUID " + uuid + ": " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void handleError(String errorMessage) {
        buildBackupStatusDto(EnvBackupStatus.FAILED)
                .withErrorMessage(errorMessage);
    }

    protected EnvBackupStatusDTO buildBackupStatusDto(EnvBackupStatus status) {
        return new EnvBackupStatusDTO()
                .withRequestId(uuid)
                .withEnvBackupDTO(dto)
                .withStatus(status)
                .withUser(user);
    }

}
