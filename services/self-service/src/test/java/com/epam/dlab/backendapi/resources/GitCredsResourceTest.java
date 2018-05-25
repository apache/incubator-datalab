package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.GitCredentialService;
import com.epam.dlab.dto.exploratory.ExploratoryGitCreds;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.exceptions.DlabException;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class GitCredsResourceTest extends TestBase {

	private GitCredentialService gitCredentialService = mock(GitCredentialService.class);

	@Rule
	public final ResourceTestRule resources = getResourceTestRuleInstance(new GitCredsResource(gitCredentialService));

	@Before
	public void setup() throws AuthenticationException {
		authSetup();
	}

	@Test
	public void updateGitCreds() {
		doNothing().when(gitCredentialService).updateGitCredentials(any(UserInfo.class),
				any(ExploratoryGitCredsDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/user/git_creds")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(getExploratoryGitCredsDTO()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(gitCredentialService)
				.updateGitCredentials(refEq(getUserInfo()), refEq(getExploratoryGitCredsDTO(), "self"));
		verifyNoMoreInteractions(gitCredentialService);
	}

	@Test
	public void updateGitCredsWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		doNothing().when(gitCredentialService).updateGitCredentials(any(UserInfo.class),
				any(ExploratoryGitCredsDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/user/git_creds")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(getExploratoryGitCredsDTO()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(gitCredentialService)
				.updateGitCredentials(refEq(getUserInfo()), refEq(getExploratoryGitCredsDTO(), "self"));
		verifyNoMoreInteractions(gitCredentialService);
	}

	@Test
	public void updateGitCredsWithException() {
		doThrow(new DlabException("Cannot update the GIT credentials")).when(gitCredentialService)
				.updateGitCredentials(any(UserInfo.class), any(ExploratoryGitCredsDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/user/git_creds")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(getExploratoryGitCredsDTO()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(gitCredentialService).updateGitCredentials(refEq(getUserInfo()),
				refEq(getExploratoryGitCredsDTO(), "self"));
		verifyNoMoreInteractions(gitCredentialService);
	}

	@Test
	public void getGitCreds() {
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
	public void getGitCredsWithFailedAuth() throws AuthenticationException {
		authFailSetup();
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
	public void getGitCredsWithException() {
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
}
