package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.exceptions.DlabException;
import io.dropwizard.auth.*;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EdgeResourceTest {

	private final String TOKEN = "TOKEN";
	private final UserInfo userInfo = getUserInfo();

	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private EdgeService edgeService = mock(EdgeService.class);

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
			.addResource(new EdgeResource(edgeService))
			.build();

	@Test
	public void start() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(edgeService.start(any(UserInfo.class))).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/edge/start")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(userInfo));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(edgeService).start(userInfo);
		verifyNoMoreInteractions(edgeService);
	}

	@Test
	public void startWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(edgeService.start(any(UserInfo.class))).thenThrow(new DlabException("Could not start edge node"));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/edge/start")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(userInfo));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		String expectedJson = "\"code\":500,\"message\":\"There was an error processing your request. " +
				"It has been logged";
		String actualJson = response.readEntity(String.class);
		assertTrue(actualJson.contains(expectedJson));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(edgeService).start(userInfo);
		verifyNoMoreInteractions(edgeService);
	}

	@Test
	public void stop() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(edgeService.stop(any(UserInfo.class))).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/edge/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(userInfo));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(edgeService).stop(userInfo);
		verifyNoMoreInteractions(edgeService);
	}

	@Test
	public void stopWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(edgeService.stop(any(UserInfo.class))).thenThrow(new DlabException("Could not stop edge node"));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/edge/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(userInfo));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		String expectedJson = "\"code\":500,\"message\":\"There was an error processing your request. " +
				"It has been logged";
		String actualJson = response.readEntity(String.class);
		assertTrue(actualJson.contains(expectedJson));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(edgeService).stop(userInfo);
		verifyNoMoreInteractions(edgeService);
	}

	private UserInfo getUserInfo() {
		return new UserInfo("testUser", TOKEN);
	}

}
