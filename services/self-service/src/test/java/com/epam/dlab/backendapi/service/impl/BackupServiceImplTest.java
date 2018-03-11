package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.BackupDao;
import com.epam.dlab.backendapi.resources.dto.BackupInfoRecord;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.backup.EnvBackupStatus;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.BackupAPI;
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
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BackupServiceImplTest {

	private final String USER = "test";

	@Mock
	private RESTService provisioningService;
	@Mock
	private BackupDao backupDao;

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
		verifyNoMoreInteractions(backupDao);

		verify(provisioningService).post(BackupAPI.BACKUP, token, ebDto, String.class);
		verifyNoMoreInteractions(provisioningService);
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

}
