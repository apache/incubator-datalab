package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.BackupDao;
import com.epam.dlab.backendapi.resources.dto.BackupInfoRecord;
import com.epam.dlab.backendapi.service.BackupService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.backup.EnvBackupStatus;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.BackupAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.util.List;

@Singleton
public class BackupServiceImpl implements BackupService {

	@Inject
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;

	@Inject
	private BackupDao backupDao;

	@Override
	public String createBackup(EnvBackupDTO dto, UserInfo user) {
		updateStatus(dto, user.getName(), EnvBackupStatus.CREATING);
		return provisioningService.post(BackupAPI.BACKUP, user.getAccessToken(), dto, String.class);
	}

	@Override
	public void updateStatus(EnvBackupDTO dto, String user, EnvBackupStatus status) {
		backupDao.createOrUpdate(dto, user, status);
	}

	@Override
	public List<BackupInfoRecord> getBackups(String userName) {
		return backupDao.getBackups(userName);
	}

	@Override
	public BackupInfoRecord getBackup(String userName, String id) {
		return backupDao.getBackup(userName, id).orElseThrow(() -> new ResourceNotFoundException(
				String.format("Backup with id %s was not found for user %s", id, userName)));
	}
}
