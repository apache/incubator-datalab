/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.epam.dlab.backendapi.service.UserRolesService;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Singleton
public class UserRolesServiceImpl implements UserRolesService {

	private static final String ROLE_NOT_FOUND_MSG = "Any of role : %s were not found";
	@Inject
	private UserRoleDao userRoleDao;

	@Override
	public List<UserRoleDto> getUserRoles() {
		return userRoleDao.getUserRoles();
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

	@Override
	public void addUserToRole(Set<String> users, Set<String> roleIds) {
		checkAnyRoleFound(roleIds, userRoleDao.addUserToRole(users, roleIds));
	}

	@Override
	public void addGroupToRole(Set<String> groups, Set<String> roleIds) {
		checkAnyRoleFound(roleIds, userRoleDao.addGroupToRole(groups, roleIds));
	}

	@Override
	public void removeUserFromRole(Set<String> users, Set<String> roleIds) {
		checkAnyRoleFound(roleIds, userRoleDao.removeUserFromRole(users, roleIds));
	}

	@Override
	public void removeGroupFromRole(Set<String> groups, Set<String> roleIds) {
		checkAnyRoleFound(roleIds, userRoleDao.removeGroupFromRole(groups, roleIds));
	}

	private void checkAnyRoleFound(Set<String> roleIds, boolean anyRoleFound) {
		if (!anyRoleFound) {
			throw new ResourceNotFoundException(String.format(ROLE_NOT_FOUND_MSG, roleIds));
		}
	}


}
