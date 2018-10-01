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

import com.epam.dlab.backendapi.dao.UserGroupDao;
import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.resources.dto.UserGroupDto;
import com.epam.dlab.backendapi.service.UserGroupService;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Singleton
@Slf4j
public class UserGroupServiceImpl implements UserGroupService {

	private static final String ROLE_NOT_FOUND_MSG = "Any of role : %s were not found";
	@Inject
	private UserGroupDao userGroupDao;
	@Inject
	private UserRoleDao userRoleDao;

	@Override
	public void createGroup(String group, Set<String> roleIds, Set<String> users) {
		checkAnyRoleFound(roleIds, userRoleDao.addGroupToRole(Collections.singleton(group), roleIds));
		if (!users.isEmpty()) {
			log.debug("Adding users {} to group {}", users, group);
			userGroupDao.addUsers(group, users);
		}
	}

	@Override
	public void addUsersToGroup(String group, Set<String> users) {
		userGroupDao.addUsers(group, users);
	}

	@Override
	public void updateRolesForGroup(String group, Set<String> roleIds) {
		userRoleDao.removeGroupWhenRoleNotIn(group, roleIds);
		checkAnyRoleFound(roleIds, userRoleDao.addGroupToRole(Collections.singleton(group), roleIds));
	}

	@Override
	public void removeUserFromGroup(String group, String user) {
		userGroupDao.removeUser(group, user);
	}

	@Override
	public void removeGroupFromRole(Set<String> groups, Set<String> roleIds) {
		checkAnyRoleFound(roleIds, userRoleDao.removeGroupFromRole(groups, roleIds));
	}

	@Override
	public List<UserGroupDto> getAggregatedRolesByGroup() {
		return userRoleDao.aggregateRolesByGroup();
	}

	private void checkAnyRoleFound(Set<String> roleIds, boolean anyRoleFound) {
		if (!anyRoleFound) {
			throw new ResourceNotFoundException(String.format(ROLE_NOT_FOUND_MSG, roleIds));
		}
	}


}
