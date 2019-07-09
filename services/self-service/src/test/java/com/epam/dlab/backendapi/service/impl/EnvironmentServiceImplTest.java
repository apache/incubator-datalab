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

import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.UserSettingsDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.epam.dlab.backendapi.resources.dto.UserResourceInfo;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.model.ResourceEnum;
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

	@Mock
	private EnvDAO envDAO;
	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private SystemUserInfoService systemUserInfoService;
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
	public void getAllEnv() {
		List<UserInstanceDTO> instances = getUserInstances();
		final ProjectDTO project = new ProjectDTO("prj", Collections.emptySet(), Collections.emptySet(),
				"key", "tag", null);
		project.setEdgeInfo(new EdgeInfo());
		when(exploratoryDAO.getInstances()).thenReturn(instances);
		doReturn(Collections.singletonList(project)).when(projectService).getProjectsWithStatus(ProjectDTO.Status.ACTIVE);

		EdgeInfo edgeInfo = new EdgeInfo();
		edgeInfo.setEdgeStatus("running");
		edgeInfo.setInstanceId("someId");
		when(keyDAO.getEdgeInfo(anyString())).thenReturn(edgeInfo);

		UserResourceInfo edgeResource = new UserResourceInfo().withResourceType(ResourceEnum.EDGE_NODE)
				.withResourceStatus(edgeInfo.getEdgeStatus()).withUser(USER);

		UserResourceInfo notebook1 = new UserResourceInfo().withResourceType(ResourceEnum.NOTEBOOK)
				.withResourceName(instances.get(0).getExploratoryName())
				.withResourceStatus(instances.get(0).getStatus()).withUser(instances.get(0)
						.getUser());
		UserResourceInfo notebook2 = new UserResourceInfo().withResourceType(ResourceEnum.NOTEBOOK)
				.withResourceName(instances.get(1).getExploratoryName())
				.withResourceStatus(instances.get(1).getStatus()).withUser(instances.get(1)
						.getUser());

		List<UserResourceInfo> actualEnv = environmentService.getAllEnv();
		assertEquals(3, actualEnv.size());

		verify(exploratoryDAO).getInstances();
		verify(projectService).getProjectsWithStatus(ProjectDTO.Status.ACTIVE);
		verifyNoMoreInteractions(exploratoryDAO, envDAO, keyDAO);
	}

	@Test
	public void stopAll() {
		doReturn(Collections.singleton(USER)).when(envDAO).fetchAllUsers();
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(getUserInstances());
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(RUNNING_STATE);
		when(edgeService.stop(any(UserInfo.class))).thenReturn(UUID);

		environmentService.stopAll();

		verify(envDAO).fetchAllUsers();
		verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
		verify(systemUserInfoService, times(3)).create(USER);
		verify(exploratoryService).stop(refEq(userInfo), eq(EXPLORATORY_NAME_1));
		verify(exploratoryService).stop(refEq(userInfo), eq(EXPLORATORY_NAME_2));
		verify(keyDAO, times(2)).getEdgeStatus(USER);
		verify(edgeService).stop(refEq(userInfo));
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(USER, Arrays.asList(UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE),
				UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
		verifyNoMoreInteractions(envDAO, keyDAO, exploratoryDAO, edgeService, exploratoryService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void stopAllWithWrongResourceState() {
		doReturn(Collections.singleton(USER)).when(envDAO).fetchAllUsers();
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg()))
				.thenReturn(getUserInstances());
		expectedException.expect(ResourceConflictException.class);

		environmentService.stopAll();
	}

	@Test
	public void stopAllWithEdgeStarting() {
		doReturn(Collections.singleton(USER)).when(envDAO).fetchAllUsers();
		when(keyDAO.getEdgeStatus(anyString())).thenReturn("starting");
		expectedException.expect(ResourceConflictException.class);

		environmentService.stopAll();
	}

	@Test
	public void stopAllWithoutEdge() {
		doReturn(Collections.singleton(USER)).when(envDAO).fetchAllUsers();
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(getUserInstances());
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STOPPED_STATE);
		when(edgeService.stop(any(UserInfo.class))).thenReturn(UUID);

		environmentService.stopAll();

		verify(envDAO).fetchAllUsers();
		verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
		verify(systemUserInfoService, times(2)).create(USER);
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
	public void stopEnvironment() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(getUserInstances());
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(RUNNING_STATE);
		when(edgeService.stop(any(UserInfo.class))).thenReturn(UUID);

		environmentService.stopEnvironment(USER);

		verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
		verify(systemUserInfoService, times(3)).create(USER);
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

		environmentService.stopEnvironment(USER);
	}

	@Test
	public void stopEnvironmentWithEdgeStarting() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn("starting");
		expectedException.expect(ResourceConflictException.class);

		environmentService.stopEnvironment(USER);
	}

	@Test
	public void stopEnvironmentWithoutEdge() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(getUserInstances());
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STOPPED_STATE);
		when(edgeService.stop(any(UserInfo.class))).thenReturn(UUID);

		environmentService.stopEnvironment(USER);

		verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
		verify(systemUserInfoService, times(2)).create(USER);
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
	public void stopEdge() {
		final UserInfo userInfo = getUserInfo();
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(RUNNING_STATE);
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(edgeService.stop(any(UserInfo.class))).thenReturn(UUID);

		environmentService.stopEdge(USER);

		verify(keyDAO).getEdgeStatus(USER);
		verify(systemUserInfoService).create(USER);
		verify(edgeService).stop(refEq(userInfo));
		verifyNoMoreInteractions(keyDAO, systemUserInfoService, edgeService);
	}

	@Test
	public void stopEdgeWhenItIsNotRunning() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn("starting");

		environmentService.stopEdge(USER);

		verify(keyDAO).getEdgeStatus(USER);
		verifyZeroInteractions(systemUserInfoService, edgeService);
		verifyNoMoreInteractions(keyDAO);
	}

	@Test
	public void stopExploratory() {
		final UserInfo userInfo = getUserInfo();
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn(UUID);

		environmentService.stopExploratory(USER, EXPLORATORY_NAME_1);

		verify(systemUserInfoService).create(USER);
		verify(exploratoryService).stop(refEq(userInfo), eq(EXPLORATORY_NAME_1));
		verifyNoMoreInteractions(systemUserInfoService, exploratoryService);
	}

	@Test
	public void stopComputational() {
		final UserInfo userInfo = getUserInfo();
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		doNothing().when(computationalService).stopSparkCluster(any(UserInfo.class), anyString(), anyString());

		environmentService.stopComputational(USER, EXPLORATORY_NAME_1, "compName");

		verify(systemUserInfoService).create(USER);
		verify(computationalService).stopSparkCluster(refEq(userInfo), eq(EXPLORATORY_NAME_1), eq("compName"));
		verifyNoMoreInteractions(systemUserInfoService, computationalService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void terminateAll() {
		doReturn(Collections.singleton(USER)).when(envDAO).fetchAllUsers();
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg()))
				.thenReturn(Collections.emptyList());
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.edgeNodeExist(anyString())).thenReturn(true);
		when(edgeService.terminate(any(UserInfo.class))).thenReturn(UUID);

		environmentService.terminateAll();

		verify(envDAO).fetchAllUsers();
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg());
		verify(systemUserInfoService).create(USER);
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
	public void terminateAllWithoutEdge() {
		doReturn(Collections.singleton(USER)).when(envDAO).fetchAllUsers();
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class),
				eq(UserInstanceStatus.CREATING), eq(UserInstanceStatus.STARTING))).thenReturn(Collections.emptyList());
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusNotIn(anyString(), eq(UserInstanceStatus.TERMINATED),
				eq(UserInstanceStatus.FAILED), eq(UserInstanceStatus.TERMINATING))).thenReturn(getUserInstances());
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.edgeNodeExist(anyString())).thenReturn(false);
		when(edgeService.terminate(any(UserInfo.class))).thenReturn(UUID);

		environmentService.terminateAll();

		verify(envDAO).fetchAllUsers();
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusNotIn(USER, UserInstanceStatus.TERMINATED,
				UserInstanceStatus.FAILED, UserInstanceStatus.TERMINATING);
		verify(systemUserInfoService, times(2)).create(USER);
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
	public void terminateAllWithWrongResourceState() {
		doReturn(Collections.singleton(USER)).when(envDAO).fetchAllUsers();
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg()))
				.thenReturn(getUserInstances());
		expectedException.expect(ResourceConflictException.class);

		environmentService.terminateAll();
	}

	@Test
	public void terminateAllWithEdgeStarting() {
		doReturn(Collections.singleton(USER)).when(envDAO).fetchAllUsers();
		when(keyDAO.getEdgeStatus(anyString())).thenReturn("starting");
		expectedException.expect(ResourceConflictException.class);

		environmentService.terminateAll();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void terminateEnvironment() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg()))
				.thenReturn(Collections.emptyList());
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.edgeNodeExist(anyString())).thenReturn(true);
		when(edgeService.terminate(any(UserInfo.class))).thenReturn(UUID);

		environmentService.terminateEnvironment(USER);

		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg());
		verify(systemUserInfoService).create(USER);
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
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.edgeNodeExist(anyString())).thenReturn(false);
		when(edgeService.terminate(any(UserInfo.class))).thenReturn(UUID);

		environmentService.terminateEnvironment(USER);

		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusNotIn(USER, UserInstanceStatus.TERMINATED,
				UserInstanceStatus.FAILED, UserInstanceStatus.TERMINATING);
		verify(systemUserInfoService, times(2)).create(USER);
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

		environmentService.terminateEnvironment(USER);
	}

	@Test
	public void terminateEnvironmentWithEdgeStarting() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn("starting");
		expectedException.expect(ResourceConflictException.class);

		environmentService.terminateEnvironment(USER);
	}

	@Test
	public void terminateExploratory() {
		final UserInfo userInfo = getUserInfo();
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn(UUID);

		environmentService.terminateExploratory(USER, EXPLORATORY_NAME_1);

		verify(systemUserInfoService).create(USER);
		verify(exploratoryService).terminate(refEq(userInfo), eq(EXPLORATORY_NAME_1));
		verifyNoMoreInteractions(systemUserInfoService, exploratoryService);
	}

	@Test
	public void terminateComputational() {
		final UserInfo userInfo = getUserInfo();
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		doNothing().when(computationalService)
				.terminateComputational(any(UserInfo.class), anyString(), anyString());

		environmentService.terminateComputational(USER, EXPLORATORY_NAME_1, "compName");

		verify(systemUserInfoService).create(USER);
		verify(computationalService)
				.terminateComputational(refEq(userInfo), eq(EXPLORATORY_NAME_1), eq("compName"));
		verifyNoMoreInteractions(systemUserInfoService, computationalService);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private List<UserInstanceDTO> getUserInstances() {
		return Arrays.asList(
				new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_1).withUser(USER).withProject("prj"),
				new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_2).withUser(USER).withProject("prj"));
	}
}