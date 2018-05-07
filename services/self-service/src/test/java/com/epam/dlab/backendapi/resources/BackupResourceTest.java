package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.BackupInfoRecord;
import com.epam.dlab.backendapi.service.BackupService;
import com.epam.dlab.backendapi.util.RequestBuilder;
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

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class BackupResourceTest {

	private static final String TOKEN = "TOKEN";
	private static final String USER = "testUser";
	private static final Date TIMESTAMP = new Date();
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
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
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(new UserInfo(USER, TOKEN)));
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
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(new UserInfo(USER, TOKEN)));
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

	private BackupInfoRecord getBackupInfo() {
		final List<String> configFiles = Arrays.asList("ss.yml", "sec.yml");
		final List<String> keys = Collections.singletonList("key.pub");
		final List<String> cert = Collections.singletonList("cert");
		final List<String> jars = Collections.singletonList("ss.jar");
		return new BackupInfoRecord(configFiles, keys, cert, jars, false, true, "file.backup",
				EnvBackupStatus.CREATED, null, TIMESTAMP);
	}
}