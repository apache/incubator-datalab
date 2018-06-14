package com.epam.dlab.dto.backup;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@Builder
@ToString
public class EnvBackupDTO {
    private final List<String> configFiles;
    private final List<String> keys;
    private final List<String> certificates;
    private final List<String> jars;
    private final boolean databaseBackup;
    private final boolean logsBackup;
    private String backupFile;
    private String id;
}
