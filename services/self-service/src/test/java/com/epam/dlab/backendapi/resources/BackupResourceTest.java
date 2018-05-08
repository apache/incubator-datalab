package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.BackupFormDTO;
import com.epam.dlab.backendapi.resources.dto.BackupInfoRecord;
import com.epam.dlab.backendapi.service.BackupService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.backup.EnvBackupStatus;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.mappers.ResourceNotFoundExceptionMapper;
import io.dropwizard.auth.*;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class BackupResourceTest {

	private final String TOKEN = "TOKEN";
	private final String USER = "testUser";
	private final Date TIMESTAMP = new Date();
	private final UserInfo userInfo = getUserInfo();
	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private BackupService backupService = mock(BackupService.class);
	private RequestId requestId = mock(RequestId.class);
	private RequestBuilder requestBuilder = mock(RequestBuilder.class);

	@Rule
	public final ResourceTestRule resources = ResourceTestRule.builder()
			.setTestContainerFactory(new GrizzlyWebTestContainerFactory())
			.addProvider(new AuthDynamicFeature(new OAuthCredentialAuthFilter.Builder<UserInfo>()
					.setAuthenticator(authenticator)
					.setAuthorizer(authorizer)
					.setRealm("SUPER SECRET STUFF")
					.setPrefix("Bearer")
					.buildAuthFilter()))
			.addProvider(new ResourceNotFoundExceptionMapper())
			.addProvider(RolesAllowedDynamicFeature.class)
			.addProvider(new AuthValueFactoryProvider.Binder<>(UserInfo.class))
			.addResource(new BackupResource(backupService, requestBuilder, requestId))
			.build();

	@Test
	public void getBackup() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
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
	public void getBackupWithNotFoundException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(backupService.getBackup(anyString(), anyString())).thenThrow(new ResourceNotFoundException("Backup not " +
				"found"));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/backup/1")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
		assertEquals("Backup not found", response.readEntity(String.class));
		assertEquals(MediaType.TEXT_PLAIN, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(backupService).getBackup(USER.toLowerCase(), "1");
		verifyNoMoreInteractions(backupService);
		verifyZeroInteractions(requestId, requestBuilder);
	}

	@Test
	public void getBackups() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
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
	public void createBackup() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
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
		verify(backupService).createBackup(getEnvBackupDto(), userInfo);
		verify(requestId).put(USER.toLowerCase(), "someUuid");
		verifyNoMoreInteractions(requestBuilder, backupService, requestId);
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

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}
}