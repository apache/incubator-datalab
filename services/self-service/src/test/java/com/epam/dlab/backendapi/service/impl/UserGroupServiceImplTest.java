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

import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.dao.UserGroupDao;
import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.resources.dto.UserGroupDto;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserGroupServiceImplTest {

	private static final String ROLE_ID = "Role id";
	private static final String USER = "test";
	private static final String GROUP = "admin";
	@Mock
	private UserRoleDao userRoleDao;
	@Mock
	private UserGroupDao userGroupDao;
	@Mock
	private ProjectDAO projectDAO;
	@InjectMocks
	private UserGroupServiceImpl userGroupService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void createGroup() {
		when(userRoleDao.addGroupToRole(anySet(), anySet())).thenReturn(true);

		userGroupService.createGroup(GROUP, Collections.singleton(ROLE_ID), Collections.singleton(USER));

		verify(userRoleDao).addGroupToRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
		verify(userGroupDao).addUsers(GROUP, Collections.singleton(USER));
	}

	@Test
	public void createGroupWithNoUsers() {
		when(userRoleDao.addGroupToRole(anySet(), anySet())).thenReturn(true);

		userGroupService.createGroup(GROUP, Collections.singleton(ROLE_ID), Collections.emptySet());

		verify(userRoleDao).addGroupToRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
		verify(userGroupDao, never()).addUsers(anyString(), anySet());
	}

	@Test
	public void createGroupWhenRoleNotFound() {
		when(userRoleDao.addGroupToRole(anySet(), anySet())).thenReturn(false);

		expectedException.expect(ResourceNotFoundException.class);
		userGroupService.createGroup(GROUP, Collections.singleton(ROLE_ID), Collections.singleton(USER));
	}

	@Test
	public void getAggregatedRoles() {
		when(userRoleDao.aggregateRolesByGroup()).thenReturn(Collections.singletonList(getUserGroup()));

		final List<UserGroupDto> aggregatedRolesByGroup = userGroupService.getAggregatedRolesByGroup();

		assertEquals(1, aggregatedRolesByGroup.size());
		assertEquals(GROUP, aggregatedRolesByGroup.get(0).getGroup());
		assertTrue(aggregatedRolesByGroup.get(0).getRoles().isEmpty());

		verify(userRoleDao).aggregateRolesByGroup();
		verifyNoMoreInteractions(userRoleDao);
	}

	@Test
	public void addUserToGroup() {
		userGroupService.addUsersToGroup(GROUP, Collections.singleton(USER));

		verify(userGroupDao).addUsers(eq(GROUP), refEq(Collections.singleton(USER)));
		verifyNoMoreInteractions(userRoleDao, userGroupDao);
	}

	@Test
	public void addRolesToGroup() {
		when(userRoleDao.addGroupToRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(true);

		userGroupService.updateRolesForGroup(GROUP, Collections.singleton(ROLE_ID));

		verify(userRoleDao).addGroupToRole(refEq(Collections.singleton(GROUP)), refEq(Collections.singleton(ROLE_ID)));
		verify(userRoleDao).removeGroupWhenRoleNotIn(GROUP, Collections.singleton(ROLE_ID));
		verifyNoMoreInteractions(userRoleDao);
	}

	@Test
	public void removeUserFromGroup() {

		userGroupService.removeUserFromGroup(GROUP, USER);

		verify(userGroupDao).removeUser(GROUP, USER);
		verifyNoMoreInteractions(userGroupDao);
	}

	@Test
	public void removeGroupFromRole() {

		when(userRoleDao.removeGroupFromRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(true);

		userGroupService.removeGroupFromRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));

		verify(userRoleDao).removeGroupFromRole(refEq(Collections.singleton(GROUP)),
				refEq(Collections.singleton(ROLE_ID)));
		verifyNoMoreInteractions(userRoleDao);
	}

	@Test
	public void removeGroupFromRoleWithException() {
		when(userRoleDao.removeGroupFromRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(false);

		expectedException.expectMessage("Any of role : [" + ROLE_ID + "] were not found");
		expectedException.expect(ResourceNotFoundException.class);

		userGroupService.removeGroupFromRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
	}

	@Test
	public void removeGroup() {

		when(userRoleDao.removeGroup(anyString())).thenReturn(true);
		final ProjectDTO projectDTO = new ProjectDTO(
				"name", Collections.emptySet(), "", "", null, Collections.emptyList(), true);
		when(projectDAO.getProjectsWithEndpointStatusNotIn(UserInstanceStatus.TERMINATED,
				UserInstanceStatus.TERMINATING)).thenReturn(Collections.singletonList(projectDTO));
		doNothing().when(userGroupDao).removeGroup(anyString());

		userGroupService.removeGroup(GROUP);

		verify(userRoleDao).removeGroup(GROUP);
		verify(userGroupDao).removeGroup(GROUP);
		verifyNoMoreInteractions(userGroupDao, userRoleDao);
	}

	@Test
	public void removeGroupWhenItIsUsedInProject() {

		when(userRoleDao.removeGroup(anyString())).thenReturn(true);
		when(projectDAO.getProjectsWithEndpointStatusNotIn(UserInstanceStatus.TERMINATED,
				UserInstanceStatus.TERMINATING)).thenReturn(Collections.singletonList( new ProjectDTO(
				"name", Collections.singleton(GROUP), "", "", null, Collections.emptyList(), true)));
		doNothing().when(userGroupDao).removeGroup(anyString());

		try {
			userGroupService.removeGroup(GROUP);
		} catch (Exception e){
			assertEquals("Group can not be removed because it is used in some project", e.getMessage());
		}

		verify(userRoleDao, never()).removeGroup(GROUP);
		verify(userGroupDao, never()).removeGroup(GROUP);
		verifyNoMoreInteractions(userGroupDao, userRoleDao);
	}

	@Test
	public void removeGroupWhenGroupNotExist() {

		final ProjectDTO projectDTO = new ProjectDTO(
				"name", Collections.emptySet(), "", "", null, Collections.emptyList(), true);
		when(projectDAO.getProjectsWithEndpointStatusNotIn(UserInstanceStatus.TERMINATED,
				UserInstanceStatus.TERMINATING)).thenReturn(Collections.singletonList(projectDTO));
		when(userRoleDao.removeGroup(anyString())).thenReturn(false);
		doNothing().when(userGroupDao).removeGroup(anyString());

		userGroupService.removeGroup(GROUP);

		verify(userRoleDao).removeGroup(GROUP);
		verify(userGroupDao).removeGroup(GROUP);
		verifyNoMoreInteractions(userGroupDao, userRoleDao);
	}

	@Test
	public void removeGroupWithException() {
		final ProjectDTO projectDTO = new ProjectDTO(
				"name", Collections.emptySet(), "", "", null, Collections.emptyList(), true);
		when(projectDAO.getProjectsWithEndpointStatusNotIn(UserInstanceStatus.TERMINATED,
				UserInstanceStatus.TERMINATING)).thenReturn(Collections.singletonList(projectDTO));
		when(userRoleDao.removeGroup(anyString())).thenThrow(new DlabException("Exception"));

		expectedException.expectMessage("Exception");
		expectedException.expect(DlabException.class);

		userGroupService.removeGroup(GROUP);
	}

	@Test
	public void updateGroup() {
		userGroupService.updateGroup(GROUP, Collections.singleton(ROLE_ID), Collections.singleton(USER));

		verify(userGroupDao).updateUsers(GROUP, Collections.singleton(USER));
		verify(userRoleDao).removeGroupWhenRoleNotIn(GROUP, Collections.singleton(ROLE_ID));
		verify(userRoleDao).addGroupToRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
		verifyNoMoreInteractions(userRoleDao, userGroupDao);
	}

	private UserGroupDto getUserGroup() {
		return new UserGroupDto(GROUP, Collections.emptyList(), Collections.emptySet());
	}
}