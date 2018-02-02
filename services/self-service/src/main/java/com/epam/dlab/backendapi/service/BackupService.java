package com.epam.dlab.backendapi.service;

import com.epam.dlab.dto.EnvBackupDTO;

public interface BackupService {
    void createBackup(String user, EnvBackupDTO dto);
}
