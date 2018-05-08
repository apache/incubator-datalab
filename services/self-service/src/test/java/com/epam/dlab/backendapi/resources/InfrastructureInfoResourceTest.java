package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.backendapi.resources.dto.InfrastructureInfo;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.exceptions.DlabException;
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
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class InfrastructureInfoResourceTest {

	private final String TOKEN = "TOKEN";
	private final String USER = "testUser";
	private final UserInfo userInfo = getUserInfo();
	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private InfrastructureInfoService infrastructureInfoService = mock(InfrastructureInfoService.class);

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
			.addResource(new InfrastructureInfoResource(infrastructureInfoService))
			.build();

	@Test
	public void status() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		final Response response = resources.getJerseyTest()
				.target("/infrastructure")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(infrastructureInfoService);
	}

	@Test
	public void healthStatus() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		HealthStatusPageDTO hspDto = getHealthStatusPageDTO();
		when(infrastructureInfoService.getHeathStatus(anyString(), anyBoolean(), anyBoolean())).thenReturn(hspDto);
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/status")
				.queryParam("full", "1")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(hspDto.getStatus(), response.readEntity(HealthStatusPageDTO.class).getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureInfoService).getHeathStatus(USER.toLowerCase(), true, true);
		verifyNoMoreInteractions(infrastructureInfoService);
	}

	@Test
	public void healthStatusWithDefaultQueryParam() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		HealthStatusPageDTO hspDto = getHealthStatusPageDTO();
		when(infrastructureInfoService.getHeathStatus(anyString(), anyBoolean(), anyBoolean())).thenReturn(hspDto);
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/status")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(hspDto.getStatus(), response.readEntity(HealthStatusPageDTO.class).getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureInfoService).getHeathStatus(USER.toLowerCase(), false, true);
		verifyNoMoreInteractions(infrastructureInfoService);
	}

	@Test
	public void healthStatusWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Could not return status of resources for user"))
				.when(infrastructureInfoService).getHeathStatus(anyString(), anyBoolean(), anyBoolean());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/status")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureInfoService).getHeathStatus(USER.toLowerCase(), false, true);
		verifyNoMoreInteractions(infrastructureInfoService);
	}

	@Test
	public void getUserResources() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		InfrastructureInfo info = getInfrastructureInfo();
		when(infrastructureInfoService.getUserResources(anyString())).thenReturn(info);
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/info")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(info.toString(), response.readEntity(InfrastructureInfo.class).toString());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureInfoService).getUserResources(USER.toLowerCase());
		verifyNoMoreInteractions(infrastructureInfoService);
	}

	@Test
	public void getUserResourcesWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Could not load list of provisioned resources for user"))
				.when(infrastructureInfoService).getUserResources(anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/info")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureInfoService).getUserResources(USER.toLowerCase());
		verifyNoMoreInteractions(infrastructureInfoService);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private HealthStatusPageDTO getHealthStatusPageDTO() {
		HealthStatusPageDTO hspdto = new HealthStatusPageDTO();
		hspdto.setStatus("someStatus");
		return hspdto;
	}

	private InfrastructureInfo getInfrastructureInfo() {
		return new InfrastructureInfo(Collections.emptyMap(), Collections.emptyList());
	}
}
