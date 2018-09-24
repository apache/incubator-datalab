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

import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.resources.dto.UserGroupDto;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
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
public class UserRolesServiceImplTest {

	private static final String ROLE_ID = "Role id";
	private static final String USER = "test";
	private static final String GROUP = "admin";
	@Mock
	private UserRoleDao dao;
	@InjectMocks
	private UserRolesServiceImpl userRolesService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getUserRoles() {
		when(dao.findAll()).thenReturn(Collections.singletonList(getUserRole()));
		final List<UserRoleDto> roles = userRolesService.getUserRoles();

		assertEquals(1, roles.size());
		assertEquals(ROLE_ID, roles.get(0).getId());

		verify(dao).findAll();
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void getAggregatedRoles() {
		when(dao.aggregateRolesByGroup()).thenReturn(Collections.singletonList(getUserGroup()));

		final List<UserGroupDto> aggregatedRolesByGroup = userRolesService.getAggregatedRolesByGroup();

		assertEquals(1, aggregatedRolesByGroup.size());
		assertEquals(GROUP, aggregatedRolesByGroup.get(0).getGroup());
		assertTrue(aggregatedRolesByGroup.get(0).getRoles().isEmpty());

		verify(dao).aggregateRolesByGroup();
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void createRole() {

		userRolesService.createRole(getUserRole());

		verify(dao).insert(refEq(getUserRole()));
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void updateRole() {
		when(dao.update(any())).thenReturn(true);
		userRolesService.updateRole(getUserRole());

		verify(dao).update(refEq(getUserRole()));
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void updateRoleWithException() {

		expectedException.expectMessage("Any of role : [" + ROLE_ID + "] were not found");
		expectedException.expect(ResourceNotFoundException.class);
		when(dao.update(any())).thenReturn(false);
		userRolesService.updateRole(getUserRole());
	}

	@Test
	public void removeRole() {

		userRolesService.removeRole(ROLE_ID);

		verify(dao).remove(ROLE_ID);
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void addUserToRole() {

		when(dao.addUserToRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(true);
		userRolesService.addUserToRole(Collections.singleton(USER), Collections.singleton(ROLE_ID));

		verify(dao).addUserToRole(refEq(Collections.singleton(USER)), refEq(Collections.singleton(ROLE_ID)));
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void addUserToRoleWithException() {
		expectedException.expectMessage("Any of role : [" + ROLE_ID + "] were not found");
		expectedException.expect(ResourceNotFoundException.class);
		when(dao.addUserToRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(false);
		userRolesService.addUserToRole(Collections.singleton(USER), Collections.singleton(ROLE_ID));
	}

	@Test
	public void addGroupToRole() {
		when(dao.addGroupToRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(true);
		userRolesService.addGroupToRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));

		verify(dao).addGroupToRole(refEq(Collections.singleton(GROUP)), refEq(Collections.singleton(ROLE_ID)));
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void addGroupToRoleWithException() {
		expectedException.expectMessage("Any of role : [" + ROLE_ID + "] were not found");
		expectedException.expect(ResourceNotFoundException.class);
		when(dao.addGroupToRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(false);
		userRolesService.addUserToRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
	}

	@Test
	public void removeUserFromRole() {
		when(dao.removeUserFromRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(true);

		userRolesService.removeUserFromRole(Collections.singleton(USER), Collections.singleton(ROLE_ID));

		verify(dao).removeUserFromRole(refEq(Collections.singleton(USER)), refEq(Collections.singleton(ROLE_ID)));
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void removeUserFromRoleWithException() {
		when(dao.removeUserFromRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(false);

		expectedException.expectMessage("Any of role : [" + ROLE_ID + "] were not found");
		expectedException.expect(ResourceNotFoundException.class);

		userRolesService.removeUserFromRole(Collections.singleton(USER), Collections.singleton(ROLE_ID));
	}

	@Test
	public void removeGroupFromRole() {

		when(dao.removeGroupFromRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(true);

		userRolesService.removeGroupFromRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));

		verify(dao).removeGroupFromRole(refEq(Collections.singleton(GROUP)), refEq(Collections.singleton(ROLE_ID)));
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void removeGroupFromRoleWithException() {
		when(dao.removeGroupFromRole(anySetOf(String.class), anySetOf(String.class))).thenReturn(false);

		expectedException.expectMessage("Any of role : [" + ROLE_ID + "] were not found");
		expectedException.expect(ResourceNotFoundException.class);

		userRolesService.removeGroupFromRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
	}

	private UserRoleDto getUserRole() {
		final UserRoleDto userRoleDto = new UserRoleDto();
		userRoleDto.setId(ROLE_ID);
		return userRoleDto;
	}

	private UserGroupDto getUserGroup() {
		return new UserGroupDto(GROUP, Collections.emptyList());
	}
}