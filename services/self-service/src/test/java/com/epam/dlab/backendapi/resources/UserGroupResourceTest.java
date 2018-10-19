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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.backendapi.resources.dto.CreateGroupDto;
import com.epam.dlab.backendapi.resources.dto.UpdateRoleGroupDto;
import com.epam.dlab.backendapi.resources.dto.UpdateUserGroupDto;
import com.epam.dlab.backendapi.resources.dto.UserGroupDto;
import com.epam.dlab.backendapi.service.UserGroupService;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class UserGroupResourceTest extends TestBase {

	private static final String USER = "user";
	private static final String ROLE_ID = "id";
	private static final String GROUP = "group";
	private UserGroupService rolesService = mock(UserGroupService.class);

	@Before
	public void setup() throws AuthenticationException {
		authSetup();
	}

	@Rule
	public final ResourceTestRule resources =
			getResourceTestRuleInstance(new UserGroupResource(rolesService));

	@Test
	public void createGroup() {

		final Response response = resources.getJerseyTest()
				.target("/group")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getCreateGroupDto(GROUP, Collections.singleton(ROLE_ID))));

		assertEquals(HttpStatus.SC_OK, response.getStatus());

		verify(rolesService).createGroup(GROUP, Collections.singleton(ROLE_ID), Collections.singleton(USER));
		verifyNoMoreInteractions(rolesService);
	}

	@Test
	public void createGroupWhenGroupNameIsEmpty() {

		final Response response = resources.getJerseyTest()
				.target("/group")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getCreateGroupDto("", Collections.singleton(ROLE_ID))));

		assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

		verifyZeroInteractions(rolesService);
	}

	@Test
	public void createGroupWhenRoleIdIsEmpty() {

		final Response response = resources.getJerseyTest()
				.target("/group")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getCreateGroupDto(GROUP, Collections.emptySet())));

		assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

		verifyZeroInteractions(rolesService);
	}

	@Test
	public void getGroups() {
		when(rolesService.getAggregatedRolesByGroup()).thenReturn(Collections.singletonList(getUserGroup()));

		final Response response = resources.getJerseyTest()
				.target("/group")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		final List<UserGroupDto> actualRoles = response.readEntity(new GenericType<List<UserGroupDto>>() {
		});

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(GROUP, actualRoles.get(0).getGroup());
		assertTrue(actualRoles.get(0).getRoles().isEmpty());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(rolesService).getAggregatedRolesByGroup();
		verifyNoMoreInteractions(rolesService);
	}

	@Test
	public void addRolesToGroup() {

		final Response response = resources.getJerseyTest()
				.target("/group/role")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(new UpdateRoleGroupDto(singleton(ROLE_ID), GROUP)));

		assertEquals(HttpStatus.SC_OK, response.getStatus());

		verify(rolesService).updateRolesForGroup(GROUP, singleton(ROLE_ID));
		verifyNoMoreInteractions(rolesService);
	}

	@Test
	public void addRolesToGroupWithValidationException() {

		final Response response = resources.getJerseyTest()
				.target("/group/role")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(new UpdateRoleGroupDto(singleton(ROLE_ID), StringUtils.EMPTY)));

		assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

		verifyZeroInteractions(rolesService);
	}

	@Test
	public void deleteGroupFromRole() {
		final Response response = resources.getJerseyTest()
				.target("/group/role")
				.queryParam("group", GROUP)
				.queryParam("roleId", ROLE_ID)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_OK, response.getStatus());


		verify(rolesService).removeGroupFromRole(singleton(GROUP), singleton(ROLE_ID));
		verifyNoMoreInteractions(rolesService);
	}

	@Test
	public void deleteGroup() {
		final Response response = resources.getJerseyTest()
				.target("/group/" + GROUP)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_OK, response.getStatus());


		verify(rolesService).removeGroup(GROUP);
		verifyNoMoreInteractions(rolesService);
	}

	@Test
	public void deleteGroupFromRoleWithValidationException() {
		final Response response = resources.getJerseyTest()
				.target("/group/role")
				.queryParam("group", GROUP)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

		verifyZeroInteractions(rolesService);
	}

	@Test
	public void addUserToGroup() {
		final Response response = resources.getJerseyTest()
				.target("/group/user")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(new UpdateUserGroupDto(GROUP, singleton(USER))));

		assertEquals(HttpStatus.SC_OK, response.getStatus());

		verify(rolesService).addUsersToGroup(GROUP, singleton(USER));
		verifyNoMoreInteractions(rolesService);
	}

	@Test
	public void addUserToGroupWithValidationException() {
		final Response response = resources.getJerseyTest()
				.target("/group/user")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.put(Entity.json(new UpdateUserGroupDto(StringUtils.EMPTY, singleton(USER))));

		assertEquals(HttpStatus.SC_UNPROCESSABLE_ENTITY, response.getStatus());

		verifyZeroInteractions(rolesService);
	}

	@Test
	public void deleteUserFromGroup() {
		final Response response = resources.getJerseyTest()
				.target("/group/user")
				.queryParam("user", USER)
				.queryParam("group", GROUP)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_OK, response.getStatus());


		verify(rolesService).removeUserFromGroup(GROUP, USER);
		verifyNoMoreInteractions(rolesService);
	}

	@Test
	public void deleteUserFromGroupWithValidationException() {
		final Response response = resources.getJerseyTest()
				.target("/group/user")
				.queryParam("group", GROUP)
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.delete();

		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());

		verifyZeroInteractions(rolesService);
	}

	private UserGroupDto getUserGroup() {
		return new UserGroupDto(GROUP, Collections.emptyList(), Collections.emptySet());
	}

	private CreateGroupDto getCreateGroupDto(String group, Set<String> roleIds) {
		final CreateGroupDto dto = new CreateGroupDto();
		dto.setName(group);
		dto.setRoleIds(roleIds);
		dto.setUsers(Collections.singleton(USER));
		return dto;
	}


}