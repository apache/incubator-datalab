package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.exceptions.ResourceConflictException;
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
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class EnvironmentResourceTest {

	private final String TOKEN = "TOKEN";
	private final String USER = "testUser";
	private final Date TIMESTAMP = new Date();
	private final UserInfo userInfo = getUserInfo();
	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private EnvironmentService environmentService = mock(EnvironmentService.class);

	@Rule
	public final ResourceTestRule resources = ResourceTestRule.builder()
			.setTestContainerFactory(new GrizzlyWebTestContainerFactory())
			.addProvider(new AuthDynamicFeature(new OAuthCredentialAuthFilter.Builder<UserInfo>()
					.setAuthenticator(authenticator)
					.setAuthorizer(authorizer)
					.setRealm("SUPER SECRET STUFF")
					.setPrefix("Bearer")
					.buildAuthFilter()))
			.addProvider(RolesAllowedDynamicFeature.class)
			.addProvider(new AuthValueFactoryProvider.Binder<>(UserInfo.class))
			.addResource(new EnvironmentResource(environmentService))
			.build();

	@Test
	public void getUsersWithActiveEnv() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(environmentService.getActiveUsers()).thenReturn(Collections.singleton("activeUser"));
		final Response response = resources.getJerseyTest()
				.target("/environment/user/active")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(Collections.singleton("activeUser"), response.readEntity(new GenericType<Set<String>>() {
		}));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).getActiveUsers();
		verifyNoMoreInteractions(environmentService);
	}

	@Test
	public void terminateEnv() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doNothing().when(environmentService).terminateEnvironment(anyString());
		final Response response = resources.getJerseyTest()
				.target("/environment/terminate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.text(USER));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).terminateEnvironment(USER);
		verifyNoMoreInteractions(environmentService);
	}

	@Test
	public void terminateEnvWithResourceConflictException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new ResourceConflictException("Can not terminate environment because on of user resource is in " +
				"status" +
				" CREATING or STARTING")).when(environmentService).terminateEnvironment(anyString());
		final Response response = resources.getJerseyTest()
				.target("/environment/terminate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.text(USER));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).terminateEnvironment(USER);
		verifyNoMoreInteractions(environmentService);
	}

	@Test
	public void stopEnv() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doNothing().when(environmentService).stopEnvironment(anyString());
		final Response response = resources.getJerseyTest()
				.target("/environment/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.text(USER));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).stopEnvironment(USER);
		verifyNoMoreInteractions(environmentService);
	}

	@Test
	public void stopEnvWithResourceConflictException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new ResourceConflictException("Can not stop environment because on of user resource is in status" +
				" CREATING or STARTING")).when(environmentService).stopEnvironment(anyString());
		final Response response = resources.getJerseyTest()
				.target("/environment/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.text(USER));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).stopEnvironment(USER);
		verifyNoMoreInteractions(environmentService);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}
}
