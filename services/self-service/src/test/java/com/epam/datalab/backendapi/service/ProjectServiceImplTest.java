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

package com.epam.datalab.backendapi.service;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.dao.ProjectDAO;
import com.epam.datalab.backendapi.dao.UserGroupDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.OdahuDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectEndpointDTO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.domain.UpdateProjectBudgetDTO;
import com.epam.datalab.backendapi.resources.TestBase;
import com.epam.datalab.backendapi.service.impl.ProjectServiceImpl;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.project.ProjectActionDTO;
import com.epam.datalab.dto.project.ProjectCreateDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceConflictException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.rest.client.RESTService;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProjectServiceImplTest extends TestBase {

    private static final String CREATE_PRJ_API = "infrastructure/project/create";
	private static final String RECREATE_PRJ_API = "infrastructure/project/recreate";
	private static final String TERMINATE_PRJ_API = "infrastructure/project/terminate";
    private static final String START_PRJ_API = "infrastructure/project/start";
    private static final String STOP_PRJ_API = "infrastructure/project/stop";

    private static final String NAME1 = "name1";
    private static final String NAME2 = "name2";
    private static final String GROUP1 = "group1";
    private static final String GROUP2 = "group2";
    private static final String UUID = "uuid";

    private static final List<UserInstanceStatus> notebookStatuses = Arrays.asList(
            UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE,
            UserInstanceStatus.CONFIGURING, UserInstanceStatus.RECONFIGURING, UserInstanceStatus.STOPPING,
            UserInstanceStatus.TERMINATING);
    private static final UserInstanceStatus[] computeStatuses = {UserInstanceStatus.CREATING, UserInstanceStatus.CONFIGURING, UserInstanceStatus.STARTING,
            UserInstanceStatus.RECONFIGURING, UserInstanceStatus.CREATING_IMAGE, UserInstanceStatus.STOPPING,
            UserInstanceStatus.TERMINATING};

    @Mock
    private ProjectDAO projectDAO;
    @Mock
    private EndpointService endpointService;
    @Mock
    private RESTService provisioningService;
    @Mock
    private RequestBuilder requestBuilder;
	@Mock
	private RequestId requestId;
	@Mock
	private ExploratoryService exploratoryService;
	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private UserGroupDAO userGroupDao;
	@Mock
	private SelfServiceApplicationConfiguration configuration;
	@Mock
	private OdahuService odahuService;
	@InjectMocks
	private ProjectServiceImpl projectService;

	@Test
	public void getProjects() {
		List<ProjectDTO> projectsMock = getProjectDTOs();
		when(projectDAO.getProjects()).thenReturn(projectsMock);

		List<ProjectDTO> projects = projectService.getProjects();

        assertEquals(projects, projectsMock);
        verify(projectDAO).getProjects();
        verifyNoMoreInteractions(projectDAO);
    }

    @Test
    public void testGetProjects() {
        List<ProjectDTO> projectsMock = getProjectDTOs();
        when(projectDAO.getProjects()).thenReturn(projectsMock);

        projectService.getProjects(getUserInfo());

        verify(projectDAO).getProjects();
        verifyNoMoreInteractions(projectDAO);
    }

    @Test
    public void getUserProjects() {
        List<ProjectDTO> projectsMock = Collections.singletonList(getProjectCreatingDTO());
        when(projectDAO.getUserProjects(any(UserInfo.class), anyBoolean())).thenReturn(projectsMock);

        List<ProjectDTO> projects = projectService.getUserProjects(getUserInfo(), Boolean.TRUE);

        assertEquals(projectsMock, projects);
        verify(projectDAO).getUserProjects(getUserInfo(), Boolean.TRUE);
        verifyNoMoreInteractions(projectDAO);
    }

    @Test
    public void getProjectsByEndpoint() {
        List<ProjectDTO> projectsMock = Collections.singletonList(getProjectCreatingDTO());
        when(projectDAO.getProjectsByEndpoint(anyString())).thenReturn(projectsMock);

        List<ProjectDTO> projects = projectService.getProjectsByEndpoint(ENDPOINT_NAME);

        assertEquals(projectsMock, projects);
        verify(projectDAO).getProjectsByEndpoint(ENDPOINT_NAME);
        verifyNoMoreInteractions(projectDAO);
    }

    @Test
    public void create() {
        when(projectDAO.get(anyString())).thenReturn(Optional.empty());
        when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
        when(provisioningService.post(anyString(), anyString(), any(), any())).thenReturn(UUID);
        when(requestBuilder.newProjectCreate(any(UserInfo.class), any(ProjectDTO.class), any(EndpointDTO.class))).thenReturn(newProjectCreate());

        ProjectDTO projectDTO = getProjectCreatingDTO();
        projectService.create(getUserInfo(), projectDTO, projectDTO.getName());

        verify(projectDAO).get(NAME1);
        verify(projectDAO).create(projectDTO);
        verify(endpointService).get(ENDPOINT_NAME);
        verify(requestBuilder).newProjectCreate(getUserInfo(), projectDTO, getEndpointDTO());
        verify(provisioningService).post(ENDPOINT_URL + CREATE_PRJ_API, TOKEN, newProjectCreate(), String.class);
        verify(requestId).put(USER.toLowerCase(), UUID);
        verifyNoMoreInteractions(projectDAO, endpointService, provisioningService, requestBuilder);
    }

    @Test(expected = ResourceConflictException.class)
    public void createWithException() {
	    when(projectDAO.get(anyString())).thenReturn(Optional.of(getProjectCreatingDTO()));

	    ProjectDTO projectDTO = getProjectCreatingDTO();
	    projectService.create(getUserInfo(), projectDTO, projectDTO.getName());

	    verify(projectDAO).get(NAME1);
	    verifyNoMoreInteractions(projectDAO);
    }

	@Test
	public void recreate() {
		ProjectDTO projectDTO = getProjectCreatingDTO();
		when(projectDAO.get(anyString())).thenReturn(Optional.of(projectDTO));
		when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
		when(provisioningService.post(anyString(), anyString(), any(), any())).thenReturn(UUID);
		when(requestBuilder.newProjectCreate(any(UserInfo.class), any(ProjectDTO.class), any(EndpointDTO.class))).thenReturn(newProjectCreate());

		projectService.recreate(getUserInfo(), ENDPOINT_NAME, projectDTO.getName());

		verify(projectDAO).get(NAME1);
		verify(projectDAO).updateEdgeStatus(NAME1, ENDPOINT_NAME, UserInstanceStatus.CREATING);
		verify(endpointService).get(ENDPOINT_NAME);
		verify(requestBuilder).newProjectCreate(getUserInfo(), projectDTO, getEndpointDTO());
		verify(provisioningService).post(ENDPOINT_URL + RECREATE_PRJ_API, TOKEN, newProjectCreate(), String.class);
		verify(requestId).put(USER.toLowerCase(), UUID);
		verifyNoMoreInteractions(projectDAO, endpointService, provisioningService, requestBuilder);
	}

	@Test
	public void get() {
		ProjectDTO projectMock = getProjectCreatingDTO();
		when(projectDAO.get(anyString())).thenReturn(Optional.of(projectMock));

		ProjectDTO project = projectService.get(NAME1);

		assertEquals(projectMock, project);
		verify(projectDAO).get(NAME1);
        verifyNoMoreInteractions(projectDAO);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void getWithException() {
        when(projectDAO.get(anyString())).thenReturn(Optional.empty());

        projectService.get(NAME1);

        verify(projectDAO).get(NAME1);
        verifyNoMoreInteractions(projectDAO);
    }

    @Test
    public void terminateEndpoint() {
	    when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
	    when(provisioningService.post(anyString(), anyString(), any(), any())).thenReturn(UUID);
	    when(requestBuilder.newProjectAction(any(UserInfo.class), anyString(), any(EndpointDTO.class))).thenReturn(getProjectActionDTO());
	    when(odahuService.get(anyString(), anyString())).thenReturn(getOdahu());

	    projectService.terminateEndpoint(getUserInfo(), ENDPOINT_NAME, NAME1);

	    verify(exploratoryService).updateProjectExploratoryStatuses(getUserInfo(), NAME1, ENDPOINT_NAME, UserInstanceStatus.TERMINATING);
	    verify(odahuService).get(NAME1, ENDPOINT_NAME);
	    verify(odahuService).terminate("name", NAME1, ENDPOINT_NAME, getUserInfo());
	    verify(projectDAO).updateEdgeStatus(NAME1, ENDPOINT_NAME, UserInstanceStatus.TERMINATING);
	    verify(endpointService).get(ENDPOINT_NAME);
	    verify(provisioningService).post(ENDPOINT_URL + TERMINATE_PRJ_API, TOKEN, getProjectActionDTO(), String.class);
	    verify(requestBuilder).newProjectAction(getUserInfo(), NAME1, getEndpointDTO());
	    verify(requestId).put(USER.toLowerCase(), UUID);
	    verifyNoMoreInteractions(projectDAO, endpointService, provisioningService, requestBuilder, exploratoryService, odahuService);
    }

    @Test
    public void terminateEndpointWithException() {
	    when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
	    when(requestBuilder.newProjectAction(any(UserInfo.class), anyString(), any(EndpointDTO.class))).thenReturn(getProjectActionDTO());
	    when(provisioningService.post(anyString(), anyString(), any(), any())).thenThrow(new DatalabException("Exception message"));
	    when(odahuService.get(anyString(), anyString())).thenReturn(getOdahu());

	    projectService.terminateEndpoint(getUserInfo(), ENDPOINT_NAME, NAME1);

	    verify(exploratoryService).updateProjectExploratoryStatuses(getUserInfo(), NAME1, ENDPOINT_NAME, UserInstanceStatus.TERMINATING);
	    verify(odahuService).get(NAME1, ENDPOINT_NAME);
	    verify(odahuService).terminate("name", NAME1, ENDPOINT_NAME, getUserInfo());
	    verify(projectDAO).updateEdgeStatus(NAME1, ENDPOINT_NAME, UserInstanceStatus.TERMINATING);
	    verify(projectDAO).updateStatus(NAME1, ProjectDTO.Status.FAILED);
	    verify(endpointService).get(ENDPOINT_NAME);
	    verify(provisioningService).post(ENDPOINT_URL + TERMINATE_PRJ_API, TOKEN, getProjectActionDTO(), String.class);
	    verify(requestBuilder).newProjectAction(getUserInfo(), NAME1, getEndpointDTO());
	    verifyNoMoreInteractions(projectDAO, endpointService, provisioningService, requestBuilder, exploratoryService, odahuService);
    }

    @Test
    public void testTerminateEndpoint() {
	    when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
	    when(provisioningService.post(anyString(), anyString(), any(), any())).thenReturn(UUID);
	    when(odahuService.get(anyString(), anyString())).thenReturn(getOdahu());
	    when(odahuService.inProgress(anyString(), anyString())).thenReturn(Boolean.FALSE);
	    when(requestBuilder.newProjectAction(any(UserInfo.class), anyString(), any(EndpointDTO.class))).thenReturn(getProjectActionDTO());
	    when(projectDAO.get(anyString())).thenReturn(Optional.of(getProjectRunningDTO()));
	    when(exploratoryDAO.fetchProjectEndpointExploratoriesWhereStatusIn(anyString(), anyListOf(String.class), anyListOf(UserInstanceStatus.class)))
			    .thenReturn(Collections.emptyList());

	    projectService.terminateEndpoint(getUserInfo(), Collections.singletonList(ENDPOINT_NAME), NAME1);

	    verify(projectDAO).get(NAME1);
	    verify(exploratoryDAO).fetchProjectEndpointExploratoriesWhereStatusIn(NAME1, Collections.singletonList(ENDPOINT_NAME), notebookStatuses, computeStatuses);
	    verify(exploratoryService).updateProjectExploratoryStatuses(getUserInfo(), NAME1, ENDPOINT_NAME, UserInstanceStatus.TERMINATING);
	    verify(projectDAO).updateEdgeStatus(NAME1, ENDPOINT_NAME, UserInstanceStatus.TERMINATING);
	    verify(endpointService).get(ENDPOINT_NAME);
	    verify(odahuService).get(NAME1, ENDPOINT_NAME);
	    verify(odahuService).terminate("name", NAME1, ENDPOINT_NAME, getUserInfo());
	    verify(odahuService).inProgress(NAME1, ENDPOINT_NAME);
	    verify(provisioningService).post(ENDPOINT_URL + TERMINATE_PRJ_API, TOKEN, getProjectActionDTO(), String.class);
	    verify(requestBuilder).newProjectAction(getUserInfo(), NAME1, getEndpointDTO());
	    verify(requestId).put(USER.toLowerCase(), UUID);
	    verifyNoMoreInteractions(projectDAO, endpointService, provisioningService, requestBuilder, exploratoryDAO, odahuService);
    }

	@Test(expected = ResourceConflictException.class)
	public void testTerminateEndpointWithException1() {
		when(projectDAO.get(anyString())).thenReturn(Optional.of(getProjectCreatingDTO()));

		projectService.terminateEndpoint(getUserInfo(), Collections.singletonList(ENDPOINT_NAME), NAME1);

		verify(projectDAO).get(NAME1);
		verifyNoMoreInteractions(projectDAO);
	}

	@Test(expected = ResourceConflictException.class)
	public void testTerminateEndpointWithException2() {
		when(projectDAO.get(anyString())).thenReturn(Optional.of(getProjectRunningDTO()));
		when(odahuService.inProgress(anyString(), anyString())).thenReturn(Boolean.TRUE);

		projectService.terminateEndpoint(getUserInfo(), Collections.singletonList(ENDPOINT_NAME), NAME1);

		verify(projectDAO).get(NAME1);
		verify(odahuService).inProgress(NAME1, ENDPOINT_NAME);
		verifyNoMoreInteractions(projectDAO, odahuService);
	}

	@Test
	public void start() {
		when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
		when(provisioningService.post(anyString(), anyString(), any(), any())).thenReturn(UUID);
		when(requestBuilder.newProjectAction(any(UserInfo.class), anyString(), any(EndpointDTO.class))).thenReturn(getProjectActionDTO());

		projectService.start(getUserInfo(), ENDPOINT_NAME, NAME1);

		verify(projectDAO).updateEdgeStatus(NAME1, ENDPOINT_NAME, UserInstanceStatus.STARTING);
        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).post(ENDPOINT_URL + START_PRJ_API, TOKEN, getProjectActionDTO(), String.class);
        verify(requestBuilder).newProjectAction(getUserInfo(), NAME1, getEndpointDTO());
        verify(requestId).put(USER.toLowerCase(), UUID);
        verifyNoMoreInteractions(projectDAO, endpointService, provisioningService, requestBuilder, requestId);
    }

    @Test
    public void testStart() {
        when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
        when(provisioningService.post(anyString(), anyString(), any(), any())).thenReturn(UUID);
        when(requestBuilder.newProjectAction(any(UserInfo.class), anyString(), any(EndpointDTO.class))).thenReturn(getProjectActionDTO());

        projectService.start(getUserInfo(), Collections.singletonList(ENDPOINT_NAME), NAME1);

        verify(projectDAO).updateEdgeStatus(NAME1, ENDPOINT_NAME, UserInstanceStatus.STARTING);
        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).post(ENDPOINT_URL + START_PRJ_API, TOKEN, getProjectActionDTO(), String.class);
        verify(requestBuilder).newProjectAction(getUserInfo(), NAME1, getEndpointDTO());
        verify(requestId).put(USER.toLowerCase(), UUID);
        verifyNoMoreInteractions(projectDAO, endpointService, provisioningService, requestBuilder, requestId);
    }

    @Test
    public void stop() {
        when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
        when(provisioningService.post(anyString(), anyString(), any(), any())).thenReturn(UUID);
        when(requestBuilder.newProjectAction(any(UserInfo.class), anyString(), any(EndpointDTO.class))).thenReturn(getProjectActionDTO());

        projectService.stop(getUserInfo(), ENDPOINT_NAME, NAME1, null);

        verify(projectDAO).updateEdgeStatus(NAME1, ENDPOINT_NAME, UserInstanceStatus.STOPPING);
        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).post(ENDPOINT_URL + STOP_PRJ_API, TOKEN, getProjectActionDTO(), String.class);
        verify(requestBuilder).newProjectAction(getUserInfo(), NAME1, getEndpointDTO());
        verify(requestId).put(USER.toLowerCase(), UUID);
        verifyNoMoreInteractions(projectDAO, endpointService, provisioningService, requestBuilder, requestId);
    }

    @Test
    public void stopWithResources() {
        when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
        when(provisioningService.post(anyString(), anyString(), any(), any())).thenReturn(UUID);
        when(requestBuilder.newProjectAction(any(UserInfo.class), anyString(), any(EndpointDTO.class))).thenReturn(getProjectActionDTO());
        when(projectDAO.get(anyString())).thenReturn(Optional.of(getProjectRunningDTO()));
        when(exploratoryDAO.fetchProjectEndpointExploratoriesWhereStatusIn(anyString(), anyListOf(String.class), anyListOf(UserInstanceStatus.class)))
                .thenReturn(Collections.emptyList());

        projectService.stopWithResources(getUserInfo(), Collections.singletonList(ENDPOINT_NAME), NAME1);

        verify(projectDAO).get(NAME1);
        verify(exploratoryDAO).fetchProjectEndpointExploratoriesWhereStatusIn(NAME1, Collections.singletonList(ENDPOINT_NAME), notebookStatuses, computeStatuses);
        verify(projectDAO).updateEdgeStatus(NAME1, ENDPOINT_NAME, UserInstanceStatus.STOPPING);
        verify(endpointService).get(ENDPOINT_NAME);
        verify(provisioningService).post(ENDPOINT_URL + STOP_PRJ_API, TOKEN, getProjectActionDTO(), String.class);
        verify(requestBuilder).newProjectAction(getUserInfo(), NAME1, getEndpointDTO());
        verify(requestId).put(USER.toLowerCase(), UUID);
        verifyNoMoreInteractions(projectDAO, endpointService, provisioningService, requestBuilder, requestId);
    }

    @Test
    public void update() {
    }

    @Test
    public void updateBudget() {
        projectService.updateBudget(getUserInfo(), Collections.singletonList(getUpdateProjectBudgetDTO()));

        verify(projectDAO).updateBudget(NAME1, 10, true);
        verifyNoMoreInteractions(projectDAO);
    }

    @Test
    public void isAnyProjectAssigned() {
        when(userGroupDao.getUserGroups(anyString())).thenReturn(Sets.newHashSet(GROUP1));
        when(projectDAO.isAnyProjectAssigned(anySet())).thenReturn(Boolean.TRUE);

        final boolean anyProjectAssigned = projectService.isAnyProjectAssigned(getUserInfo());

        assertEquals(anyProjectAssigned, Boolean.TRUE);
        verify(userGroupDao).getUserGroups(USER.toLowerCase());
        verify(projectDAO).isAnyProjectAssigned(Sets.newHashSet(GROUP1));
        verifyNoMoreInteractions(userGroupDao, projectDAO);
    }

    @Test
    public void checkExploratoriesAndComputationalProgress() {
        when(exploratoryDAO.fetchProjectEndpointExploratoriesWhereStatusIn(anyString(), anyListOf(String.class), anyListOf(UserInstanceStatus.class)))
                .thenReturn(Collections.emptyList());

        final boolean b = projectService.checkExploratoriesAndComputationalProgress(NAME1, Collections.singletonList(ENDPOINT_NAME));

        assertEquals(b, Boolean.TRUE);
        verify(exploratoryDAO).fetchProjectEndpointExploratoriesWhereStatusIn(NAME1, Collections.singletonList(ENDPOINT_NAME), notebookStatuses, computeStatuses);
        verifyNoMoreInteractions(exploratoryDAO);
    }

    private List<ProjectDTO> getProjectDTOs() {
        ProjectDTO project1 = ProjectDTO.builder()
                .name(NAME1)
                .groups(getGroup(GROUP1))
                .build();
        ProjectDTO project2 = ProjectDTO.builder()
                .name(NAME2)
                .groups(getGroup(GROUP2))
                .build();
        return Arrays.asList(project1, project2);
    }

    private ProjectDTO getProjectCreatingDTO() {
        ProjectEndpointDTO projectEndpointDTO = new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.CREATING, null);
        return ProjectDTO.builder()
                .name(NAME1)
                .groups(getGroup(GROUP1))
                .endpoints(Collections.singletonList(projectEndpointDTO))
                .build();
    }

    private ProjectDTO getProjectRunningDTO() {
        ProjectEndpointDTO projectEndpointDTO = new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING, null);
        return ProjectDTO.builder()
                .name(NAME1)
                .groups(getGroup(GROUP1))
                .endpoints(Collections.singletonList(projectEndpointDTO))
                .build();
    }

    private Set<String> getGroup(String group) {
	    return Collections.singleton(group);
    }

	private ProjectCreateDTO newProjectCreate() {
		return ProjectCreateDTO.builder()
				.name(NAME1)
				.endpoint(ENDPOINT_NAME)
				.build();
	}

	private Optional<OdahuDTO> getOdahu() {
		return Optional.of(new OdahuDTO("name", NAME1, ENDPOINT_NAME, UserInstanceStatus.RUNNING, Collections.emptyMap()));
	}

	private ProjectActionDTO getProjectActionDTO() {
		return new ProjectActionDTO(NAME1, ENDPOINT_NAME);
	}

	private UpdateProjectBudgetDTO getUpdateProjectBudgetDTO() {
		return new UpdateProjectBudgetDTO(NAME1, 10, true);
	}
}