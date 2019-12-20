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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.UserSettingsDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceConflictException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentServiceImplTest {

	private static final String USER = "test";
	private static final String EXPLORATORY_NAME_1 = "expName1";
	private static final String EXPLORATORY_NAME_2 = "expName2";
	private static final String TOKEN = "token";
	private static final String UUID = "213-12312-321";
	private static final String RUNNING_STATE = "running";
	private static final String STOPPED_STATE = "stopped";
	private static final String PROJECT_NAME = "projectName";
	private static final String ENDPOINT_NAME = "endpointName";
	private static final String ADMIN = "admin";

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
	private EdgeService edgeService;
	@Mock
	private KeyDAO keyDAO;
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
	public void getActiveUsersWithException() {
		doThrow(new DlabException("Users not found")).when(envDAO).fetchActiveEnvUsers();

		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Users not found");

		environmentService.getUsers();
	}

	@Test
	public void getAllUsers() {
		doReturn(Collections.singleton(USER)).when(envDAO).fetchAllUsers();
		final Set<String> users = environmentService.getUserNames();

		assertEquals(1, users.size());
		assertTrue(users.contains(USER));

		verify(envDAO).fetchAllUsers();
		verifyNoMoreInteractions(envDAO);
	}

	@Test
	public void getAllUsersWithException() {
		doThrow(new DlabException("Users not found")).when(envDAO).fetchAllUsers();

		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Users not found");

		environmentService.getUserNames();
	}


	@Test
	public void stopEnvironment() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(getUserInstances());
		when(securityService.getUserInfoOffline(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(RUNNING_STATE);
		when(edgeService.stop(any(UserInfo.class))).thenReturn(UUID);

		environmentService.stopEnvironment(userInfo, USER);

		verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
		verify(securityService).getUserInfoOffline(USER);
		verify(exploratoryService).stop(refEq(userInfo), eq(EXPLORATORY_NAME_1));
		verify(exploratoryService).stop(refEq(userInfo), eq(EXPLORATORY_NAME_2));
		verify(keyDAO, times(2)).getEdgeStatus(USER);
		verify(edgeService).stop(refEq(userInfo));
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(USER, Arrays.asList(UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE),
				UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
		verifyNoMoreInteractions(keyDAO, exploratoryDAO, edgeService, exploratoryService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void stopEnvironmentWithWrongResourceState() {
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg()))
				.thenReturn(getUserInstances());
		expectedException.expect(ResourceConflictException.class);

		environmentService.stopEnvironment(getUserInfo(), USER);
	}

	@Test
	public void stopEnvironmentWithEdgeStarting() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn("starting");
		expectedException.expect(ResourceConflictException.class);

		environmentService.stopEnvironment(getUserInfo(), USER);
	}

	@Test
	public void stopEnvironmentWithoutEdge() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(getUserInstances());
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STOPPED_STATE);
		when(edgeService.stop(any(UserInfo.class))).thenReturn(UUID);

		environmentService.stopEnvironment(userInfo, USER);

		verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
		verify(exploratoryService).stop(refEq(userInfo), eq(EXPLORATORY_NAME_1));
		verify(exploratoryService).stop(refEq(userInfo), eq(EXPLORATORY_NAME_2));
		verify(keyDAO, times(2)).getEdgeStatus(USER);
		verify(edgeService, never()).stop(refEq(userInfo));
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(USER, Arrays.asList(UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE),
				UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
		verifyNoMoreInteractions(keyDAO, envDAO, exploratoryDAO, edgeService, exploratoryService);
	}

	@Test
	public void stopProjectEnvironment() {
		final UserInfo userInfo = getUserInfo();
		final ProjectDTO projectDTO = getProjectDTO();
		when(exploratoryDAO.fetchRunningExploratoryFieldsForProject(anyString())).thenReturn(getUserInstances());
		when(securityService.getServiceAccountInfo(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(projectService.get(anyString())).thenReturn(projectDTO);
		doNothing().when(projectService).stop(any(UserInfo.class), anyString(), anyString());

		environmentService.stopProjectEnvironment(PROJECT_NAME);

		verify(exploratoryDAO).fetchRunningExploratoryFieldsForProject(PROJECT_NAME);
		verify(exploratoryService).stop(refEq(userInfo), eq(EXPLORATORY_NAME_1));
		verify(exploratoryService).stop(refEq(userInfo), eq(EXPLORATORY_NAME_2));
		verify(securityService, times(2)).getServiceAccountInfo(USER);
		verify(securityService).getServiceAccountInfo(ADMIN);
		verify(projectService).get(eq(PROJECT_NAME));
		verify(projectService).stop(refEq(userInfo), eq(ENDPOINT_NAME), eq(PROJECT_NAME));
		verify(exploratoryDAO).fetchProjectExploratoriesWhereStatusIn(PROJECT_NAME, Arrays.asList(UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE),
				UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
		verifyNoMoreInteractions(exploratoryDAO, exploratoryService, securityService, projectService);
	}

	@Test
	public void stopEdge() {
		final UserInfo userInfo = getUserInfo();
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(RUNNING_STATE);
		when(securityService.getUserInfoOffline(anyString())).thenReturn(userInfo);
		when(edgeService.stop(any(UserInfo.class))).thenReturn(UUID);

		environmentService.stopEdge(USER);

		verify(keyDAO).getEdgeStatus(USER);
		verify(securityService).getUserInfoOffline(USER);
		verify(edgeService).stop(refEq(userInfo));
		verifyNoMoreInteractions(keyDAO, securityService, edgeService);
	}

	@Test
	public void stopEdgeWhenItIsNotRunning() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn("starting");

		environmentService.stopEdge(USER);

		verify(keyDAO).getEdgeStatus(USER);
		verifyZeroInteractions(securityService, edgeService);
		verifyNoMoreInteractions(keyDAO);
	}

	@Test
	public void stopExploratory() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn(UUID);

		environmentService.stopExploratory(new UserInfo(USER, TOKEN), USER, EXPLORATORY_NAME_1);

		verify(exploratoryService).stop(refEq(userInfo), eq(EXPLORATORY_NAME_1));
		verifyNoMoreInteractions(securityService, exploratoryService);
	}

	@Test
	public void stopComputational() {
		final UserInfo userInfo = getUserInfo();
		doNothing().when(computationalService).stopSparkCluster(any(UserInfo.class), anyString(), anyString());

		environmentService.stopComputational(userInfo, USER, EXPLORATORY_NAME_1, "compName");

		verify(computationalService).stopSparkCluster(refEq(userInfo), eq(EXPLORATORY_NAME_1), eq("compName"));
		verifyNoMoreInteractions(securityService, computationalService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void terminateEnvironment() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg()))
				.thenReturn(Collections.emptyList());
		when(securityService.getUserInfoOffline(anyString())).thenReturn(userInfo);
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.edgeNodeExist(anyString())).thenReturn(true);
		when(edgeService.terminate(any(UserInfo.class))).thenReturn(UUID);

		environmentService.terminateEnvironment(userInfo, USER);

		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg());
		verify(securityService).getUserInfoOffline(USER);
		verify(keyDAO).edgeNodeExist(USER);
		verify(edgeService).terminate(refEq(userInfo));
		verify(exploratoryService).updateExploratoryStatuses(USER, UserInstanceStatus.TERMINATING);
		verify(keyDAO).getEdgeStatus(userInfo.getName());
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(USER, Arrays.asList(UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE),
				UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
		verifyNoMoreInteractions(keyDAO, envDAO, exploratoryDAO, edgeService, exploratoryService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void terminateEnvironmentWithoutEdge() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class),
				eq(UserInstanceStatus.CREATING), eq(UserInstanceStatus.STARTING))).thenReturn(Collections.emptyList());
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusNotIn(anyString(), eq(UserInstanceStatus.TERMINATED),
				eq(UserInstanceStatus.FAILED), eq(UserInstanceStatus.TERMINATING))).thenReturn(getUserInstances());
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.edgeNodeExist(anyString())).thenReturn(false);
		when(edgeService.terminate(any(UserInfo.class))).thenReturn(UUID);

		environmentService.terminateEnvironment(userInfo, USER);

		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusNotIn(USER, UserInstanceStatus.TERMINATED,
				UserInstanceStatus.FAILED, UserInstanceStatus.TERMINATING);
		verify(exploratoryService).terminate(refEq(userInfo), eq(EXPLORATORY_NAME_1));
		verify(exploratoryService).terminate(refEq(userInfo), eq(EXPLORATORY_NAME_2));
		verify(keyDAO).edgeNodeExist(USER);
		verify(edgeService, never()).terminate(refEq(userInfo));
		verify(keyDAO).getEdgeStatus(USER);
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(USER, Arrays.asList(UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE),
				UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
		verifyNoMoreInteractions(keyDAO, envDAO, exploratoryDAO, edgeService, exploratoryService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void terminateEnvironmentWithWrongResourceState() {
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg()))
				.thenReturn(getUserInstances());
		expectedException.expect(ResourceConflictException.class);

		environmentService.terminateEnvironment(getUserInfo(), USER);
	}

	@Test
	public void terminateEnvironmentWithEdgeStarting() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn("starting");
		expectedException.expect(ResourceConflictException.class);

		environmentService.terminateEnvironment(getUserInfo(), USER);
	}

	@Test
	public void terminateExploratory() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn(UUID);

		environmentService.terminateExploratory(userInfo, USER, EXPLORATORY_NAME_1);

		verify(exploratoryService).terminate(refEq(userInfo), eq(EXPLORATORY_NAME_1));
		verifyNoMoreInteractions(securityService, exploratoryService);
	}

	@Test
	public void terminateComputational() {
		final UserInfo userInfo = getUserInfo();
		doNothing().when(computationalService)
				.terminateComputational(any(UserInfo.class), anyString(), anyString());

		environmentService.terminateComputational(userInfo, USER, EXPLORATORY_NAME_1, "compName");

		verify(computationalService)
				.terminateComputational(refEq(userInfo), eq(EXPLORATORY_NAME_1), eq("compName"));
		verifyNoMoreInteractions(securityService, computationalService);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private List<UserInstanceDTO> getUserInstances() {
		return Arrays.asList(
				new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_1).withUser(USER).withProject("prj"),
				new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_2).withUser(USER).withProject("prj"));
	}

	private ProjectDTO getProjectDTO() {
		return new ProjectDTO(PROJECT_NAME, Collections.emptySet(), "", "", null,
				Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING,
						new EdgeInfo())), true);
	}
}