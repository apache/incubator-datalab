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
import com.epam.datalab.dto.backup.EnvBackupDTO;
import com.epam.datalab.dto.backup.EnvBackupStatus;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.rest.client.RESTService;
import com.epam.datalab.rest.contracts.BackupAPI;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BackupServiceImplTest {

    private final String USER = "test";

    @Mock
    private RESTService provisioningService;
    @Mock
    private BackupDAO backupDao;

    @InjectMocks
    private BackupServiceImpl backupService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void createBackup() {
        doNothing().when(backupDao).createOrUpdate(any(EnvBackupDTO.class), anyString(), any(EnvBackupStatus.class));
        String expectedUuid = "1234-56789765-4321";
        String token = "token";
        when(provisioningService.post(refEq(BackupAPI.BACKUP), eq(token), any(EnvBackupDTO.class), any()))
                .thenReturn(expectedUuid);

        EnvBackupDTO ebDto = EnvBackupDTO.builder().build();
        UserInfo userInfo = new UserInfo(USER, token);
        String uuid = backupService.createBackup(ebDto, userInfo);
        assertNotNull(uuid);
        assertEquals(expectedUuid, uuid);

        verify(backupDao).createOrUpdate(ebDto, USER, EnvBackupStatus.CREATING);
        verify(provisioningService).post(BackupAPI.BACKUP, token, ebDto, String.class);
        verifyNoMoreInteractions(backupDao, provisioningService);
    }

    @Test
    public void updateStatus() {
        doNothing().when(backupDao).createOrUpdate(any(EnvBackupDTO.class), anyString(), any(EnvBackupStatus.class));

        EnvBackupDTO ebDto = EnvBackupDTO.builder().build();
        backupService.updateStatus(ebDto, USER, EnvBackupStatus.CREATING);

        verify(backupDao).createOrUpdate(ebDto, USER, EnvBackupStatus.CREATING);
        verifyNoMoreInteractions(backupDao);
    }

    @Test
    public void getBackups() {
        BackupInfoRecord biRecord = mock(BackupInfoRecord.class);
        when(backupDao.getBackups(anyString())).thenReturn(Collections.singletonList(biRecord));

        List<BackupInfoRecord> biRecords = backupService.getBackups(USER);
        assertNotNull(biRecords);
        assertEquals(1, biRecords.size());
        assertEquals(Collections.singletonList(biRecord), biRecords);

        verify(backupDao).getBackups(USER);
        verifyNoMoreInteractions(backupDao);
    }

    @Test
    public void getBackup() {
        BackupInfoRecord biRecord = mock(BackupInfoRecord.class);
        when(backupDao.getBackup(anyString(), anyString())).thenReturn(Optional.of(biRecord));

        String id = "someId";
        BackupInfoRecord actualBiRecord = backupService.getBackup(USER, id);
        assertNotNull(actualBiRecord);
        assertEquals(biRecord, actualBiRecord);

        verify(backupDao).getBackup(USER, id);
        verifyNoMoreInteractions(backupDao);
    }

    @Test
    public void getBackupWithException() {
        String id = "someId";
        doThrow(new ResourceNotFoundException(String.format("Backup with id %s was not found for user %s", id, USER)))
                .when(backupDao).getBackup(USER, id);
        expectedException.expect(ResourceNotFoundException.class);
        expectedException.expectMessage("Backup with id " + id + " was not found for user " + USER);

        backupService.getBackup(USER, id);
    }

    @Test
    public void getBackupWhenBackupIsAbsent() {
        String id = "someId";
        when(backupDao.getBackup(USER, id)).thenReturn(Optional.empty());
        expectedException.expect(ResourceNotFoundException.class);
        expectedException.expectMessage("Backup with id " + id + " was not found for user " + USER);

        backupService.getBackup(USER, id);
    }

}
