package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.*;
import com.epam.dlab.backendapi.service.LibraryService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import io.dropwizard.auth.*;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.bson.Document;
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
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class LibExploratoryResourceTest {

	private final String TOKEN = "TOKEN";
	private final String USER = "testUser";
	private final UserInfo userInfo = getUserInfo();
	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private ExploratoryDAO exploratoryDAO = mock(ExploratoryDAO.class);
	private ExploratoryLibDAO libraryDAO = mock(ExploratoryLibDAO.class);
	private LibraryService libraryService = mock(LibraryService.class);
	private RESTService provisioningService = mock(RESTService.class);
	private RequestId requestId = mock(RequestId.class);

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
			.addResource(new LibExploratoryResource(exploratoryDAO, libraryDAO, libraryService, provisioningService,
					requestId))
			.build();

	@Test
	public void getLibGroupListWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn
				(getUserInstanceDto());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/lib_groups")
				.queryParam("exploratory_name", "explName")
				.queryParam("computational_name", "compName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryDAO).fetchExploratoryFields(USER.toLowerCase(), "explName", "compName");
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void getLibGroupListWithoutComputationalWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(getUserInstanceDto());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/lib_groups")
				.queryParam("exploratory_name", "explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryDAO).fetchExploratoryFields(USER.toLowerCase(), "explName");
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void getLibList() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(libraryService.getLibs(anyString(), anyString(), anyString())).thenReturn(getDocuments());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/lib_list")
				.queryParam("exploratory_name", "explName")
				.queryParam("computational_name", "compName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(getDocuments(), response.readEntity(new GenericType<List<Document>>() {
		}));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(libraryService).getLibs(USER.toLowerCase(), "explName", "compName");
		verifyNoMoreInteractions(libraryService);
	}

	@Test
	public void getLibListWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Cannot load installed libraries"))
				.when(libraryService).getLibs(anyString(), anyString(), anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/lib_list")
				.queryParam("exploratory_name", "explName")
				.queryParam("computational_name", "compName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(libraryService).getLibs(USER.toLowerCase(), "explName", "compName");
		verifyNoMoreInteractions(libraryService);
	}

	@Test
	public void getLibListFormatted() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(libraryService.getLibInfo(anyString(), anyString())).thenReturn(getLibInfoRecords());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/lib_list/formatted")
				.queryParam("exploratory_name", "explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(libraryService).getLibInfo(USER.toLowerCase(), "explName");
		verifyNoMoreInteractions(libraryService);
	}

	@Test
	public void getLibListFormattedWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Cannot load  formatted list of installed libraries"))
				.when(libraryService).getLibInfo(anyString(), anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/lib_list/formatted")
				.queryParam("exploratory_name", "explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(libraryService).getLibInfo(USER.toLowerCase(), "explName");
		verifyNoMoreInteractions(libraryService);
	}

	@Test
	public void libInstall() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(libraryService.generateLibraryInstallDTO(any(UserInfo.class), any(LibInstallFormDTO.class)))
				.thenReturn(getLibraryInstallDTO());
		when(libraryService.prepareComputationalLibInstallation(anyString(), any(LibInstallFormDTO.class),
				any(LibraryInstallDTO.class))).thenReturn(getLibraryInstallDTO());
		when(provisioningService.post(anyString(), anyString(), any(LibraryInstallDTO.class), any()))
				.thenReturn("someUuid");
		when(requestId.put(anyString(), anyString())).thenReturn("someUuid");
		LibInstallFormDTO libInstallFormDTO = new LibInstallFormDTO();
		libInstallFormDTO.setComputationalName("compName");
		libInstallFormDTO.setNotebookName("explName");
		libInstallFormDTO.setLibs(Collections.emptyList());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/lib_install")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(libInstallFormDTO));

		assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(libraryService, provisioningService, requestId);
	}


	@Test
	public void libInstallWithoutComputational() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(libraryService.generateLibraryInstallDTO(any(UserInfo.class), any(LibInstallFormDTO.class)))
				.thenReturn(getLibraryInstallDTO());
		when(libraryService.prepareExploratoryLibInstallation(anyString(), any(LibInstallFormDTO.class),
				any(LibraryInstallDTO.class))).thenReturn(getLibraryInstallDTO());
		when(provisioningService.post(anyString(), anyString(), any(LibraryInstallDTO.class), any()))
				.thenReturn("someUuid");
		when(requestId.put(anyString(), anyString())).thenReturn("someUuid");
		LibInstallFormDTO libInstallFormDTO = new LibInstallFormDTO();
		libInstallFormDTO.setComputationalName("");
		libInstallFormDTO.setNotebookName("explName");
		libInstallFormDTO.setLibs(Collections.emptyList());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/lib_install")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(libInstallFormDTO));

		assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(libraryService, provisioningService, requestId);
	}

	@Test
	public void getLibraryListWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString()))
				.thenReturn(getUserInstanceDto());
		SearchLibsFormDTO searchLibsFormDTO = new SearchLibsFormDTO();
		searchLibsFormDTO.setComputationalName("compName");
		searchLibsFormDTO.setNotebookName("explName");
		searchLibsFormDTO.setGroup("someGroup");
		searchLibsFormDTO.setStartWith("someText");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/search/lib_list")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(searchLibsFormDTO));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryDAO).fetchExploratoryFields(USER.toLowerCase(), "explName", "compName");
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void getLibraryListWithoutComputationalWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString()))
				.thenReturn(getUserInstanceDto());
		SearchLibsFormDTO searchLibsFormDTO = new SearchLibsFormDTO();
		searchLibsFormDTO.setComputationalName("");
		searchLibsFormDTO.setNotebookName("explName");
		searchLibsFormDTO.setGroup("someGroup");
		searchLibsFormDTO.setStartWith("someText");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/search/lib_list")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(searchLibsFormDTO));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryDAO).fetchExploratoryFields(USER.toLowerCase(), "explName");
		verifyNoMoreInteractions(exploratoryDAO);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private UserInstanceDTO getUserInstanceDto() {
		UserComputationalResource ucResource = new UserComputationalResource();
		ucResource.setComputationalName("compName");
		return new UserInstanceDTO().withUser(USER).withExploratoryName("explName")
				.withResources(Collections.singletonList(ucResource));
	}

	private List<Document> getDocuments() {
		return Collections.singletonList(new Document());
	}

	private List<LibInfoRecord> getLibInfoRecords() {
		return Collections.singletonList(new LibInfoRecord(
				new LibKey(), Collections.singletonList(new LibraryStatus())));
	}

	private LibraryInstallDTO getLibraryInstallDTO() {
		return new LibraryInstallDTO().withComputationalName("compName");
	}
}
