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
package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.resources.TestBase;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserRoleServiceImplTest extends TestBase {

    private static final String ROLE_ID = "roleId";
    @Mock
    private UserRoleDao dao;
    @InjectMocks
    private UserRoleServiceImpl userRoleService;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
	public void createRole() {

		userRoleService.createRole(getUserRole());

		verify(dao).insert(refEq(getUserRole()));
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void updateRole() {
		when(dao.update(any())).thenReturn(true);
		userRoleService.updateRole(getUserRole());

		verify(dao).update(refEq(getUserRole()));
		verifyNoMoreInteractions(dao);
	}

	@Test
	public void updateRoleWithException() {

		expectedException.expectMessage("Any of role : [" + ROLE_ID + "] were not found");
		expectedException.expect(ResourceNotFoundException.class);
		when(dao.update(any())).thenReturn(false);
		userRoleService.updateRole(getUserRole());
	}

	@Test
	public void removeRole() {

		userRoleService.removeRole(ROLE_ID);

		verify(dao).remove(ROLE_ID);
		verifyNoMoreInteractions(dao);
	}

	private UserRoleDto getUserRole() {
		final UserRoleDto userRoleDto = new UserRoleDto();
		userRoleDto.setId(ROLE_ID);
		return userRoleDto;
	}
}