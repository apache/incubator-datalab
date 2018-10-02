/*
 * **************************************************************************
 *
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ***************************************************************************
 */

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.dao.UserGroupDao;
import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.resources.dto.UserGroupDto;
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
	@InjectMocks
	private UserGroupServiceImpl userRolesService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void createGroup() {
		when(userRoleDao.addGroupToRole(anySet(), anySet())).thenReturn(true);

		userRolesService.createGroup(GROUP, Collections.singleton(ROLE_ID), Collections.singleton(USER));

		verify(userRoleDao).addGroupToRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
		verify(userGroupDao).addUsers(GROUP, Collections.singleton(USER));
	}

	@Test
	public void createGroupWithNoUsers() {
		when(userRoleDao.addGroupToRole(anySet(), anySet())).thenReturn(true);

		userRolesService.createGroup(GROUP, Collections.singleton(ROLE_ID), Collections.emptySet());

		verify(userRoleDao).addGroupToRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
		verify(userGroupDao, never()).addUsers(anyString(), anySet());
	}

	@Test
	public void createGroupWhenRoleNotFound() {
		when(userRoleDao.addGroupToRole(anySet(), anySet())).thenReturn(false);

		expectedException.expect(ResourceNotFoundException.class);
		userRolesService.createGroup(GROUP, Collections.singleton(ROLE_ID), Collections.singleton(USER));
	}

	@Test
	public void getAggregatedRoles() {
		when(userRoleDao.aggregateRolesByGroup()).thenReturn(Collections.singletonList(getUserGroup()));

		final List<UserGroupDto> aggregatedRolesByGroup = userRolesService.getAggregatedRolesByGroup();

		assertEquals(1, aggregatedRolesByGroup.size());
		assertEquals(GROUP, aggregatedRolesByGroup.get(0).getGroup());
		assertTrue(aggregatedRolesByGroup.get(0).getRoles().isEmpty());

		verify(userRoleDao).aggregateRolesByGroup();
		verifyNoMoreInteractions(userRoleDao);
	}

	@Test
	public void addUserToGroup() {
		userRolesService.addUsersToGroup(GROUP, Collections.singleton(USER));

		verify(userGroupDao).addUsers(eq(GROUP), refEq(Collections.singleton(USER)));
		verifyNoMoreInteractions(userRoleDao, userGroupDao);
	}

	@Test
	public void addRolesToGroup() {
		when(userRoleDao.addGroupToRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(true);

		userRolesService.updateRolesForGroup(GROUP, Collections.singleton(ROLE_ID));

		verify(userRoleDao).addGroupToRole(refEq(Collections.singleton(GROUP)), refEq(Collections.singleton(ROLE_ID)));
		verify(userRoleDao).removeGroupWhenRoleNotIn(GROUP, Collections.singleton(ROLE_ID));
		verifyNoMoreInteractions(userRoleDao);
	}

	@Test
	public void removeUserFromGroup() {

		userRolesService.removeUserFromGroup(GROUP, USER);

		verify(userGroupDao).removeUser(GROUP, USER);
		verifyNoMoreInteractions(userGroupDao);
	}

	@Test
	public void removeGroupFromRole() {

		when(userRoleDao.removeGroupFromRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(true);

		userRolesService.removeGroupFromRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));

		verify(userRoleDao).removeGroupFromRole(refEq(Collections.singleton(GROUP)),
				refEq(Collections.singleton(ROLE_ID)));
		verifyNoMoreInteractions(userRoleDao);
	}

	@Test
	public void removeGroupFromRoleWithException() {
		when(userRoleDao.removeGroupFromRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(false);

		expectedException.expectMessage("Any of role : [" + ROLE_ID + "] were not found");
		expectedException.expect(ResourceNotFoundException.class);

		userRolesService.removeGroupFromRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
	}

	private UserGroupDto getUserGroup() {
		return new UserGroupDto(GROUP, Collections.emptyList(), Collections.emptySet());
	}
}