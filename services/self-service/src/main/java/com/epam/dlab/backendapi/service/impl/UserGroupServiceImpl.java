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
import com.epam.dlab.backendapi.annotation.Audit;
import com.epam.dlab.backendapi.annotation.ResourceName;
import com.epam.dlab.backendapi.annotation.User;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.dao.UserGroupDao;
import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.domain.AuditDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.resources.dto.UserGroupDto;
import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.AuditService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.dlab.backendapi.domain.AuditActionEnum.CREATE_GROUP;
import static com.epam.dlab.backendapi.domain.AuditActionEnum.DELETE_GROUP;
import static com.epam.dlab.backendapi.domain.AuditActionEnum.UPDATE_GROUP;

@Singleton
@Slf4j
public class UserGroupServiceImpl implements UserGroupService {
	private static final String AUDIT_ADD_ROLE_MESSAGE = "Added role(s): %s.\n";
	private static final String AUDIT_REMOVE_ROLE_MESSAGE = "Removed role(s): %s.\n";
	private static final String AUDIT_ADD_USER_MESSAGE = "Added user(s): %s.\n";
	private static final String AUDIT_REMOVE_USER_MESSAGE = "Removed user(s): %s.\n";
	private static final String ROLE_NOT_FOUND_MSG = "Any of role : %s were not found";
	private static final String ADMIN = "admin";
	private static final String PROJECT_ADMIN = "projectAdmin";

	private final UserGroupDao userGroupDao;
	private final UserRoleDao userRoleDao;
	private final ProjectDAO projectDAO;
	private final ProjectService projectService;
	private final AuditService auditService;
	private final SelfServiceApplicationConfiguration configuration;

	@Inject
	public UserGroupServiceImpl(UserGroupDao userGroupDao, UserRoleDao userRoleDao, ProjectDAO projectDAO, ProjectService projectService, AuditService auditService,
								SelfServiceApplicationConfiguration configuration) {
		this.userGroupDao = userGroupDao;
		this.userRoleDao = userRoleDao;
		this.projectDAO = projectDAO;
		this.projectService = projectService;
		this.auditService = auditService;
		this.configuration = configuration;
	}

	@Audit(action = CREATE_GROUP)
	@Override
	public void createGroup(@User UserInfo userInfo, @ResourceName String group, Set<String> roleIds, Set<String> users) {
		checkAnyRoleFound(roleIds, userRoleDao.addGroupToRole(Collections.singleton(group), roleIds));
		log.debug("Adding users {} to group {}", users, group);
		userGroupDao.addUsers(group, users);
	}

	@Override
	public void updateGroup(UserInfo userInfo, String group, Map<String, String> roles, Set<String> users) {
		if (UserRoles.isAdmin(userInfo)) {
			updateGroup(userInfo.getName(), group, roles, users);
		} else if (UserRoles.isProjectAdmin(userInfo)) {
			projectService.getProjects(userInfo)
					.stream()
					.map(ProjectDTO::getGroups)
					.flatMap(Collection::stream)
					.filter(g -> g.equalsIgnoreCase(group))
					.findAny()
					.orElseThrow(() -> new DlabException(String.format("User %s doesn't have appropriate permission", userInfo.getName())));
			updateGroup(userInfo.getName(), group, roles, users);
		} else {
			throw new DlabException(String.format("User %s doesn't have appropriate permission", userInfo.getName()));
		}
	}

	@Audit(action = DELETE_GROUP)
	@Override
	public void removeGroup(@User UserInfo userInfo, @ResourceName String groupId) {
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

	private void updateGroup(String user, String group, Map<String, String> roles, Set<String> users) {
		Set<String> roleIds = roles.keySet();
		if (configuration.isAuditEnabled()) {
			audit(user, group, roles, users);
		}
		log.debug("Updating users for group {}: {}", group, users);
		userGroupDao.updateUsers(group, users);
		log.debug("Removing group {} from existing roles", group);
		userRoleDao.removeGroupWhenRoleNotIn(group, roleIds);
		log.debug("Adding group {} to roles {}", group, roleIds);
		userRoleDao.addGroupToRole(Collections.singleton(group), roleIds);
	}

	private void audit(String user, String group, Map<String, String> newRoles, Set<String> users) {
		final String auditInfo = roleAudit(group, newRoles) + getUserAudit(group, users);
		AuditDTO auditDTO = AuditDTO.builder()
				.user(user)
				.resourceName(group)
				.action(UPDATE_GROUP)
				.info(auditInfo)
				.build();
		auditService.save(auditDTO);
	}

	private String getUserAudit(String group, Set<String> users) {
		StringBuilder auditInfo = new StringBuilder();
		Set<String> oldUsers = userGroupDao.getUsers(group);
		HashSet<String> newUsers = new HashSet<>(users);
		newUsers.removeAll(oldUsers);
		if (!newUsers.isEmpty()) {
			auditInfo.append(String.format(AUDIT_ADD_USER_MESSAGE, String.join(", ", newUsers)));
		}
		HashSet<String> removedUsers = new HashSet<>(oldUsers);
		removedUsers.removeAll(users);
		if (!removedUsers.isEmpty()) {
			auditInfo.append(String.format(AUDIT_REMOVE_USER_MESSAGE, String.join(", ", removedUsers)));
		}
		return auditInfo.toString();
	}

	private String roleAudit(String group, Map<String, String> newRoles) {
		StringBuilder auditInfo = new StringBuilder();
		Map<String, String> oldRoles = userRoleDao.aggregateRolesByGroup()
				.stream()
				.filter(g -> g.getGroup().equals(group))
				.map(UserGroupDto::getRoles)
				.flatMap(Collection::stream)
				.collect(Collectors.toMap(UserRoleDto::getId, UserRoleDto::getDescription));
		if (!getRoleDescription(oldRoles, newRoles).isEmpty()) {
			auditInfo.append(String.format(AUDIT_ADD_ROLE_MESSAGE, getRoleDescription(oldRoles, newRoles)));
		}
		if (!getRoleDescription(newRoles, oldRoles).isEmpty()) {
			auditInfo.append(String.format(AUDIT_REMOVE_ROLE_MESSAGE, getRoleDescription(newRoles, oldRoles)));
		}
		return auditInfo.toString();
	}

	private String getRoleDescription(Map<String, String> newRoles, Map<String, String> oldRoles) {
		Set<String> removedRoleIds = new HashSet<>(oldRoles.keySet());
		removedRoleIds.removeAll(newRoles.keySet());
		return removedRoleIds
				.stream()
				.map(oldRoles::get)
				.collect(Collectors.joining(", "));
	}

	private void checkAnyRoleFound(Set<String> roleIds, boolean anyRoleFound) {
		if (!anyRoleFound) {
			throw new ResourceNotFoundException(String.format(ROLE_NOT_FOUND_MSG, roleIds));
		}
	}
}
