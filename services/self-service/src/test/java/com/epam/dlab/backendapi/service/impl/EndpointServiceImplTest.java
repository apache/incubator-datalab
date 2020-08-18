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

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.dao.EndpointDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.UserRoleDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.EndpointResourcesDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.resources.TestBase;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.client.RESTService;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EndpointServiceImplTest extends TestBase {
	private static final String HEALTH_CHECK = "healthcheck";
	private static final String EXPLORATORY_NAME_1 = "expName1";
	private static final String EXPLORATORY_NAME_2 = "expName2";
	private static final String PROJECT_NAME_1 = "projectName";
	private static final String PROJECT_NAME_2 = "projectName_2";

	@Mock
	private EndpointDAO endpointDAO;
	@Mock
	private ProjectService projectService;
	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private RESTService provisioningService;
	@Mock
	private UserRoleDAO userRoleDao;

	@InjectMocks
	private EndpointServiceImpl endpointService;

	@Test
	public void getEndpoints() {
		List<EndpointDTO> endpoints = getEndpointDTOs();
		when(endpointDAO.getEndpoints()).thenReturn(endpoints);

		List<EndpointDTO> actualEndpoints = endpointService.getEndpoints();

		assertEquals("lists should be equal", endpoints, actualEndpoints);
		verify(endpointDAO).getEndpoints();
		verifyNoMoreInteractions(endpointDAO);
	}

	@Test
	public void getEndpointsWithStatus() {
		List<EndpointDTO> endpoints = Collections.singletonList(getEndpointDTO());
		when(endpointDAO.getEndpointsWithStatus(anyString())).thenReturn(endpoints);

		List<EndpointDTO> actualEndpoints = endpointService.getEndpointsWithStatus(EndpointDTO.EndpointStatus.ACTIVE);

		assertEquals("lists should be equal", endpoints, actualEndpoints);
		verify(endpointDAO).getEndpointsWithStatus(EndpointDTO.EndpointStatus.ACTIVE.toString());
		verifyNoMoreInteractions(endpointDAO);
	}

	@Test
	public void getEndpointResources() {
		List<UserInstanceDTO> userInstances = getUserInstances();
		List<ProjectDTO> projectDTOs = getProjectDTOs();
		when(exploratoryDAO.fetchExploratoriesByEndpointWhereStatusNotIn(anyString(), anyListOf(UserInstanceStatus.class)))
				.thenReturn(userInstances);
		when(projectService.getProjectsByEndpoint(anyString())).thenReturn(projectDTOs);

		EndpointResourcesDTO actualEndpointResources = endpointService.getEndpointResources(ENDPOINT_NAME);

		assertEquals("objects should be equal", new EndpointResourcesDTO(userInstances, projectDTOs), actualEndpointResources);
		verify(exploratoryDAO).fetchExploratoriesByEndpointWhereStatusNotIn(ENDPOINT_NAME, Arrays.asList(UserInstanceStatus.TERMINATED, UserInstanceStatus.FAILED));
		verify(projectService).getProjectsByEndpoint(ENDPOINT_NAME);
		verifyNoMoreInteractions(exploratoryDAO, projectService);
	}

	@Test
	public void get() {
		EndpointDTO endpointDTO = getEndpointDTO();
		when(endpointDAO.get(anyString())).thenReturn(Optional.of(endpointDTO));

		EndpointDTO actualEndpointDTO = endpointService.get(ENDPOINT_NAME);

		assertEquals("objects should be equal", endpointDTO, actualEndpointDTO);
		verify(endpointDAO).get(ENDPOINT_NAME);
		verifyNoMoreInteractions(endpointDAO);
	}

	@Test(expected = ResourceNotFoundException.class)
	public void getWithException() {
		when(endpointDAO.get(anyString())).thenReturn(Optional.empty());

		endpointService.get(ENDPOINT_NAME);
	}

	@Test
	public void create() {
		Response response = mock(Response.class);
		when(endpointDAO.get(anyString())).thenReturn(Optional.empty());
		when(endpointDAO.getEndpointWithUrl(anyString())).thenReturn(Optional.empty());
		when(provisioningService.get(anyString(), anyString(), any(Class.class))).thenReturn(response);
		when(response.readEntity(any(Class.class))).thenReturn(CloudProvider.AWS);
		when(response.getStatus()).thenReturn(HttpStatus.SC_OK);

		endpointService.create(getUserInfo(), ENDPOINT_NAME, getEndpointDTO());

		verify(endpointDAO).get(ENDPOINT_NAME);
		verify(endpointDAO).getEndpointWithUrl(ENDPOINT_URL);
		verify(provisioningService).get(ENDPOINT_URL + HEALTH_CHECK, TOKEN, Response.class);
		verify(endpointDAO).create(getEndpointDTO());
		verify(userRoleDao).updateMissingRoles(CloudProvider.AWS);
		verifyNoMoreInteractions(endpointDAO, provisioningService, userRoleDao);
	}

	@Test(expected = ResourceConflictException.class)
	public void createWithException1() {
		when(endpointDAO.get(anyString())).thenReturn(Optional.of(getEndpointDTO()));

		endpointService.create(getUserInfo(), ENDPOINT_NAME, getEndpointDTO());
	}

	@Test(expected = ResourceConflictException.class)
	public void createWithException2() {
		when(endpointDAO.get(anyString())).thenReturn(Optional.empty());
		when(endpointDAO.getEndpointWithUrl(anyString())).thenReturn(Optional.of(getEndpointDTO()));

		endpointService.create(getUserInfo(), ENDPOINT_NAME, getEndpointDTO());
	}

	@Test(expected = DlabException.class)
	public void createWithException3() {
		when(endpointDAO.get(anyString())).thenReturn(Optional.empty());
		when(endpointDAO.getEndpointWithUrl(anyString())).thenReturn(Optional.empty());
		when(provisioningService.get(anyString(), anyString(), any(Class.class))).thenThrow(new DlabException("Exception message"));

		endpointService.create(getUserInfo(), ENDPOINT_NAME, getEndpointDTO());
	}

	@Test(expected = DlabException.class)
	public void createWithException4() {
		Response response = mock(Response.class);
		when(endpointDAO.get(anyString())).thenReturn(Optional.empty());
		when(endpointDAO.getEndpointWithUrl(anyString())).thenReturn(Optional.empty());
		when(provisioningService.get(anyString(), anyString(), any(Class.class))).thenReturn(response);
		when(response.readEntity(any(Class.class))).thenReturn(new Object());

		endpointService.create(getUserInfo(), ENDPOINT_NAME, getEndpointDTO());
	}

	@Test(expected = ResourceNotFoundException.class)
	public void createWithException5() {
		Response response = mock(Response.class);
		when(endpointDAO.get(anyString())).thenReturn(Optional.empty());
		when(endpointDAO.getEndpointWithUrl(anyString())).thenReturn(Optional.empty());
		when(provisioningService.get(anyString(), anyString(), any(Class.class))).thenReturn(response);
		when(response.readEntity(any(Class.class))).thenReturn(CloudProvider.AWS);
		when(response.getStatus()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);

		endpointService.create(getUserInfo(), ENDPOINT_NAME, getEndpointDTO());
	}

	@Test(expected = DlabException.class)
	public void createWithException6() {
		Response response = mock(Response.class);
		when(endpointDAO.get(anyString())).thenReturn(Optional.empty());
		when(endpointDAO.getEndpointWithUrl(anyString())).thenReturn(Optional.empty());
		when(provisioningService.get(anyString(), anyString(), any(Class.class))).thenReturn(response);
		when(response.readEntity(any(Class.class))).thenReturn(null);
		when(response.getStatus()).thenReturn(HttpStatus.SC_OK);

		endpointService.create(getUserInfo(), ENDPOINT_NAME, getEndpointDTO());
	}

	@Test
	public void updateEndpointStatus() {
		endpointService.updateEndpointStatus(ENDPOINT_NAME, EndpointDTO.EndpointStatus.ACTIVE);

		verify(endpointDAO).updateEndpointStatus(ENDPOINT_NAME, EndpointDTO.EndpointStatus.ACTIVE.toString());
		verifyNoMoreInteractions(endpointDAO);
	}

	@Test
	public void remove() {
		List<ProjectDTO> projectDTOs = getProjectDTOs();
		List<EndpointDTO> endpointDTOs = getEndpointDTOs();
		when(endpointDAO.get(anyString())).thenReturn(Optional.of(getEndpointDTO()));
		when(projectService.getProjectsByEndpoint(anyString())).thenReturn(projectDTOs);
		when(projectService.checkExploratoriesAndComputationalProgress(anyString(), anyListOf(String.class))).thenReturn(Boolean.TRUE);
		when(endpointDAO.getEndpoints()).thenReturn(endpointDTOs);

		endpointService.remove(getUserInfo(), ENDPOINT_NAME);

		verify(endpointDAO).get(ENDPOINT_NAME);
		verify(projectService).getProjectsByEndpoint(ENDPOINT_NAME);
		verify(projectService).checkExploratoriesAndComputationalProgress(PROJECT_NAME_1, Collections.singletonList(ENDPOINT_NAME));
		verify(projectService).checkExploratoriesAndComputationalProgress(PROJECT_NAME_2, Collections.singletonList(ENDPOINT_NAME));
		verify(projectService).terminateEndpoint(getUserInfo(), ENDPOINT_NAME, PROJECT_NAME_1);
		verify(projectService).terminateEndpoint(getUserInfo(), ENDPOINT_NAME, PROJECT_NAME_2);
		verify(endpointDAO).remove(ENDPOINT_NAME);
		verify(endpointDAO).getEndpoints();
		verify(userRoleDao).removeUnnecessaryRoles(CloudProvider.AWS, Arrays.asList(CloudProvider.AWS, CloudProvider.GCP));
		verifyNoMoreInteractions(endpointDAO, projectService, userRoleDao);
	}

	@Test(expected = ResourceNotFoundException.class)
	public void removeWithException1() {
		when(endpointDAO.get(anyString())).thenReturn(Optional.empty());

		endpointService.remove(getUserInfo(), ENDPOINT_NAME);
	}

	@Test(expected = ResourceConflictException.class)
	public void removeWithException2() {
		when(endpointDAO.get(anyString())).thenReturn(Optional.of(getEndpointDTO()));
		when(projectService.getProjectsByEndpoint(anyString())).thenReturn(getProjectDTOs());
		when(projectService.checkExploratoriesAndComputationalProgress(anyString(), anyListOf(String.class))).thenReturn(Boolean.FALSE);

		endpointService.remove(getUserInfo(), ENDPOINT_NAME);
	}

	@Test(expected = ResourceConflictException.class)
	public void removeWithException3() {
		when(endpointDAO.get(anyString())).thenReturn(Optional.of(getEndpointDTO()));
		when(projectService.getProjectsByEndpoint(anyString())).thenReturn(getCreatingProjectDTO());
		when(projectService.checkExploratoriesAndComputationalProgress(anyString(), anyListOf(String.class))).thenReturn(Boolean.TRUE);

		endpointService.remove(getUserInfo(), ENDPOINT_NAME);
	}

	private List<UserInstanceDTO> getUserInstances() {
		return Arrays.asList(
				new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_1).withUser(USER).withProject(PROJECT_NAME_1).withEndpoint(ENDPOINT_NAME),
				new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_2).withUser(USER).withProject(PROJECT_NAME_1).withEndpoint(ENDPOINT_NAME)
		);
	}

	private List<EndpointDTO> getEndpointDTOs() {
		return Arrays.asList(getEndpointDTO(), getInactiveEndpointDTO());
	}

	private EndpointDTO getInactiveEndpointDTO() {
		return new EndpointDTO("local2", "endpoint_url2", "endpoint_account2", "endpoint_tag2",
				EndpointDTO.EndpointStatus.INACTIVE, CloudProvider.GCP);
	}

	private List<ProjectDTO> getProjectDTOs() {
		ProjectDTO project1 = ProjectDTO.builder()
				.name(PROJECT_NAME_1)
				.endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING, null)))
				.build();
		ProjectDTO project2 = ProjectDTO.builder()
				.name(PROJECT_NAME_2)
				.endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING, null)))
				.build();
		return Arrays.asList(project1, project2);
	}

	private List<ProjectDTO> getCreatingProjectDTO() {
		ProjectDTO project = ProjectDTO.builder()
				.name(PROJECT_NAME_1)
				.endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.CREATING, null)))
				.build();
		return Collections.singletonList(project);
	}
}