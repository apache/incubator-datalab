package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.AccessKeyService;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.exceptions.DlabException;
import io.dropwizard.auth.*;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
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
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class KeyUploaderResourceTest {

	private final String TOKEN = "TOKEN";
	private final String USER = "testUser";
	private final UserInfo userInfo = getUserInfo();
	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private AccessKeyService keyService = mock(AccessKeyService.class);

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
			.addProvider(MultiPartFeature.class)
			.addResource(new KeyUploaderResource(keyService))
			.build();

	@Test
	public void checkKey() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(keyService.getUserKeyStatus(anyString())).thenReturn(KeyLoadStatus.SUCCESS);
		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).getUserKeyStatus(USER.toLowerCase());
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void checkKeyWithErrorStatus() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(keyService.getUserKeyStatus(anyString())).thenReturn(KeyLoadStatus.ERROR);
		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).getUserKeyStatus(USER.toLowerCase());
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void loadKey() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(keyService.uploadKey(any(UserInfo.class), anyString(), anyBoolean())).thenReturn("someUuid");

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "ssh-h;glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).uploadKey(userInfo, "ssh-h;glfh;lgfmhgfmmgfkl", true);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void loadKeyWithWrongKeyFormat() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(keyService.uploadKey(any(UserInfo.class), anyString(), anyBoolean())).thenReturn("someUuid");

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(keyService);
	}

	@Test
	public void loadKeyWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Could not upload the key and create EDGE node"))
				.when(keyService).uploadKey(any(UserInfo.class), anyString(), anyBoolean());

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "ssh-h;glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).uploadKey(userInfo, "ssh-h;glfh;lgfmhgfmmgfkl", true);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void reuploadKey() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(keyService.uploadKey(any(UserInfo.class), anyString(), anyBoolean())).thenReturn("someUuid");

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "ssh-h;glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.queryParam("is_primary_uploading", "false")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).uploadKey(userInfo, "ssh-h;glfh;lgfmhgfmmgfkl", false);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void reuploadKeyWithWrongKeyFormat() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(keyService.uploadKey(any(UserInfo.class), anyString(), anyBoolean())).thenReturn("someUuid");

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.queryParam("is_primary_uploading", "false")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(keyService);
	}

	@Test
	public void reuploadKeyWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Could not reupload the key. Previous key has been deleted"))
				.when(keyService).uploadKey(any(UserInfo.class), anyString(), anyBoolean());

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "ssh-h;glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.queryParam("is_primary_uploading", "false")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).uploadKey(userInfo, "ssh-h;glfh;lgfmhgfmmgfkl", false);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void recoverEdge() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(keyService.recoverEdge(any(UserInfo.class))).thenReturn("someUuid");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key/recover")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).recoverEdge(userInfo);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void recoverEdgeWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Could not upload the key and create EDGE node"))
				.when(keyService).recoverEdge(any(UserInfo.class));

		final Response response = resources.getJerseyTest()
				.target("/user/access_key/recover")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).recoverEdge(userInfo);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void generateKey() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		when(keyService.generateKey(any(UserInfo.class))).thenReturn("someUuid");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key/generate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).generateKey(userInfo);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void generateKeyWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new DlabException("Can not generate private/public key pair due to"))
				.when(keyService).generateKey(any(UserInfo.class));

		final Response response = resources.getJerseyTest()
				.target("/user/access_key/generate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).generateKey(userInfo);
		verifyNoMoreInteractions(keyService);
	}


	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}
}
