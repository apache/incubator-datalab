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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.ExploratoryActionFormDTO;
import com.epam.dlab.backendapi.resources.dto.ExploratoryCreateFormDTO;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.dto.aws.computational.ClusterConfig;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.exploratory.Exploratory;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ExploratoryResourceTest extends TestBase {

	private ExploratoryService exploratoryService = mock(ExploratoryService.class);

	@Rule
	public final ResourceTestRule resources = getResourceTestRuleInstance(new ExploratoryResource(exploratoryService));

	@Before
	public void setup() throws AuthenticationException {
		authSetup();
	}

	@Test
	public void create() {
		when(exploratoryService.create(any(UserInfo.class), any(Exploratory.class), anyString())).thenReturn(
				"someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(getExploratoryCreateFormDTO()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).create(refEq(getUserInfo()), refEq(getExploratory(getExploratoryCreateFormDTO())),
				eq("project"));
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void createWithException() {
		doThrow(new DlabException("Could not create exploratory environment"))
				.when(exploratoryService).create(any(UserInfo.class), any(Exploratory.class), anyString());
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

		verify(exploratoryService).create(getUserInfo(), getExploratory(getExploratoryCreateFormDTO()), "project");
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void start() {
		when(exploratoryService.start(any(UserInfo.class), anyString(), anyString())).thenReturn("someUuid");
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
	public void startWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(exploratoryService.start(any(UserInfo.class), anyString(), anyString())).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getExploratoryActionFormDTO()));

		assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verifyZeroInteractions(exploratoryService);
	}

	@Test
	public void stop() {
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/someName/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).stop(getUserInfo(), "someName");
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void stopWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/someName/stop")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).stop(getUserInfo(), "someName");
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void stopWithException() {
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

		verify(exploratoryService).stop(getUserInfo(), "someName");
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void terminate() {
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/someName/terminate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).terminate(getUserInfo(), "someName");
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void terminateWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/someName/terminate")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals("someUuid", response.readEntity(String.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(exploratoryService).terminate(getUserInfo(), "someName");
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void terminateWithException() {
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

		verify(exploratoryService).terminate(getUserInfo(), "someName");
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void updateSparkConfig() {
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/someName/reconfigure")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(Collections.singletonList(new ClusterConfig())));

		assertEquals(HttpStatus.SC_OK, response.getStatus());

		verify(exploratoryService).updateClusterConfig(refEq(getUserInfo()), eq("someName"),
				eq(Collections.singletonList(new ClusterConfig())));
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void getSparkConfig() {
		final ClusterConfig config = new ClusterConfig();
		config.setClassification("test");
		when(exploratoryService.getClusterConfig(any(UserInfo.class), anyString())).thenReturn(Collections.singletonList(config));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/someName/cluster/config")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		final List<ClusterConfig> clusterConfigs = response.readEntity(new GenericType<List<ClusterConfig>>() {
		});
		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(1, clusterConfigs.size());
		assertEquals("test", clusterConfigs.get(0).getClassification());

		verify(exploratoryService).getClusterConfig(refEq(getUserInfo()), eq("someName"));
		verifyNoMoreInteractions(exploratoryService);
	}

	private ExploratoryCreateFormDTO getExploratoryCreateFormDTO() {
		ExploratoryCreateFormDTO ecfDto = new ExploratoryCreateFormDTO();
		ecfDto.setImage("someImage");
		ecfDto.setTemplateName("someTemplateName");
		ecfDto.setName("someName");
		ecfDto.setShape("someShape");
		ecfDto.setVersion("someVersion");
		ecfDto.setImageName("someImageName");
		ecfDto.setProject("project");
		ecfDto.setEndpoint("endpoint");
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
				.shape(formDTO.getShape())
				.endpoint(formDTO.getEndpoint())
				.project(formDTO.getProject()).build();
	}
}
