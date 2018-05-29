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

import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.exceptions.ResourceConflictException;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class EnvironmentResourceTest extends TestBase {

	private EnvironmentService environmentService = mock(EnvironmentService.class);

	@Rule
	public final ResourceTestRule resources = getResourceTestRuleInstance(new EnvironmentResource(environmentService));

	@Before
	public void setup() throws AuthenticationException {
		authSetup();
	}

	@Test
	public void getUsersWithActiveEnv() {
		when(environmentService.getActiveUsers()).thenReturn(Collections.singleton("activeUser"));
		final Response response = resources.getJerseyTest()
				.target("/environment/user/active")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(Collections.singleton("activeUser"), response.readEntity(new GenericType<Set<String>>() {
		}));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).getActiveUsers();
		verifyNoMoreInteractions(environmentService);
	}

	@Test
	public void getUsersWithActiveEnvWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(environmentService.getActiveUsers()).thenReturn(Collections.singleton("activeUser"));
		final Response response = resources.getJerseyTest()
				.target("/environment/user/active")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(environmentService);
	}

	@Test
	public void terminateEnv() {
		doNothing().when(environmentService).terminateEnvironment(anyString());
		final Response response = resources.getJerseyTest()
				.target("/environment/terminate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.text(USER));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).terminateEnvironment(USER);
		verifyNoMoreInteractions(environmentService);
	}

	@Test
	public void terminateEnvWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		doNothing().when(environmentService).terminateEnvironment(anyString());
		final Response response = resources.getJerseyTest()
				.target("/environment/terminate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.text(USER));

		assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(environmentService);
	}

	@Test
	public void terminateEnvWithResourceConflictException() {
		doThrow(new ResourceConflictException("Can not terminate environment because on of user resource is in " +
				"status" +
				" CREATING or STARTING")).when(environmentService).terminateEnvironment(anyString());
		final Response response = resources.getJerseyTest()
				.target("/environment/terminate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.text(USER));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).terminateEnvironment(USER);
		verifyNoMoreInteractions(environmentService);
	}

	@Test
	public void stopEnv() {
		doNothing().when(environmentService).stopEnvironment(anyString());
		final Response response = resources.getJerseyTest()
				.target("/environment/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.text(USER));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).stopEnvironment(USER);
		verifyNoMoreInteractions(environmentService);
	}

	@Test
	public void stopEnvWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		doNothing().when(environmentService).stopEnvironment(anyString());
		final Response response = resources.getJerseyTest()
				.target("/environment/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.text(USER));

		assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(environmentService);
	}

	@Test
	public void stopEnvWithResourceConflictException() {
		doThrow(new ResourceConflictException("Can not stop environment because on of user resource is in status" +
				" CREATING or STARTING")).when(environmentService).stopEnvironment(anyString());
		final Response response = resources.getJerseyTest()
				.target("/environment/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.text(USER));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).stopEnvironment(USER);
		verifyNoMoreInteractions(environmentService);
	}
}
