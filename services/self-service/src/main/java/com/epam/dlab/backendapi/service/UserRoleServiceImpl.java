package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Singleton
public class UserRoleServiceImpl implements UserRoleService {
	private static final String ROLE_NOT_FOUND_MSG = "Any of role : %s were not found";
	@Inject
	private UserRoleDao userRoleDao;

	@Override
	public List<UserRoleDto> getUserRoles() {
		return userRoleDao.findAll();
	}

	@Override
	public void createRole(UserRoleDto dto) {
		userRoleDao.insert(dto);
	}

	@Override
	public void updateRole(UserRoleDto dto) {
		checkAnyRoleFound(Collections.singleton(dto.getId()), userRoleDao.update(dto));
	}

	@Override
	public void removeRole(String roleId) {
		userRoleDao.remove(roleId);
	}

	private void checkAnyRoleFound(Set<String> roleIds, boolean anyRoleFound) {
		if (!anyRoleFound) {
			throw new ResourceNotFoundException(String.format(ROLE_NOT_FOUND_MSG, roleIds));
		}
	}
}
