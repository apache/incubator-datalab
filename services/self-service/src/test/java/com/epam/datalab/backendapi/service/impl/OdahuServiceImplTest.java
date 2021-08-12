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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.dao.OdahuDAO;
import com.epam.datalab.backendapi.domain.BudgetDTO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.OdahuCreateDTO;
import com.epam.datalab.backendapi.domain.OdahuDTO;
import com.epam.datalab.backendapi.domain.OdahuFieldsDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectEndpointDTO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.edge.EdgeInfo;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceConflictException;
import com.epam.datalab.rest.client.RESTService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OdahuServiceImplTest {

	private static final String USER = "testUser";
	private static final String TOKEN = "testToken";
	private static final String PROJECT = "testProject";
	private static final String ENDPOINT_URL = "https://localhsot:8080/";
	private static final String ENDPOINT_NAME = "testEndpoint";
	private static final String tag = "testTag";
	private static final String uuid = "34dsr324";
	private static final String ODAHU_NAME = "odahuTest";
	private static UserInfo userInfo;

	@Mock
	private OdahuDAO odahuDAO;
	@Mock
	private ProjectService projectService;
	@Mock
	private EndpointService endpointService;
	@Mock
	private RequestId requestId;
	@Mock
	private RESTService provisioningService;
	@Mock
	private RequestBuilder requestBuilder;
	@InjectMocks
	private OdahuServiceImpl odahuService;

	@BeforeClass
	public static void setUp() {
		userInfo = new UserInfo(USER, TOKEN);
	}

	@Test
	public void findOdahuTest() {
		List<OdahuDTO> odahuDTOList = odahuService.findOdahu();
		assertNotNull(odahuDTOList);

		verify(odahuDAO).findOdahuClusters();
	}

	@Test
	public void createTest() {
		EndpointDTO endpointDTO = getEndpointDTO();
		ProjectDTO projectDTO = getProjectDTO(UserInstanceStatus.RUNNING);
		OdahuCreateDTO odahuCreateDTO = getOdahuCreateDTO();

		when(projectService.get(anyString())).thenReturn(projectDTO);
		when(odahuDAO.create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
				odahuCreateDTO.getEndpoint(), UserInstanceStatus.CREATING, getTags()))).thenReturn(true);
		when(endpointService.get(odahuCreateDTO.getEndpoint())).thenReturn(endpointDTO);
		when(requestId.put(userInfo.getName(), uuid)).thenReturn(uuid);

		odahuService.create(PROJECT, odahuCreateDTO, userInfo);

		verify(odahuDAO).findOdahuClusters(odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint());
		verify(odahuDAO).create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
				odahuCreateDTO.getEndpoint(), UserInstanceStatus.CREATING, getTags()));
		verify(endpointService).get(odahuCreateDTO.getEndpoint());
		verify(projectService).get(PROJECT);
		verify(provisioningService).post(ENDPOINT_URL + "infrastructure/odahu", userInfo.getAccessToken(),
				requestBuilder.newOdahuCreate(userInfo.getName(), odahuCreateDTO, projectDTO, endpointDTO), String.class);
		verifyNoMoreInteractions(odahuDAO, provisioningService, endpointService, projectService);
	}

	@Test(expected = ResourceConflictException.class)
	public void createIfClusterActive() {
		OdahuCreateDTO odahuCreateDTO = getOdahuCreateDTO();
		OdahuDTO failedOdahu = getOdahuDTO(UserInstanceStatus.RUNNING);
		List<OdahuDTO> runningOdahuList = singletonList(failedOdahu);
		when(odahuDAO.findOdahuClusters(anyString(), anyString())).thenReturn(runningOdahuList);

		odahuService.create(PROJECT, odahuCreateDTO, userInfo);
		verify(odahuDAO).findOdahuClusters(odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint());
		verifyNoMoreInteractions(odahuDAO);
	}

	@Test(expected = DatalabException.class)
	public void createIfDBissue() {
		EndpointDTO endpointDTO = getEndpointDTO();
		ProjectDTO projectDTO = getProjectDTO(UserInstanceStatus.RUNNING);
		OdahuCreateDTO odahuCreateDTO = getOdahuCreateDTO();

		when(projectService.get(anyString())).thenReturn(projectDTO);
		when(odahuDAO.create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
				odahuCreateDTO.getEndpoint(), UserInstanceStatus.CREATING, getTags()))).thenReturn(false);
		when(endpointService.get(anyString())).thenReturn(endpointDTO);
		when(requestId.put(userInfo.getName(), uuid)).thenReturn(uuid);

		odahuService.create(PROJECT, odahuCreateDTO, userInfo);

		verify(odahuDAO).findOdahuClusters(odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint());
		verify(odahuDAO).create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
				odahuCreateDTO.getEndpoint(), UserInstanceStatus.CREATING, getTags()));
		verify(endpointService).get(odahuCreateDTO.getEndpoint());
		verify(projectService).get(PROJECT);
		verify(provisioningService).post(ENDPOINT_URL + "infrastructure/odahu", userInfo.getAccessToken(),
				requestBuilder.newOdahuCreate(userInfo.getName(), odahuCreateDTO, projectDTO, endpointDTO), String.class);
		verifyNoMoreInteractions(odahuDAO, provisioningService, endpointService, projectService);
	}

	@Test
	public void startTest() {
		when(endpointService.get(anyString())).thenReturn(getEndpointDTO());

		odahuService.start(ODAHU_NAME, PROJECT, ENDPOINT_URL, userInfo);

		verify(endpointService).get(ENDPOINT_URL);
		verify(odahuDAO).updateStatus(ODAHU_NAME, PROJECT, ENDPOINT_URL, UserInstanceStatus.STARTING);
		verify(odahuDAO).getFields(ODAHU_NAME, PROJECT, ENDPOINT_URL);
		verify(provisioningService).post(ENDPOINT_URL + "infrastructure/odahu/start", userInfo.getAccessToken(),
				requestBuilder.newOdahuAction(userInfo.getName(), ODAHU_NAME, getProjectDTO(UserInstanceStatus.STARTING), getEndpointDTO(), new OdahuFieldsDTO()), String.class);
		verifyNoMoreInteractions(endpointService, odahuDAO);
	}

	@Test
	public void stopTest() {
		when(endpointService.get(anyString())).thenReturn(getEndpointDTO());

		odahuService.stop(ODAHU_NAME, PROJECT, ENDPOINT_URL, userInfo);

		verify(endpointService).get(ENDPOINT_URL);
		verify(odahuDAO).updateStatus(ODAHU_NAME, PROJECT, ENDPOINT_URL, UserInstanceStatus.STOPPING);
		verify(odahuDAO).getFields(ODAHU_NAME, PROJECT, ENDPOINT_URL);
		verify(provisioningService).post(ENDPOINT_URL + "infrastructure/odahu/stop", userInfo.getAccessToken(),
				requestBuilder.newOdahuAction(userInfo.getName(), ODAHU_NAME, getProjectDTO(UserInstanceStatus.STOPPING), getEndpointDTO(), new OdahuFieldsDTO()), String.class);
		verifyNoMoreInteractions(endpointService, odahuDAO, provisioningService);
	}

	@Test
	public void terminateTest() {
		List<OdahuDTO> odahuDTOS = Arrays.asList(
				getOdahuDTO(UserInstanceStatus.RUNNING),
				getOdahuDTO(UserInstanceStatus.FAILED));
		when(odahuDAO.findOdahuClusters(anyString(), anyString())).thenReturn(odahuDTOS);
		when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
		when(odahuDAO.getFields(anyString(), anyString(), anyString())).thenReturn(new OdahuFieldsDTO());

		odahuService.terminate(ODAHU_NAME, PROJECT, ENDPOINT_URL, userInfo);

		verify(odahuDAO).findOdahuClusters(PROJECT, ENDPOINT_URL);
		verify(endpointService).get(ENDPOINT_URL);
		verify(odahuDAO).updateStatus(ODAHU_NAME, PROJECT, ENDPOINT_URL, UserInstanceStatus.TERMINATING);
		verify(odahuDAO).getFields(ODAHU_NAME, PROJECT, ENDPOINT_URL);
		verify(provisioningService).post(ENDPOINT_URL + "infrastructure/odahu/terminate", userInfo.getAccessToken(),
				requestBuilder.newOdahuAction(userInfo.getName(), PROJECT, getProjectDTO(UserInstanceStatus.TERMINATING), getEndpointDTO(), new OdahuFieldsDTO()), String.class);
		verifyNoMoreInteractions(odahuDAO, endpointService, provisioningService);
	}

	@Test(expected = DatalabException.class)
	public void terminateIfIncorrectStatusTest() {
		List<OdahuDTO> odahuDTOS = Arrays.asList(
				getOdahuDTO(UserInstanceStatus.TERMINATING),
				getOdahuDTO(UserInstanceStatus.FAILED));
		when(odahuDAO.findOdahuClusters(anyString(), anyString())).thenReturn(odahuDTOS);

		odahuService.terminate(ODAHU_NAME, PROJECT, ENDPOINT_URL, userInfo);

		verify(odahuDAO).findOdahuClusters(PROJECT, ENDPOINT_URL);
		verifyNoMoreInteractions(odahuDAO, endpointService, provisioningService);
	}

	private Map<String, String> getTags() {
		Map<String, String> tags = new HashMap<>();
		tags.put("custom_tag", getOdahuCreateDTO().getCustomTag());
		tags.put("project_tag", getOdahuCreateDTO().getProject());
		tags.put("endpoint_tag", getOdahuCreateDTO().getEndpoint());
		return tags;
	}

	private EndpointDTO getEndpointDTO() {
		return new EndpointDTO(ENDPOINT_NAME, ENDPOINT_URL, "testAccount", tag, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.GCP);
	}

	private ProjectDTO getProjectDTO(UserInstanceStatus instanceStatus) {
		return new ProjectDTO(PROJECT,
				Collections.emptySet(),
				"ssh-testKey\n", tag, new BudgetDTO(200, Boolean.FALSE),
				singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, instanceStatus, new EdgeInfo())),
				true);
	}

	private OdahuDTO getOdahuDTO(UserInstanceStatus instanceStatus) {
		return new OdahuDTO(ODAHU_NAME, PROJECT, ENDPOINT_NAME, instanceStatus, getTags());
	}

	private OdahuCreateDTO getOdahuCreateDTO() {
		return new OdahuCreateDTO(ODAHU_NAME, PROJECT, ENDPOINT_URL, tag);
	}
}
