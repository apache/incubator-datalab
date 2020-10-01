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

package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.resources.dto.BackupFormDTO;
import com.epam.datalab.backendapi.resources.dto.BackupInfoRecord;
import com.epam.datalab.backendapi.service.BackupService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.dto.backup.EnvBackupDTO;
import com.epam.datalab.dto.backup.EnvBackupStatus;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class BackupResourceTest extends TestBase {

    private final Date TIMESTAMP = new Date();
    private BackupService backupService = mock(BackupService.class);
    private RequestId requestId = mock(RequestId.class);
    private RequestBuilder requestBuilder = mock(RequestBuilder.class);

    @Rule
    public final ResourceTestRule resources =
            getResourceTestRuleInstance(new BackupResource(backupService, requestBuilder, requestId));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void getBackup() {
        when(backupService.getBackup(anyString(), anyString())).thenReturn(getBackupInfo());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/backup/1")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(getBackupInfo(), response.readEntity(BackupInfoRecord.class));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(backupService).getBackup(USER.toLowerCase(), "1");
        verifyNoMoreInteractions(backupService);
        verifyZeroInteractions(requestId, requestBuilder);
    }

    @Test
    public void getBackupWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        when(backupService.getBackup(anyString(), anyString())).thenReturn(getBackupInfo());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/backup/1")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verifyZeroInteractions(backupService, requestId, requestBuilder);
    }

    @Test
    public void getBackupWithNotFoundException() {
        when(backupService.getBackup(anyString(), anyString())).thenThrow(new ResourceNotFoundException("Backup not " +
                "found"));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/backup/1")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(backupService).getBackup(USER.toLowerCase(), "1");
        verifyNoMoreInteractions(backupService);
        verifyZeroInteractions(requestId, requestBuilder);
    }

    @Test
    public void getBackups() {
        when(backupService.getBackups(anyString())).thenReturn(Collections.singletonList(getBackupInfo()));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/backup")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(Collections.singletonList(getBackupInfo()),
                response.readEntity(new GenericType<List<BackupInfoRecord>>() {
                }));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(backupService).getBackups(USER.toLowerCase());
        verifyNoMoreInteractions(backupService);
        verifyZeroInteractions(requestId, requestBuilder);
    }

    @Test
    public void getBackupsWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        when(backupService.getBackups(anyString())).thenReturn(Collections.singletonList(getBackupInfo()));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/backup")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verifyZeroInteractions(backupService, requestId, requestBuilder);
    }

    @Test
    public void createBackup() {
        when(requestBuilder.newBackupCreate(any(BackupFormDTO.class), anyString())).thenReturn(getEnvBackupDto());
        when(backupService.createBackup(any(EnvBackupDTO.class), any(UserInfo.class))).thenReturn("someUuid");
        when(requestId.put(anyString(), anyString())).thenReturn("someUuid");

        final Response response = resources.getJerseyTest()
                .target("/infrastructure/backup")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getBackupFormDto()));

        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(requestBuilder).newBackupCreate(eq(getBackupFormDto()), anyString());
        verify(backupService).createBackup(getEnvBackupDto(), getUserInfo());
        verify(requestId).put(USER.toLowerCase(), "someUuid");
        verifyNoMoreInteractions(requestBuilder, backupService, requestId);
    }

    @Test
    public void createBackupWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        when(requestBuilder.newBackupCreate(any(BackupFormDTO.class), anyString())).thenReturn(getEnvBackupDto());
        when(backupService.createBackup(any(EnvBackupDTO.class), any(UserInfo.class))).thenReturn("someUuid");
        when(requestId.put(anyString(), anyString())).thenReturn("someUuid");

        final Response response = resources.getJerseyTest()
                .target("/infrastructure/backup")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getBackupFormDto()));

        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verifyZeroInteractions(requestBuilder, backupService, requestId);
    }

    private BackupInfoRecord getBackupInfo() {
        final List<String> configFiles = Arrays.asList("ss.yml", "sec.yml");
        final List<String> keys = Collections.singletonList("key.pub");
        final List<String> cert = Collections.singletonList("cert");
        final List<String> jars = Collections.singletonList("ss.jar");
        return new BackupInfoRecord(configFiles, keys, cert, jars, false, true, "file.backup",
                EnvBackupStatus.CREATED, null, TIMESTAMP);
    }

    private BackupFormDTO getBackupFormDto() {
        return new BackupFormDTO(Arrays.asList("ss.yml", "sec.yml"), Collections.singletonList("key.pub"),
                Collections.singletonList("cert"), Collections.singletonList("ss.jar"), false, true);
    }

    private EnvBackupDTO getEnvBackupDto() {
        return EnvBackupDTO.builder()
                .configFiles(Arrays.asList("ss.yml", "sec.yml"))
                .keys(Collections.singletonList("key.pub"))
                .certificates(Collections.singletonList("cert"))
                .jars(Collections.singletonList("ss.jar"))
                .databaseBackup(false)
                .logsBackup(true)
                .backupFile("file.backup")
                .id("someId")
                .build();
    }
}