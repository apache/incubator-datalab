package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.BackupInfoRecord;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.backup.EnvBackupStatus;

import java.util.List;

public interface BackupService {
	String createBackup(EnvBackupDTO dto, UserInfo userInfo);

	void updateStatus(EnvBackupDTO dto, String user, EnvBackupStatus status);

	List<BackupInfoRecord> getBackups(String userName);

	BackupInfoRecord getBackup(String userName, String id);
}
