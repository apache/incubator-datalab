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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.dao.UserGroupDao;
import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.resources.dto.UserGroupDto;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.service.UserGroupService;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class UserGroupServiceImpl implements UserGroupService {
	private static final String ROLE_NOT_FOUND_MSG = "Any of role : %s were not found";
	private static final String ADMIN = "admin";
	private static final String PROJECT_ADMIN = "projectAdmin";

	@Inject
	private UserGroupDao userGroupDao;
	@Inject
	private UserRoleDao userRoleDao;
	@Inject
	private ProjectDAO projectDAO;
	@Inject
	private ProjectService projectService;

	@Override
	public void createGroup(String group, Set<String> roleIds, Set<String> users) {
		checkAnyRoleFound(roleIds, userRoleDao.addGroupToRole(Collections.singleton(group), roleIds));
		log.debug("Adding users {} to group {}", users, group);
		userGroupDao.addUsers(group, users);
	}

	@Override
	public void updateGroup(UserInfo user, String group, Set<String> roleIds, Set<String> users) {
		if (UserRoles.isAdmin(user)) {
			updateGroup(group, roleIds, users);
		} else if (UserRoles.isProjectAdmin(user)) {
			projectService.getProjects(user)
					.stream()
					.map(ProjectDTO::getGroups)
					.flatMap(Collection::stream)
					.filter(g -> g.equalsIgnoreCase(group))
					.findAny()
					.orElseThrow(() -> new DlabException(String.format("User %s doesn't have appropriate permission", user.getName())));
			updateGroup(group, roleIds, users);
		} else {
			throw new DlabException(String.format("User %s doesn't have appropriate permission", user.getName()));
		}
	}

	@Override
	public void removeGroup(String groupId) {
		if (projectDAO.getProjectsWithEndpointStatusNotIn(UserInstanceStatus.TERMINATED,
				UserInstanceStatus.TERMINATING)
				.stream()
				.map(ProjectDTO::getGroups)
				.noneMatch(groups -> groups.contains(groupId))) {
			userRoleDao.removeGroup(groupId);
			userGroupDao.removeGroup(groupId);
		} else {
			throw new ResourceConflictException("Group can not be removed because it is used in some project");
		}
	}

	@Override
	public List<UserGroupDto> getAggregatedRolesByGroup(UserInfo user) {
		if (UserRoles.isAdmin(user)) {
			return userRoleDao.aggregateRolesByGroup();
		} else if (UserRoles.isProjectAdmin(user)) {
			Set<String> groups = projectService.getProjects(user)
					.stream()
					.map(ProjectDTO::getGroups)
					.flatMap(Collection::stream)
					.collect(Collectors.toSet());
			return userRoleDao.aggregateRolesByGroup()
					.stream()
					.filter(userGroup -> groups.contains(userGroup.getGroup()) && !containsAdministrationPermissions(userGroup))
					.collect(Collectors.toList());
		} else {
			throw new DlabException(String.format("User %s doesn't have appropriate permission", user.getName()));
		}
	}

	private boolean containsAdministrationPermissions(UserGroupDto userGroup) {
		List<String> ids = userGroup.getRoles()
				.stream()
				.map(UserRoleDto::getId)
				.collect(Collectors.toList());
		return ids.contains(ADMIN) || ids.contains(PROJECT_ADMIN);
	}

	private void updateGroup(String group, Set<String> roleIds, Set<String> users) {
		log.debug("Updating users for group {}: {}", group, users);
		userGroupDao.updateUsers(group, users);
		log.debug("Removing group {} from existing roles", group);
		userRoleDao.removeGroupWhenRoleNotIn(group, roleIds);
		log.debug("Adding group {} to roles {}", group, roleIds);
		userRoleDao.addGroupToRole(Collections.singleton(group), roleIds);
	}

	private void checkAnyRoleFound(Set<String> roleIds, boolean anyRoleFound) {
		if (!anyRoleFound) {
			throw new ResourceNotFoundException(String.format(ROLE_NOT_FOUND_MSG, roleIds));
		}
	}
}
