package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.AccessKeyService;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.exceptions.DlabException;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class KeyUploaderResourceTest {

	private AccessKeyService keyService = mock(AccessKeyService.class);

	@Rule
	public final ResourceTestRule resources =
			TestHelper.getResourceTestRuleInstance(new KeyUploaderResource(keyService));

	@Before
	public void setup() throws AuthenticationException {
		TestHelper.authSetup();
	}

	@Test
	public void checkKey() {
		when(keyService.getUserKeyStatus(anyString())).thenReturn(KeyLoadStatus.SUCCESS);
		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).getUserKeyStatus(TestHelper.USER.toLowerCase());
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void checkKeyWithFailedAuth() throws AuthenticationException {
		TestHelper.authFailSetup();
		when(keyService.getUserKeyStatus(anyString())).thenReturn(KeyLoadStatus.SUCCESS);
		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).getUserKeyStatus(TestHelper.USER.toLowerCase());
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void checkKeyWithErrorStatus() {
		when(keyService.getUserKeyStatus(anyString())).thenReturn(KeyLoadStatus.ERROR);
		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).getUserKeyStatus(TestHelper.USER.toLowerCase());
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void loadKey() {
		when(keyService.uploadKey(any(UserInfo.class), anyString(), anyBoolean())).thenReturn("someUuid");

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "ssh-h;glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).uploadKey(TestHelper.getUserInfo(), "ssh-h;glfh;lgfmhgfmmgfkl", true);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void loadKeyWithFailedAuth() throws AuthenticationException {
		TestHelper.authFailSetup();
		when(keyService.uploadKey(any(UserInfo.class), anyString(), anyBoolean())).thenReturn("someUuid");

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "ssh-h;glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).uploadKey(TestHelper.getUserInfo(), "ssh-h;glfh;lgfmhgfmmgfkl", true);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void loadKeyWithWrongKeyFormat() {
		when(keyService.uploadKey(any(UserInfo.class), anyString(), anyBoolean())).thenReturn("someUuid");

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(keyService);
	}

	@Test
	public void loadKeyWithException() {
		doThrow(new DlabException("Could not upload the key and create EDGE node"))
				.when(keyService).uploadKey(any(UserInfo.class), anyString(), anyBoolean());

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "ssh-h;glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).uploadKey(TestHelper.getUserInfo(), "ssh-h;glfh;lgfmhgfmmgfkl", true);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void reuploadKey() {
		when(keyService.uploadKey(any(UserInfo.class), anyString(), anyBoolean())).thenReturn("someUuid");

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "ssh-h;glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.queryParam("is_primary_uploading", "false")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).uploadKey(TestHelper.getUserInfo(), "ssh-h;glfh;lgfmhgfmmgfkl", false);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void reuploadKeyWithFailedAuth() throws AuthenticationException {
		TestHelper.authFailSetup();
		when(keyService.uploadKey(any(UserInfo.class), anyString(), anyBoolean())).thenReturn("someUuid");

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "ssh-h;glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.queryParam("is_primary_uploading", "false")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).uploadKey(TestHelper.getUserInfo(), "ssh-h;glfh;lgfmhgfmmgfkl", false);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void reuploadKeyWithWrongKeyFormat() {
		when(keyService.uploadKey(any(UserInfo.class), anyString(), anyBoolean())).thenReturn("someUuid");

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.queryParam("is_primary_uploading", "false")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(keyService);
	}

	@Test
	public void reuploadKeyWithException() {
		doThrow(new DlabException("Could not reupload the key. Previous key has been deleted"))
				.when(keyService).uploadKey(any(UserInfo.class), anyString(), anyBoolean());

		FormDataMultiPart multiPart = new FormDataMultiPart()
				.field("file", "ssh-h;glfh;lgfmhgfmmgfkl");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key")
				.queryParam("is_primary_uploading", "false")
				.register(MultiPartFeature.class)
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.entity(multiPart, multiPart.getMediaType()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).uploadKey(TestHelper.getUserInfo(), "ssh-h;glfh;lgfmhgfmmgfkl", false);
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void recoverEdge() {
		when(keyService.recoverEdge(any(UserInfo.class))).thenReturn("someUuid");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key/recover")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).recoverEdge(TestHelper.getUserInfo());
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void recoverEdgeWithFailedAuth() throws AuthenticationException {
		TestHelper.authFailSetup();
		when(keyService.recoverEdge(any(UserInfo.class))).thenReturn("someUuid");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key/recover")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).recoverEdge(TestHelper.getUserInfo());
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void recoverEdgeWithException() {
		doThrow(new DlabException("Could not upload the key and create EDGE node"))
				.when(keyService).recoverEdge(any(UserInfo.class));

		final Response response = resources.getJerseyTest()
				.target("/user/access_key/recover")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).recoverEdge(TestHelper.getUserInfo());
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void generateKey() {
		when(keyService.generateKey(any(UserInfo.class))).thenReturn("someUuid");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key/generate")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).generateKey(TestHelper.getUserInfo());
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void generateKeyWithFailedAuth() throws AuthenticationException {
		TestHelper.authFailSetup();
		when(keyService.generateKey(any(UserInfo.class))).thenReturn("someUuid");

		final Response response = resources.getJerseyTest()
				.target("/user/access_key/generate")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).generateKey(TestHelper.getUserInfo());
		verifyNoMoreInteractions(keyService);
	}

	@Test
	public void generateKeyWithException() {
		doThrow(new DlabException("Can not generate private/public key pair due to"))
				.when(keyService).generateKey(any(UserInfo.class));

		final Response response = resources.getJerseyTest()
				.target("/user/access_key/generate")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.json(""));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(keyService).generateKey(TestHelper.getUserInfo());
		verifyNoMoreInteractions(keyService);
	}

}
