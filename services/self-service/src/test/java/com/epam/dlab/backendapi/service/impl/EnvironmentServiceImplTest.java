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
import com.epam.dlab.backendapi.dao.UserSettingsDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.service.SecurityService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyVararg;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentServiceImplTest {

	private static final String USER = "test";
	private static final String EXPLORATORY_NAME_1 = "expName1";
	private static final String EXPLORATORY_NAME_2 = "expName2";
	private static final String TOKEN = "token";
	private static final String UUID = "213-12312-321";
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
		when(exploratoryService.stop(any(UserInfo.class), anyString(), anyString())).thenReturn(UUID);

		environmentService.stopEnvironment(userInfo, USER, PROJECT_NAME);

		verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
		verify(exploratoryService).stop(refEq(userInfo), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1));
		verify(exploratoryService).stop(refEq(userInfo), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_2));
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(USER, Arrays.asList(UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE),
				UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
		verifyNoMoreInteractions(exploratoryDAO, exploratoryService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void stopEnvironmentWithWrongResourceState() {
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), any(List.class), anyVararg()))
				.thenReturn(getUserInstances());
		expectedException.expect(ResourceConflictException.class);

		environmentService.stopEnvironment(getUserInfo(), USER, PROJECT_NAME);
	}

	@Test
	public void stopEnvironmentWithoutEdge() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(getUserInstances());
		when(exploratoryService.stop(any(UserInfo.class), anyString(), anyString())).thenReturn(UUID);

		environmentService.stopEnvironment(userInfo, USER, PROJECT_NAME);

		verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
		verify(exploratoryService).stop(refEq(userInfo), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1));
		verify(exploratoryService).stop(refEq(userInfo), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_2));
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(USER, Arrays.asList(UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE),
				UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
		verifyNoMoreInteractions(envDAO, exploratoryDAO, exploratoryService);
	}

	@Test
	public void stopProjectEnvironment() {
		final UserInfo userInfo = getUserInfo();
		final ProjectDTO projectDTO = getProjectDTO();
		when(exploratoryDAO.fetchRunningExploratoryFieldsForProject(anyString())).thenReturn(getUserInstances());
		when(securityService.getServiceAccountInfo(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString(), anyString())).thenReturn(UUID);
		when(projectService.get(anyString())).thenReturn(projectDTO);
		doNothing().when(projectService).stop(any(UserInfo.class), anyString(), anyString());

		environmentService.stopProjectEnvironment(PROJECT_NAME);

		verify(exploratoryDAO).fetchRunningExploratoryFieldsForProject(PROJECT_NAME);
		verify(exploratoryService).stop(refEq(userInfo), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1));
		verify(exploratoryService).stop(refEq(userInfo), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_2));
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
	public void stopExploratory() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryService.stop(any(UserInfo.class), anyString(), anyString())).thenReturn(UUID);

		environmentService.stopExploratory(new UserInfo(USER, TOKEN), USER, PROJECT_NAME, EXPLORATORY_NAME_1);

		verify(exploratoryService).stop(refEq(userInfo), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1));
		verifyNoMoreInteractions(securityService, exploratoryService);
	}

	@Test
	public void stopComputational() {
		final UserInfo userInfo = getUserInfo();
		doNothing().when(computationalService).stopSparkCluster(any(UserInfo.class), anyString(), anyString(), anyString());

		environmentService.stopComputational(userInfo, USER, PROJECT_NAME, EXPLORATORY_NAME_1, "compName");

		verify(computationalService).stopSparkCluster(refEq(userInfo), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1), eq("compName"));
		verifyNoMoreInteractions(securityService, computationalService);
	}

	@Test
	public void terminateExploratory() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryService.terminate(any(UserInfo.class), anyString(), anyString())).thenReturn(UUID);

		environmentService.terminateExploratory(userInfo, USER, PROJECT_NAME, EXPLORATORY_NAME_1);

		verify(exploratoryService).terminate(refEq(userInfo), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1));
		verifyNoMoreInteractions(securityService, exploratoryService);
	}

	@Test
	public void terminateComputational() {
		final UserInfo userInfo = getUserInfo();
		doNothing().when(computationalService)
				.terminateComputational(any(UserInfo.class), anyString(), anyString(), anyString());

		environmentService.terminateComputational(userInfo, USER, PROJECT_NAME, EXPLORATORY_NAME_1, "compName");

		verify(computationalService)
				.terminateComputational(refEq(userInfo), eq(PROJECT_NAME), eq(EXPLORATORY_NAME_1), eq("compName"));
		verifyNoMoreInteractions(securityService, computationalService);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private List<UserInstanceDTO> getUserInstances() {
		return Arrays.asList(
				new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_1).withUser(USER).withProject(PROJECT_NAME),
				new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_2).withUser(USER).withProject(PROJECT_NAME));
	}

	private ProjectDTO getProjectDTO() {
		return new ProjectDTO(PROJECT_NAME, Collections.emptySet(), "", "", null,
				Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING,
						new EdgeInfo())), true);
	}
}