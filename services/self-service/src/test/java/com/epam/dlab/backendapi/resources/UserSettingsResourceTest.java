package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.UserSettingsDAO;
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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.*;

public class UserSettingsResourceTest {

	private UserSettingsDAO userSettingsDAO = mock(UserSettingsDAO.class);

	@Rule
	public final ResourceTestRule resources =
			TestHelper.getResourceTestRuleInstance(new UserSettingsResource(userSettingsDAO));

	@Before
	public void setup() throws AuthenticationException {
		TestHelper.authSetup();
	}

	@Test
	public void getSettings() {
		when(userSettingsDAO.getUISettings(any(UserInfo.class))).thenReturn("someSettings");
		final Response response = resources.getJerseyTest()
				.target("/user/settings")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someSettings", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(userSettingsDAO).getUISettings(refEq(TestHelper.getUserInfo()));
		verifyNoMoreInteractions(userSettingsDAO);
	}

	@Test
	public void getSettingsWithFailedAuth() throws AuthenticationException {
		TestHelper.authFailSetup();
		when(userSettingsDAO.getUISettings(any(UserInfo.class))).thenReturn("someSettings");
		final Response response = resources.getJerseyTest()
				.target("/user/settings")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someSettings", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(userSettingsDAO).getUISettings(refEq(TestHelper.getUserInfo()));
		verifyNoMoreInteractions(userSettingsDAO);
	}

	@Test
	public void saveSettings() {
		doNothing().when(userSettingsDAO).setUISettings(any(UserInfo.class), anyString());
		final Response response = resources.getJerseyTest()
				.target("/user/settings")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.json("someSettings"));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(userSettingsDAO).setUISettings(refEq(TestHelper.getUserInfo()), eq("someSettings"));
		verifyNoMoreInteractions(userSettingsDAO);
	}

	@Test
	public void saveSettingsWithFailedAuth() throws AuthenticationException {
		TestHelper.authFailSetup();
		doNothing().when(userSettingsDAO).setUISettings(any(UserInfo.class), anyString());
		final Response response = resources.getJerseyTest()
				.target("/user/settings")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.json("someSettings"));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(userSettingsDAO).setUISettings(refEq(TestHelper.getUserInfo()), eq("someSettings"));
		verifyNoMoreInteractions(userSettingsDAO);
	}

	@Test
	public void saveSettingsWithException() {
		doThrow(new RuntimeException()).when(userSettingsDAO).setUISettings(any(UserInfo.class), anyString());
		final Response response = resources.getJerseyTest()
				.target("/user/settings")
				.request()
				.header("Authorization", "Bearer " + TestHelper.TOKEN)
				.post(Entity.json("someSettings"));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertTrue(response.readEntity(String.class).contains("{\"code\":500,\"message\":\"There was an error " +
				"processing your request. It has been logged"));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(userSettingsDAO).setUISettings(refEq(TestHelper.getUserInfo()), eq("someSettings"));
		verifyNoMoreInteractions(userSettingsDAO);
	}
}
