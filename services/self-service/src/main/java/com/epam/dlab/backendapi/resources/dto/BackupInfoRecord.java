package com.epam.dlab.backendapi.resources.dto;

import com.epam.dlab.dto.backup.EnvBackupStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BackupInfoRecord {

    private final List<String> configFiles;
    private final List<String> keys;
    private final List<String> certificates;
    private final List<String> jars;
    private final boolean databaseBackup;
    private final boolean logsBackup;
    private final String backupFile;
    private final EnvBackupStatus status;
    @JsonProperty("error_message")
    private final String errorMessage;
    private final Date timestamp;
}
