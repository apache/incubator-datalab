package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EnvStatusDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceConflictException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
	private EnvStatusDAO envStatusDAO;
	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private SystemUserInfoService systemUserInfoService;
	@Mock
	private ExploratoryService exploratoryService;
	@Mock
	private EdgeService edgeService;
	@Mock
	private KeyDAO keyDAO;

	@InjectMocks
	private EnvironmentServiceImpl environmentService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getActiveUsers() {
		doReturn(Collections.singleton(USER)).when(envStatusDAO).fetchActiveEnvUsers();
		final Set<String> activeUsers = environmentService.getActiveUsers();

		assertEquals(1, activeUsers.size());
		assertTrue(activeUsers.contains(USER));

		verify(envStatusDAO).fetchActiveEnvUsers();
		verifyNoMoreInteractions(envStatusDAO);
	}

	@Test
	public void getActiveUsersWithException() {
		doThrow(new DlabException("User not found")).when(envStatusDAO).fetchActiveEnvUsers();

		expectedException.expect(DlabException.class);
		expectedException.expectMessage("User not found");

		environmentService.getActiveUsers();
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
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(userInfo.getName(), UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING);
		verifyNoMoreInteractions(keyDAO, exploratoryDAO, edgeService, exploratoryService);
	}

	@Test
	public void stopEnvironmentWithWrongResourceState() {
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), Matchers.anyVararg())).
				thenReturn(getUserInstances());
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
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(userInfo.getName(), UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING);
		verifyNoMoreInteractions(keyDAO, envStatusDAO, exploratoryDAO, edgeService, exploratoryService);
	}

	@Test
	public void terminateEnvironment() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIncludedOrExcluded(anyBoolean(), anyString()))
				.thenReturn(getUserInstances());
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.edgeNodeExist(anyString())).thenReturn(true);
		when(edgeService.terminate(any(UserInfo.class))).thenReturn(UUID);

		environmentService.terminateEnvironment(USER);

		verify(exploratoryDAO, never()).fetchUserExploratoriesWhereStatusIncludedOrExcluded(anyBoolean(), anyString(),
				any());
		verify(systemUserInfoService).create(USER);
		verify(keyDAO).edgeNodeExist(USER);
		verify(edgeService).terminate(refEq(userInfo));
		verify(exploratoryService).updateExploratoryStatuses(USER, UserInstanceStatus.TERMINATING);
		verify(keyDAO).getEdgeStatus(userInfo.getName());
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(userInfo.getName(), UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING);
		verifyNoMoreInteractions(keyDAO, envStatusDAO, exploratoryDAO, edgeService, exploratoryService);
	}

	@Test
	public void terminateEnvironmentWithoutEdge() {
		final UserInfo userInfo = getUserInfo();
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIncludedOrExcluded(anyBoolean(), anyString(),
				Matchers.<UserInstanceStatus>anyVararg())).thenReturn(getUserInstances());
		when(systemUserInfoService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn(UUID);
		when(keyDAO.edgeNodeExist(anyString())).thenReturn(false);
		when(edgeService.terminate(any(UserInfo.class))).thenReturn(UUID);

		environmentService.terminateEnvironment(USER);

		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIncludedOrExcluded(false, USER,
				UserInstanceStatus.TERMINATED, UserInstanceStatus.FAILED, UserInstanceStatus.TERMINATING);
		verify(systemUserInfoService, times(2)).create(USER);
		verify(exploratoryService).terminate(refEq(userInfo), eq(EXPLORATORY_NAME_1));
		verify(exploratoryService).terminate(refEq(userInfo), eq(EXPLORATORY_NAME_2));
		verify(keyDAO).edgeNodeExist(USER);
		verify(edgeService, never()).terminate(refEq(userInfo));
		verify(keyDAO).getEdgeStatus(userInfo.getName());
		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(userInfo.getName(), UserInstanceStatus.CREATING,
				UserInstanceStatus.STARTING);
		verifyNoMoreInteractions(keyDAO, envStatusDAO, exploratoryDAO, edgeService, exploratoryService);
	}

	@Test
	public void terminateEnvironmentWithWrongResourceState() {
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), Matchers.anyVararg())).
				thenReturn(getUserInstances());
		expectedException.expect(ResourceConflictException.class);

		environmentService.terminateEnvironment(USER);
	}

	@Test
	public void terminateEnvironmentWithEdgeStarting() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn("starting");
		expectedException.expect(ResourceConflictException.class);

		environmentService.terminateEnvironment(USER);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private List<UserInstanceDTO> getUserInstances() {
		return Arrays.asList(
				new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_1).withUser(USER),
				new UserInstanceDTO().withExploratoryName(EXPLORATORY_NAME_2).withUser(USER));
	}
}