package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.GitCredentialService;
import com.epam.dlab.dto.exploratory.ExploratoryGitCreds;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
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
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class GitCredsResourceTest {

	private final String TOKEN = "TOKEN";
	private final String USER = "testUser";
	private final UserInfo userInfo = getUserInfo();
	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private GitCredentialService gitCredentialService = mock(GitCredentialService.class);

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
			.addResource(new GitCredsResource(gitCredentialService))
			.build();

	@Test
	public void updateGitCreds() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doNothing().when(gitCredentialService).updateGitCredentials(any(UserInfo.class),
				any(ExploratoryGitCredsDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/user/git_creds")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(getExploratoryGitCredsDTO()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(gitCredentialService).updateGitCredentials(refEq(userInfo), refEq(getExploratoryGitCredsDTO(), "self"));
		verifyNoMoreInteractions(gitCredentialService);
	}

	@Test
	public void updateGitCredsWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Cannot update the GIT credentials")).when(gitCredentialService)
				.updateGitCredentials(any(UserInfo.class), any(ExploratoryGitCredsDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/user/git_creds")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(getExploratoryGitCredsDTO()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(gitCredentialService).updateGitCredentials(refEq(userInfo), refEq(getExploratoryGitCredsDTO(), "self"));
		verifyNoMoreInteractions(gitCredentialService);
	}

	@Test
	public void getGitCreds() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		ExploratoryGitCredsDTO egcDto = getExploratoryGitCredsDTO();
		when(gitCredentialService.getGitCredentials(anyString())).thenReturn(egcDto);
		final Response response = resources.getJerseyTest()
				.target("/user/git_creds")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(egcDto.getGitCreds(), response.readEntity(ExploratoryGitCredsDTO.class).getGitCreds());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(gitCredentialService).getGitCredentials(USER.toLowerCase());
		verifyNoMoreInteractions(gitCredentialService);
	}

	@Test
	public void getGitCredsWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Cannot load GIT credentials for user"))
				.when(gitCredentialService).getGitCredentials(anyString());
		final Response response = resources.getJerseyTest()
				.target("/user/git_creds")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(gitCredentialService).getGitCredentials(USER.toLowerCase());
		verifyNoMoreInteractions(gitCredentialService);
	}

	private ExploratoryGitCredsDTO getExploratoryGitCredsDTO() {
		ExploratoryGitCredsDTO exploratoryGitCredsDTO = new ExploratoryGitCredsDTO();
		exploratoryGitCredsDTO.setGitCreds(Collections.singletonList(new ExploratoryGitCreds()));
		return exploratoryGitCredsDTO;
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}
}
