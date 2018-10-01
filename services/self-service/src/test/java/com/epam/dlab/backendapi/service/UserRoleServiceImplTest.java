package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.dao.UserRoleDao;
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
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserRoleServiceImplTest {

	private static final String ROLE_ID = "roleId";
	@Mock
	private UserRoleDao dao;
	@InjectMocks
	private UserRoleServiceImpl userRoleService;
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getUserRoles() {
		when(dao.findAll()).thenReturn(Collections.singletonList(getUserRole()));
		final List<UserRoleDto> roles = userRoleService.getUserRoles();

		assertEquals(1, roles.size());
		assertEquals(ROLE_ID, roles.get(0).getId());

		verify(dao).findAll();
		verifyNoMoreInteractions(dao);
	}


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