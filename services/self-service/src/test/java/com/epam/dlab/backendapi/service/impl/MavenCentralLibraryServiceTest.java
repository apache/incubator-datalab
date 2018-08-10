/*
 * **************************************************************************
 *
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
 *
 * ***************************************************************************
 */

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.domain.MavenSearchArtifactResponse;
import com.epam.dlab.backendapi.resources.dto.LibraryDTO;
import com.epam.dlab.exceptions.DlabException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MavenCentralLibraryServiceTest {

	@Mock
	private Client client;
	@InjectMocks
	private MavenCentralLibraryService mavenCentralLibraryService;
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getLibraryId() {
		final WebTarget webTarget = mock(WebTarget.class);
		final Invocation.Builder requestBuilder = mock(Invocation.Builder.class);
		final Response response = mock(Response.class);
		final Response.StatusType statusType = mock(Response.StatusType.class);
		when(client.target(any(URI.class))).thenReturn(webTarget);
		when(webTarget.request()).thenReturn(requestBuilder);
		when(requestBuilder.get()).thenReturn(response);
		when(response.getStatusInfo()).thenReturn(statusType);
		when(statusType.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
		when(response.readEntity(MavenSearchArtifactResponse.class)).thenReturn(getMavenResponse());

		final LibraryDTO libDTO = mavenCentralLibraryService.getLibrary("groupId", "artifactId", "version");

		assertNotNull(libDTO);
		assertEquals("groupId:artifactId", libDTO.getName());
		assertEquals("version", libDTO.getVersion());

		verify(client).target(refEq(URI.create("http://search.maven.org/solrsearch/select?q=a:%22artifactId%22+AND+g" +
				":%22groupId%22+AND+v:%22version%22+AND+p:%22jar%22&rows=20&wt=json&core=gav&p=jar")));
		verify(webTarget).request();
		verify(response).readEntity(MavenSearchArtifactResponse.class);
	}

	@Test
	public void getLibraryIdWithException() {
		final WebTarget webTarget = mock(WebTarget.class);
		final Invocation.Builder requestBuilder = mock(Invocation.Builder.class);
		final Response response = mock(Response.class);
		final Response.StatusType statusType = mock(Response.StatusType.class);
		when(client.target(any(URI.class))).thenReturn(webTarget);
		when(webTarget.request()).thenReturn(requestBuilder);
		when(requestBuilder.get()).thenReturn(response);
		when(response.getStatusInfo()).thenReturn(statusType);
		when(statusType.getStatusCode()).thenReturn(500);
		when(statusType.getReasonPhrase()).thenReturn("Exception");
		when(response.readEntity(MavenSearchArtifactResponse.class)).thenReturn(getMavenResponse());

		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Can not get artifact info from maven central due to: Exception");

		mavenCentralLibraryService.getLibrary("groupId", "artifactId", "version");
	}

	private MavenSearchArtifactResponse getMavenResponse() {
		final MavenSearchArtifactResponse response = new MavenSearchArtifactResponse();
		MavenSearchArtifactResponse.Response.Artifact artifact = new MavenSearchArtifactResponse.Response.Artifact();
		artifact.setId("test.group:artifact:1.0");
		artifact.setVersion("1.0");
		final MavenSearchArtifactResponse.Response rsp = new MavenSearchArtifactResponse.Response();
		rsp.setArtifacts(Collections.singletonList(artifact));
		response.setResponse(rsp);
		return response;
	}
}