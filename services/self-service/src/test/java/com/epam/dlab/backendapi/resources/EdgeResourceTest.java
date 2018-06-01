/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.EdgeService;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class EdgeResourceTest extends TestBase {

	private EdgeService edgeService = mock(EdgeService.class);

	@Rule
	public final ResourceTestRule resources = getResourceTestRuleInstance(new EdgeResource(edgeService));

	@Before
	public void setup() throws AuthenticationException {
		authSetup();
	}

	@Test
	public void start() {
		when(edgeService.start(any(UserInfo.class))).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/edge/start")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getUserInfo()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(edgeService).start(getUserInfo());
		verifyNoMoreInteractions(edgeService);
	}

	@Test
	public void startWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(edgeService.start(any(UserInfo.class))).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/edge/start")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getUserInfo()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(edgeService).start(getUserInfo());
		verifyNoMoreInteractions(edgeService);
	}

	@Test
	public void startWithException() {
		when(edgeService.start(any(UserInfo.class))).thenThrow(new DlabException("Could not start edge node"));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/edge/start")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getUserInfo()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		String expectedJson = "\"code\":500,\"message\":\"There was an error processing your request. " +
				"It has been logged";
		String actualJson = response.readEntity(String.class);
		assertTrue(actualJson.contains(expectedJson));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(edgeService).start(getUserInfo());
		verifyNoMoreInteractions(edgeService);
	}

	@Test
	public void stop() {
		when(edgeService.stop(any(UserInfo.class))).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/edge/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getUserInfo()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(edgeService).stop(getUserInfo());
		verifyNoMoreInteractions(edgeService);
	}

	@Test
	public void stopWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(edgeService.stop(any(UserInfo.class))).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/edge/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getUserInfo()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(edgeService).stop(getUserInfo());
		verifyNoMoreInteractions(edgeService);
	}

	@Test
	public void stopWithException() {
		when(edgeService.stop(any(UserInfo.class))).thenThrow(new DlabException("Could not stop edge node"));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure/edge/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getUserInfo()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		String expectedJson = "\"code\":500,\"message\":\"There was an error processing your request. " +
				"It has been logged";
		String actualJson = response.readEntity(String.class);
		assertTrue(actualJson.contains(expectedJson));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(edgeService).stop(getUserInfo());
		verifyNoMoreInteractions(edgeService);
	}

}
