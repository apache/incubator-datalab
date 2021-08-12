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
import com.epam.datalab.backendapi.dao.EnvDAO;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.dao.UserSettingsDAO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectEndpointDTO;
import com.epam.datalab.backendapi.resources.dto.UserDTO;
import com.epam.datalab.backendapi.resources.dto.UserResourceInfo;
import com.epam.datalab.backendapi.service.ComputationalService;
import com.epam.datalab.backendapi.service.ExploratoryService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.service.SecurityService;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.edge.EdgeInfo;
import com.epam.datalab.dto.status.EnvResource;
import com.epam.datalab.dto.status.EnvResourceList;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceConflictException;
import com.epam.datalab.model.ResourceEnum;
import com.epam.datalab.model.ResourceType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.epam.datalab.dto.UserInstanceStatus.CREATING;
import static com.epam.datalab.dto.UserInstanceStatus.CREATING_IMAGE;
import static com.epam.datalab.dto.UserInstanceStatus.STARTING;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentServiceImplTest {

    private static final String AUDIT_QUOTA_MESSAGE = "Billing quota reached";
    private static final String AUDIT_UPDATE_STATUS = "Sync up with console status";
    private static final String AUDIT_MESSAGE = "Notebook name %s";
    private static final String DATALAB_SYSTEM_USER = "DataLab system user";
    private static final String DATALAB_SYSTEM_USER_TOKEN = "token";
    private static final String USER = "test";
    private static final String EXPLORATORY_NAME_1 = "expName1";
    private static final String EXPLORATORY_NAME_2 = "expName2";
    private static final String TOKEN = "token";
    private static final String UUID = "213-12312-321";
    private static final String PROJECT_NAME = "projectName";
    private static final String ENDPOINT_NAME = "endpointName";
    private static final String SHAPE = "shape";

    private static final String INSTANCE_ID = "instance_id";
    private static final String NAME = "name";
    private static final String PROJECT = "project";
    private static final String ENDPOINT = "endpoint";
    private static final String STATUS = "running";

    @Mock
    private EnvDAO envDAO;
    @Mock
    private ExploratoryDAO exploratoryDAO;
    @Mock
    private SecurityService securityService;
    @Mock
    private ExploratoryService exploratoryService;
    @Mock
    private ComputationalService computationalService;
    @Mock
    private UserSettingsDAO userSettingsDAO;
    @Mock
    private ProjectService projectService;

    @InjectMocks
    private EnvironmentServiceImpl environmentService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void getActiveUsers() {
        doReturn(Collections.singleton(USER)).when(envDAO).fetchActiveEnvUsers();
        doReturn(Collections.singleton(USER + "2")).when(envDAO).fetchUsersNotIn(anySet());
        when(userSettingsDAO.getAllowedBudget(anyString())).thenReturn(Optional.empty());
        final List<UserDTO> activeUsers = environmentService.getUsers();

        assertEquals(2, activeUsers.size());
        assertEquals(USER, activeUsers.get(0).getName());
        assertEquals(USER + "2", activeUsers.get(1).getName());

        verify(userSettingsDAO).getAllowedBudget(USER);
        verify(userSettingsDAO).getAllowedBudget(USER + "2");
        verify(envDAO).fetchActiveEnvUsers();
        verify(envDAO).fetchUsersNotIn(Collections.singleton(USER));
        verifyNoMoreInteractions(envDAO);
    }

    @Test
    public void getAllEnv() {
        when(exploratoryDAO.getInstances()).thenReturn(getUserInstances());
        when(projectService.getProjects(any(UserInfo.class))).thenReturn(Collections.singletonList(getProjectDTO()));

        List<UserResourceInfo> actualAllEnv = environmentService.getAllEnv(getUserInfo());

        List<UserResourceInfo> userResources = Arrays.asList(getUserResourceInfoEdge(), getUserResourceInfo(EXPLORATORY_NAME_1), getUserResourceInfo(EXPLORATORY_NAME_2));
        assertEquals("lists are not equal", userResources, actualAllEnv);
        verify(exploratoryDAO).getInstances();
        verify(projectService).getProjects(getUserInfo());
        verifyNoMoreInteractions(exploratoryDAO, projectService);
    }

    @Test
    public void getAllEnvWithoutEdge() {
        when(exploratoryDAO.getInstances()).thenReturn(getUserInstances());
        when(projectService.getProjects(any(UserInfo.class))).thenReturn(Collections.singletonList(getProjectDTOWithoutEndpoint()));

        List<UserResourceInfo> actualAllEnv = environmentService.getAllEnv(getUserInfo());

        List<UserResourceInfo> userResources = Arrays.asList(getUserResourceInfo(EXPLORATORY_NAME_1), getUserResourceInfo(EXPLORATORY_NAME_2));
        assertEquals("lists are not equal", userResources, actualAllEnv);
        verify(exploratoryDAO).getInstances();
        verify(projectService).getProjects(getUserInfo());
        verifyNoMoreInteractions(exploratoryDAO, projectService);
    }

    @Test
    public void stopAll() {
        when(projectService.getProjects()).thenReturn(Collections.singletonList(getProjectDTO()));
        when(exploratoryDAO.fetchProjectExploratoriesWhereStatusIn(anyString(), anyListOf(UserInstanceStatus.class))).thenReturn(Collections.emptyList());
        when(exploratoryDAO.fetchRunningExploratoryFieldsForProject(anyString())).thenReturn(getUserInstances());
        when(securityService.getServiceAccountInfo(anyString())).thenReturn(getDataLabSystemUser());
        when(projectService.get(anyString())).thenReturn(getProjectDTO());

        environmentService.stopAll();

        verify(projectService).getProjects();
        verify(exploratoryDAO).fetchProjectExploratoriesWhereStatusIn(PROJECT_NAME, Arrays.asList(CREATING, STARTING, CREATING_IMAGE), CREATING, STARTING, CREATING_IMAGE);
        verify(exploratoryDAO).fetchRunningExploratoryFieldsForProject(PROJECT_NAME);
        verify(securityService, times(3)).getServiceAccountInfo(DATALAB_SYSTEM_USER);
        verify(exploratoryService).stop(getDataLabSystemUser(), USER, PROJECT_NAME, EXPLORATORY_NAME_1, AUDIT_QUOTA_MESSAGE);
        verify(exploratoryService).stop(getDataLabSystemUser(), USER, PROJECT_NAME, EXPLORATORY_NAME_2, AUDIT_QUOTA_MESSAGE);
        verify(projectService).get(PROJECT_NAME);
        verify(projectService).stop(getDataLabSystemUser(), ENDPOINT_NAME, PROJECT_NAME, AUDIT_QUOTA_MESSAGE);
        verifyNoMoreInteractions(projectService, exploratoryDAO, securityService, exploratoryService);
    }

    @Test(expected = ResourceConflictException.class)
    public void stopAllWithException() {
        when(projectService.getProjects()).thenReturn(Collections.singletonList(getProjectDTO()));
        when(exploratoryDAO.fetchProjectExploratoriesWhereStatusIn(PROJECT_NAME, Arrays.asList(CREATING, STARTING, CREATING_IMAGE), CREATING, STARTING, CREATING_IMAGE)).thenReturn(getUserInstances());

        environmentService.stopAll();

        verify(projectService).getProjects();
        verify(exploratoryDAO).fetchProjectExploratoriesWhereStatusIn(PROJECT_NAME, Arrays.asList(CREATING, STARTING, CREATING_IMAGE), CREATING, STARTING, CREATING_IMAGE);
        verifyNoMoreInteractions(projectService, exploratoryDAO, securityService, exploratoryService);
    }

    @Test
    public void stopAllWithStoppedProject() {
        when(projectService.getProjects()).thenReturn(Collections.singletonList(getProjectDTOWithStoppedEdge()));
        when(exploratoryDAO.fetchProjectExploratoriesWhereStatusIn(anyString(), anyListOf(UserInstanceStatus.class))).thenReturn(Collections.emptyList());
        when(exploratoryDAO.fetchRunningExploratoryFieldsForProject(anyString())).thenReturn(getUserInstances());
        when(securityService.getServiceAccountInfo(anyString())).thenReturn(getDataLabSystemUser());
        when(projectService.get(anyString())).thenReturn(getProjectDTOWithStoppedEdge());

        environmentService.stopAll();

        verify(projectService).getProjects();
        verify(exploratoryDAO).fetchProjectExploratoriesWhereStatusIn(PROJECT_NAME, Arrays.asList(CREATING, STARTING, CREATING_IMAGE), CREATING, STARTING, CREATING_IMAGE);
        verify(exploratoryDAO).fetchRunningExploratoryFieldsForProject(PROJECT_NAME);
        verify(securityService, times(2)).getServiceAccountInfo(DATALAB_SYSTEM_USER);
        verify(exploratoryService).stop(getDataLabSystemUser(), USER, PROJECT_NAME, EXPLORATORY_NAME_1, AUDIT_QUOTA_MESSAGE);
        verify(exploratoryService).stop(getDataLabSystemUser(), USER, PROJECT_NAME, EXPLORATORY_NAME_2, AUDIT_QUOTA_MESSAGE);
        verify(projectService).get(PROJECT_NAME);
        verifyNoMoreInteractions(projectService, exploratoryDAO, securityService, exploratoryService);
    }

    @Test
    public void stopEnvironmentWithServiceAccount() {
        when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), anyListOf(UserInstanceStatus.class))).thenReturn(Collections.emptyList());
        when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(getUserInstances());
        when(securityService.getServiceAccountInfo(anyString())).thenReturn(getDataLabSystemUser());

        environmentService.stopEnvironmentWithServiceAccount(USER);

        verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(USER, Arrays.asList(CREATING, STARTING, CREATING_IMAGE), CREATING, STARTING, CREATING_IMAGE);
        verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
        verify(securityService, times(2)).getServiceAccountInfo(DATALAB_SYSTEM_USER);
        verify(exploratoryService).stop(getDataLabSystemUser(), USER, PROJECT_NAME, EXPLORATORY_NAME_1, AUDIT_QUOTA_MESSAGE);
        verify(exploratoryService).stop(getDataLabSystemUser(), USER, PROJECT_NAME, EXPLORATORY_NAME_2, AUDIT_QUOTA_MESSAGE);
        verifyNoMoreInteractions(exploratoryDAO, securityService, exploratoryService);
    }

    @Test(expected = ResourceConflictException.class)
    public void stopEnvironmentWithServiceAccountWithException() {
        when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(USER, Arrays.asList(CREATING, STARTING, CREATING_IMAGE), CREATING, STARTING, CREATING_IMAGE))
                .thenReturn(getUserInstances());

        environmentService.stopEnvironmentWithServiceAccount(USER);

        verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(USER, Arrays.asList(CREATING, STARTING, CREATING_IMAGE), CREATING, STARTING, CREATING_IMAGE);
        verifyNoMoreInteractions(exploratoryDAO, securityService, exploratoryService);
    }

    @Test
    public void getActiveUsersWithException() {
        doThrow(new DatalabException("Users not found")).when(envDAO).fetchActiveEnvUsers();

        expectedException.expect(DatalabException.class);
        expectedException.expectMessage("Users not found");

        environmentService.getUsers();
    }

    @Test
    public void stopProjectEnvironment() {
        final UserInfo userInfo = getUserInfo();
        final ProjectDTO projectDTO = getProjectDTO();
        when(exploratoryDAO.fetchRunningExploratoryFieldsForProject(anyString())).thenReturn(getUserInstances());
        when(securityService.getServiceAccountInfo(anyString())).thenReturn(userInfo);
        when(exploratoryService.stop(any(UserInfo.class), anyString(), anyString(), anyString(), anyString())).thenReturn(UUID);
        when(projectService.get(anyString())).thenReturn(projectDTO);
        doNothing().when(projectService).stop(any(UserInfo.class), anyString(), anyString(), anyString());

        environmentService.stopProjectEnvironment(PROJECT_NAME);

        verify(exploratoryDAO).fetchRunningExploratoryFieldsForProject(PROJECT_NAME);
        verify(exploratoryService).stop(refEq(userInfo), eq(USER), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1), eq(AUDIT_QUOTA_MESSAGE));
        verify(exploratoryService).stop(refEq(userInfo), eq(USER), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_2), eq(AUDIT_QUOTA_MESSAGE));
        verify(securityService, times(3)).getServiceAccountInfo(DATALAB_SYSTEM_USER);
        verify(projectService).get(eq(PROJECT_NAME));
        verify(projectService).stop(refEq(userInfo), eq(ENDPOINT_NAME), eq(PROJECT_NAME), eq(AUDIT_QUOTA_MESSAGE));
        verify(exploratoryDAO).fetchProjectExploratoriesWhereStatusIn(PROJECT_NAME, Arrays.asList(UserInstanceStatus.CREATING,
                UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE),
                UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
        verifyNoMoreInteractions(exploratoryDAO, exploratoryService, projectService);
    }

    @Test
    public void stopExploratory() {
        final UserInfo userInfo = getUserInfo();
        when(exploratoryService.stop(any(UserInfo.class), anyString(), anyString(), anyString(), anyString())).thenReturn(UUID);

        environmentService.stopExploratory(userInfo, USER, PROJECT_NAME, EXPLORATORY_NAME_1);

        verify(exploratoryService).stop(refEq(userInfo), eq(USER), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1), eq(null));
        verifyNoMoreInteractions(securityService, exploratoryService);
    }

    @Test
    public void stopComputational() {
        final UserInfo userInfo = getUserInfo();
        doNothing().when(computationalService).stopSparkCluster(any(UserInfo.class), anyString(), anyString(), anyString(), anyString(), anyString());

        environmentService.stopComputational(userInfo, USER, PROJECT_NAME, EXPLORATORY_NAME_1, "compName");

        verify(computationalService).stopSparkCluster(refEq(userInfo), eq(userInfo.getName()), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1), eq("compName"),
                eq(String.format(AUDIT_MESSAGE, EXPLORATORY_NAME_1)));
        verifyNoMoreInteractions(securityService, computationalService);
    }

    @Test
    public void terminateExploratory() {
        final UserInfo userInfo = getUserInfo();
        when(exploratoryService.terminate(any(UserInfo.class), anyString(), anyString(), anyString(), anyString())).thenReturn(UUID);

        environmentService.terminateExploratory(userInfo, USER, PROJECT_NAME, EXPLORATORY_NAME_1);

        verify(exploratoryService).terminate(refEq(userInfo), eq(USER), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1), eq(null));
        verifyNoMoreInteractions(securityService, exploratoryService);
    }

    @Test
    public void terminateComputational() {
        final UserInfo userInfo = getUserInfo();
        doNothing().when(computationalService)
                .terminateComputational(any(UserInfo.class), anyString(), anyString(), anyString(), anyString(), anyString());

        environmentService.terminateComputational(userInfo, USER, PROJECT_NAME, EXPLORATORY_NAME_1, "compName");

        verify(computationalService).terminateComputational(refEq(userInfo), eq(userInfo.getName()), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1), eq("compName"),
                eq(String.format(AUDIT_MESSAGE, EXPLORATORY_NAME_1)));
        verifyNoMoreInteractions(securityService, computationalService);
    }

    @Test
    public void updateEnvironmentStatuses() {
        environmentService.updateEnvironmentStatuses(getEnvResourceList());

        verify(projectService,times(2)).updateAfterStatusCheck(getSystemUser(), PROJECT, ENDPOINT, INSTANCE_ID, UserInstanceStatus.of(STATUS), AUDIT_UPDATE_STATUS);
        verify(exploratoryService,times(2)).updateAfterStatusCheck(getSystemUser(), PROJECT, ENDPOINT, NAME, INSTANCE_ID, UserInstanceStatus.of(STATUS), AUDIT_UPDATE_STATUS);
        verify(computationalService,times(2)).updateAfterStatusCheck(getSystemUser(), PROJECT, ENDPOINT, NAME, INSTANCE_ID, UserInstanceStatus.of(STATUS), AUDIT_UPDATE_STATUS);
        verifyNoMoreInteractions(projectService, exploratoryService, computationalService);
    }

    @Test
    public void updateEnvironmentStatusesWithUnknownStatus() {
        EnvResourceList envResourceList = EnvResourceList.builder()
                .hostList(Collections.singletonList(new EnvResource().withStatus("unknown status")))
                .clusterList(Collections.singletonList(new EnvResource().withStatus("unknown status")))
                .build();

        environmentService.updateEnvironmentStatuses(envResourceList);

        verifyZeroInteractions(projectService, exploratoryService, computationalService);
    }

    private UserInfo getSystemUser() {
        return new UserInfo(DATALAB_SYSTEM_USER, null);
    }

    private EnvResourceList getEnvResourceList() {
        List<EnvResource> hostList = Arrays.asList(getEnvResource(ResourceType.EDGE), getEnvResource(ResourceType.EXPLORATORY),
                getEnvResource(ResourceType.COMPUTATIONAL));
        List<EnvResource> clusterList = Arrays.asList(getEnvResource(ResourceType.EDGE), getEnvResource(ResourceType.EXPLORATORY),
                getEnvResource(ResourceType.COMPUTATIONAL));
        return  EnvResourceList.builder()
                .hostList(hostList)
                .clusterList(clusterList)
                .build();
    }

    private EnvResource getEnvResource(ResourceType resourceType) {
        return new EnvResource()
                .withId(INSTANCE_ID)
                .withName(NAME)
                .withProject(PROJECT)
                .withEndpoint(ENDPOINT)
                .withStatus(STATUS)
                .withResourceType(resourceType);
    }

    private UserResourceInfo getUserResourceInfoEdge() {
        return UserResourceInfo.builder()
                .resourceType(ResourceEnum.EDGE_NODE)
                .resourceStatus("running")
                .project(PROJECT_NAME)
                .endpoint(ENDPOINT_NAME)
                .ip(null)
                .build();
    }

    private UserResourceInfo getUserResourceInfo(String exploratoryName) {
        return UserResourceInfo.builder()
                .resourceType(ResourceEnum.NOTEBOOK)
                .resourceName(exploratoryName)
                .resourceShape(SHAPE)
                .resourceStatus("running")
                .computationalResources(Collections.emptyList())
                .user(USER)
                .project(PROJECT_NAME)
                .endpoint(ENDPOINT_NAME)
                .cloudProvider("aws")
                .exploratoryUrls(null)
                .gpuEnabled(Boolean.FALSE)
                .build();
    }

    private UserInfo getUserInfo() {
        return new UserInfo(USER, TOKEN);
    }

    private UserInfo getDataLabSystemUser() {
        return new UserInfo(DATALAB_SYSTEM_USER, DATALAB_SYSTEM_USER_TOKEN);
    }

    private List<UserInstanceDTO> getUserInstances() {
        return Arrays.asList(
                new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_1).withUser(USER).withProject(PROJECT_NAME).withEndpoint(ENDPOINT_NAME)
                        .withShape(SHAPE).withStatus("running").withResources(Collections.emptyList()).withCloudProvider("aws"),
                new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_2).withUser(USER).withProject(PROJECT_NAME).withEndpoint(ENDPOINT_NAME)
                        .withShape(SHAPE).withStatus("running").withResources(Collections.emptyList()).withCloudProvider("aws"));
    }

    private ProjectDTO getProjectDTO() {
        return new ProjectDTO(PROJECT_NAME, Collections.emptySet(), "", "", null,
                Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING, new EdgeInfo())), true);
    }

    private ProjectDTO getProjectDTOWithStoppedEdge() {
        return new ProjectDTO(PROJECT_NAME, Collections.emptySet(), "", "", null,
                Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.STOPPED, new EdgeInfo())), true);
    }

    private ProjectDTO getProjectDTOWithoutEndpoint() {
        return new ProjectDTO(PROJECT_NAME, Collections.emptySet(), "", "", null, null, true);
    }
}