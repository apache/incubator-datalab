package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.UserSettingsDAO;
import io.dropwizard.auth.*;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.*;

public class UserSettingsResourceTest {

	private final String TOKEN = "TOKEN";
	private final UserInfo userInfo = getUserInfo();
	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private UserSettingsDAO userSettingsDAO = mock(UserSettingsDAO.class);

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
			.addResource(new UserSettingsResource(userSettingsDAO))
			.build();

	@Test
	public void getSettings() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(userSettingsDAO.getUISettings(any(UserInfo.class))).thenReturn("someSettings");
		final Response response = resources.getJerseyTest()
				.target("/user/settings")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someSettings", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(userSettingsDAO).getUISettings(refEq(userInfo));
		verifyNoMoreInteractions(userSettingsDAO);
	}

	@Test
	public void saveSettings() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doNothing().when(userSettingsDAO).setUISettings(any(UserInfo.class), anyString());
		final Response response = resources.getJerseyTest()
				.target("/user/settings")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json("someSettings"));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(userSettingsDAO).setUISettings(refEq(userInfo), eq("someSettings"));
		verifyNoMoreInteractions(userSettingsDAO);
	}

	@Test
	public void saveSettingsWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new RuntimeException()).when(userSettingsDAO).setUISettings(any(UserInfo.class), anyString());
		final Response response = resources.getJerseyTest()
				.target("/user/settings")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json("someSettings"));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertTrue(response.readEntity(String.class).contains("{\"code\":500,\"message\":\"There was an error " +
				"processing your request. It has been logged"));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(userSettingsDAO).setUISettings(refEq(userInfo), eq("someSettings"));
		verifyNoMoreInteractions(userSettingsDAO);
	}

	private UserInfo getUserInfo() {
		return new UserInfo("testUser", TOKEN);
	}
}
