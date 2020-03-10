/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.epam.dlab.backendapi.service.EnvironmentService;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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
		when(environmentService.getUsers()).thenReturn(Collections.singletonList(new UserDTO("activeUser",
				null, UserDTO.Status.ACTIVE)));
		final Response response = resources.getJerseyTest()
				.target("/environment/user")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(Collections.singletonList(new UserDTO("activeUser", null, UserDTO.Status.ACTIVE)),
				response.readEntity(new GenericType<List<UserDTO>>() {
				}));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).getUsers();
		verifyNoMoreInteractions(environmentService);
	}

	@Test
	public void getUsersWithActiveEnvWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(environmentService.getUsers()).thenReturn(Collections.singletonList(new UserDTO("activeUser",
				null, UserDTO.Status.ACTIVE)));
		final Response response = resources.getJerseyTest()
				.target("/environment/user")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(environmentService);
	}

	@Test
	public void getAllEnv() {
		when(environmentService.getAllEnv()).thenReturn(Collections.emptyList());
		final Response response = resources.getJerseyTest()
				.target("/environment/all")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(environmentService).getAllEnv();
		verifyNoMoreInteractions(environmentService);
	}

	@Test
	public void getAllEnvWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(environmentService.getAllEnv()).thenReturn(Collections.emptyList());
		final Response response = resources.getJerseyTest()
				.target("/environment/all")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(environmentService);
	}
}
