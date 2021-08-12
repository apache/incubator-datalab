/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.dao.BackupDAO;
import com.epam.datalab.backendapi.resources.dto.BackupInfoRecord;
import com.epam.datalab.backendapi.service.BackupService;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.backup.EnvBackupDTO;
import com.epam.datalab.dto.backup.EnvBackupStatus;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.rest.client.RESTService;
import com.epam.datalab.rest.contracts.BackupAPI;
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
    private BackupDAO backupDao;

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
