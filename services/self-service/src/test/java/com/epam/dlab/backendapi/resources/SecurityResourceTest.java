package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.backendapi.domain.EnvStatusListener;
import com.epam.dlab.dto.UserCredentialDTO;
import com.epam.dlab.rest.client.RESTService;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SecurityResourceTest extends TestBase {

	private SecurityDAO securityDAO = mock(SecurityDAO.class);
	private RESTService securityService = mock(RESTService.class);
	private EnvStatusListener envStatusListener = mock(EnvStatusListener.class);
	private SelfServiceApplicationConfiguration configuration = mock(SelfServiceApplicationConfiguration.class);

	@Rule
	public final ResourceTestRule resources = getResourceTestRuleInstance(
			new SecurityResource(securityDAO, securityService, envStatusListener, configuration));

	@Before
	public void setup() throws AuthenticationException {
		authSetup();
	}

	@Test
	public void userLoginWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		doNothing().when(securityDAO).writeLoginAttempt(any(UserCredentialDTO.class));
		when(securityService.post(anyString(), any(UserCredentialDTO.class), any())).thenReturn(mock(Response.class));
		final Response response = resources.getJerseyTest()
				.target("/user/login")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getUserCredentialDTO()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.valueOf("text/html;charset=ISO-8859-1"), response.getMediaType());

		verify(securityDAO).writeLoginAttempt(refEq(getUserCredentialDTO()));
		verify(securityService).post(eq("login"), refEq(getUserCredentialDTO()), eq(Response.class));
		verifyNoMoreInteractions(securityDAO, securityService);
	}

	@Test
	public void userLoginWithException() {
		doNothing().when(securityDAO).writeLoginAttempt(any(UserCredentialDTO.class));
		when(securityService.post(anyString(), any(UserCredentialDTO.class), any())).thenReturn(mock(Response.class));
		final Response response = resources.getJerseyTest()
				.target("/user/login")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getUserCredentialDTO()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.valueOf("text/html;charset=ISO-8859-1"), response.getMediaType());

		verify(securityDAO).writeLoginAttempt(refEq(getUserCredentialDTO()));
		verify(securityService).post(eq("login"), refEq(getUserCredentialDTO()), eq(Response.class));
		verifyNoMoreInteractions(securityDAO, securityService);
	}

	@Test
	public void authorizeWhenRolePolicyNotEnabled() {
		doNothing().when(envStatusListener).registerSession(any(UserInfo.class));
		when(configuration.isRolePolicyEnabled()).thenReturn(false);
		final Response response = resources.getJerseyTest()
				.target("/user/authorize")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(USER));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(envStatusListener).registerSession(refEq(getUserInfo()));
		verify(configuration).isRolePolicyEnabled();
		verifyNoMoreInteractions(envStatusListener, configuration);
	}

	@Test
	public void authorizeWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		doNothing().when(envStatusListener).registerSession(any(UserInfo.class));
		when(configuration.isRolePolicyEnabled()).thenReturn(false);
		final Response response = resources.getJerseyTest()
				.target("/user/authorize")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(USER));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(envStatusListener).registerSession(refEq(getUserInfo()));
		verify(configuration).isRolePolicyEnabled();
		verifyNoMoreInteractions(envStatusListener, configuration);
	}

	@Test
	public void authorizeWhenRolePolicyEnabled() {
		doNothing().when(envStatusListener).registerSession(any(UserInfo.class));
		when(configuration.isRolePolicyEnabled()).thenReturn(true);
		when(configuration.getRoleDefaultAccess()).thenReturn(true);
		final Response response = resources.getJerseyTest()
				.target("/user/authorize")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(USER));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(envStatusListener).registerSession(refEq(getUserInfo()));
		verify(configuration).isRolePolicyEnabled();
		verify(configuration).getRoleDefaultAccess();
		verifyNoMoreInteractions(envStatusListener, configuration);
	}

	@Test
	public void authorizeWhenStatusIsNotOk() {
		doNothing().when(envStatusListener).registerSession(any(UserInfo.class));
		when(configuration.isRolePolicyEnabled()).thenReturn(true);
		final Response response = resources.getJerseyTest()
				.target("/user/authorize")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json("someUser"));

		assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(envStatusListener, configuration);
	}

	@Test
	public void authorizeWithException() {
		doThrow(new RuntimeException()).when(envStatusListener).registerSession(any(UserInfo.class));
		when(configuration.isRolePolicyEnabled()).thenReturn(true);
		final Response response = resources.getJerseyTest()
				.target("/user/authorize")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(USER));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(envStatusListener).registerSession(refEq(getUserInfo()));
		verifyNoMoreInteractions(envStatusListener);
		verifyZeroInteractions(configuration);
	}

	@Test
	public void userLogoutWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		doNothing().when(envStatusListener).unregisterSession(any(UserInfo.class));
		when(securityService.post(anyString(), anyString(), any())).thenReturn(mock(Response.class));
		final Response response = resources.getJerseyTest()
				.target("/user/logout")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.valueOf("text/html;charset=ISO-8859-1"), response.getMediaType());

		verify(envStatusListener).unregisterSession(refEq(getUserInfo()));
		verify(securityService).post(eq("logout"), eq(TOKEN), eq(Response.class));
		verifyNoMoreInteractions(envStatusListener, securityService);
	}

	@Test
	public void userLogoutWithException() {
		doNothing().when(envStatusListener).unregisterSession(any(UserInfo.class));
		when(securityService.post(anyString(), anyString(), any())).thenReturn(mock(Response.class));
		final Response response = resources.getJerseyTest()
				.target("/user/logout")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.valueOf("text/html;charset=ISO-8859-1"), response.getMediaType());

		verify(envStatusListener).unregisterSession(refEq(getUserInfo()));
		verify(securityService).post(eq("logout"), eq(TOKEN), eq(Response.class));
		verifyNoMoreInteractions(envStatusListener, securityService);
	}

	private UserCredentialDTO getUserCredentialDTO() {
		UserCredentialDTO userCredentialDTO = new UserCredentialDTO();
		userCredentialDTO.setUsername(USER);
		userCredentialDTO.setPassword("somePass");
		userCredentialDTO.setAccessToken(TOKEN);
		return userCredentialDTO;
	}
}
