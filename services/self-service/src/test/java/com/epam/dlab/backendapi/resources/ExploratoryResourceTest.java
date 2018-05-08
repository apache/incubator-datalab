package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.ExploratoryActionFormDTO;
import com.epam.dlab.backendapi.resources.dto.ExploratoryCreateFormDTO;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.exloratory.Exploratory;
import io.dropwizard.auth.*;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ExploratoryResourceTest {

	private final String TOKEN = "TOKEN";
	private final UserInfo userInfo = getUserInfo();
	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private ExploratoryService exploratoryService = mock(ExploratoryService.class);

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
			.addResource(new ExploratoryResource(exploratoryService))
			.build();

	@Test
	public void create() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(exploratoryService.create(any(UserInfo.class), any(Exploratory.class))).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(getExploratoryCreateFormDTO()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).create(userInfo, getExploratory(getExploratoryCreateFormDTO()));
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void createWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Could not create exploratory environment"))
				.when(exploratoryService).create(any(UserInfo.class), any(Exploratory.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(getExploratoryCreateFormDTO()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		String expectedJson = "\"code\":500,\"message\":\"There was an error processing your request. " +
				"It has been logged";
		String actualJson = response.readEntity(String.class);
		assertTrue(actualJson.contains(expectedJson));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).create(userInfo, getExploratory(getExploratoryCreateFormDTO()));
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void start() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(exploratoryService.start(any(UserInfo.class), anyString())).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getExploratoryActionFormDTO()));

		assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
		assertEquals("{\"errors\":[\"notebookInstanceName may not be empty\"]}", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(exploratoryService);
	}

	@Test
	public void stop() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/someName/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).stop(userInfo, "someName");
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void stopWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Could not stop exploratory environment"))
				.when(exploratoryService).stop(any(UserInfo.class), anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/someName/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		String expectedJson = "\"code\":500,\"message\":\"There was an error processing your request. " +
				"It has been logged";
		String actualJson = response.readEntity(String.class);
		assertTrue(actualJson.contains(expectedJson));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).stop(userInfo, "someName");
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void terminate() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/someName/terminate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).terminate(userInfo, "someName");
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void terminateWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Could not terminate exploratory environment"))
				.when(exploratoryService).terminate(any(UserInfo.class), anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/someName/terminate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		String expectedJson = "\"code\":500,\"message\":\"There was an error processing your request. " +
				"It has been logged";
		String actualJson = response.readEntity(String.class);
		assertTrue(actualJson.contains(expectedJson));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).terminate(userInfo, "someName");
		verifyNoMoreInteractions(exploratoryService);
	}

	private UserInfo getUserInfo() {
		return new UserInfo("testUser", TOKEN);
	}

	private ExploratoryCreateFormDTO getExploratoryCreateFormDTO() {
		ExploratoryCreateFormDTO ecfDto = new ExploratoryCreateFormDTO();
		ecfDto.setImage("someImage");
		ecfDto.setTemplateName("someTemplateName");
		ecfDto.setName("someName");
		ecfDto.setShape("someShape");
		ecfDto.setVersion("someVersion");
		ecfDto.setImageName("someImageName");
		return ecfDto;
	}

	private ExploratoryActionFormDTO getExploratoryActionFormDTO() {
		return new ExploratoryActionFormDTO();
	}

	private Exploratory getExploratory(@Valid @NotNull ExploratoryCreateFormDTO formDTO) {
		return Exploratory.builder()
				.name(formDTO.getName())
				.dockerImage(formDTO.getImage())
				.imageName(formDTO.getImageName())
				.templateName(formDTO.getTemplateName())
				.version(formDTO.getVersion())
				.shape(formDTO.getShape()).build();
	}
}
