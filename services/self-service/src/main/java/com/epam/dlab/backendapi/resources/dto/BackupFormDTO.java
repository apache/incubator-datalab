package com.epam.dlab.backendapi.resources.dto;

import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

@Data
@ToString
public class BackupFormDTO {
    @NotEmpty
    private final List<String> configFiles;
    @NotEmpty
    private final List<String> keys;
    @NotEmpty
    private final List<String> certificates;
    private final List<String> jars;
    private final boolean databaseBackup;
    private final boolean logsBackup;
}
