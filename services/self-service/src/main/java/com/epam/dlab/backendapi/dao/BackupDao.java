package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.resources.dto.BackupInfoRecord;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.backup.EnvBackupStatus;

import java.util.List;
import java.util.Optional;

public interface BackupDao {
    void createOrUpdate(EnvBackupDTO dto, String user, EnvBackupStatus status);

    List<BackupInfoRecord> getBackups(String userName);
    Optional<BackupInfoRecord> getBackup(String userName, String id);
}
