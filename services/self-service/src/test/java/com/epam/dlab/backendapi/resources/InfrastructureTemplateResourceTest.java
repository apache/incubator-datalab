package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.InfrastructureTemplateService;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.exceptions.DlabException;
import io.dropwizard.auth.*;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class InfrastructureTemplateResourceTest {

	private final String TOKEN = "TOKEN";
	private final UserInfo userInfo = getUserInfo();
	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private InfrastructureTemplateService infrastructureTemplateService = mock(InfrastructureTemplateService.class);

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
			.addResource(new InfrastructureTemplateResource(infrastructureTemplateService))
			.build();

	@Test
	public void getComputationalTemplates() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		FullComputationalTemplate fullComputationalTemplate =
				new FullComputationalTemplate(new ComputationalMetadataDTO());
		when(infrastructureTemplateService.getComputationalTemplates(any(UserInfo.class)))
				.thenReturn(Collections.singletonList(fullComputationalTemplate));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_templates/computational_templates")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureTemplateService).getComputationalTemplates(userInfo);
		verifyNoMoreInteractions(infrastructureTemplateService);
	}

	@Test
	public void getComputationalTemplatesWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Could not load list of computational templates for user"))
				.when(infrastructureTemplateService).getComputationalTemplates(any(UserInfo.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_templates/computational_templates")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureTemplateService).getComputationalTemplates(userInfo);
		verifyNoMoreInteractions(infrastructureTemplateService);
	}

	@Test
	public void getExploratoryTemplates() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		ExploratoryMetadataDTO exploratoryMetadataDTO =
				new ExploratoryMetadataDTO("someImageName");
		when(infrastructureTemplateService.getExploratoryTemplates(any(UserInfo.class)))
				.thenReturn(Collections.singletonList(exploratoryMetadataDTO));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_templates/exploratory_templates")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(Collections.singletonList(exploratoryMetadataDTO),
				response.readEntity(new GenericType<List<ExploratoryMetadataDTO>>() {
				}));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureTemplateService).getExploratoryTemplates(userInfo);
		verifyNoMoreInteractions(infrastructureTemplateService);
	}

	@Test
	public void getExploratoryTemplatesWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Could not load list of exploratory templates for user"))
				.when(infrastructureTemplateService).getExploratoryTemplates(any(UserInfo.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_templates/exploratory_templates")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureTemplateService).getExploratoryTemplates(userInfo);
		verifyNoMoreInteractions(infrastructureTemplateService);
	}

	private UserInfo getUserInfo() {
		return new UserInfo("testUser", TOKEN);
	}
}
