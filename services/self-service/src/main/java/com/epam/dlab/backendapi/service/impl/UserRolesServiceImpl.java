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
import com.google.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

public class UserRolesServiceImpl implements UserRolesService {

	private static final String GROUPS = "groups";
	private static final String USERS = "users";

	@Inject
	private UserRoleDao userRoleDao;

	@Override
	public List<UserRoleDto> getUserRoles() {
		return userRoleDao.getUserRoles();
	}

	@Override
	public void createRole(UserRoleDto dto) {
		userRoleDao.createRole(dto);
	}

	@Override
	public void updateRole(UserRoleDto dto) {
		userRoleDao.updateRole(dto);
	}

	@Override
	public void removeRole(String roleId, String user) {
		userRoleDao.removeRoleById(roleId);
	}

	@Override
	public void assignRolesForUser(String userName, Set<String> roleIds) {
		assignRoles(USERS, userName, roleIds);
	}

	@Override
	public void assignRolesForGroup(String groupName, Set<String> roleIds) {
		assignRoles(GROUPS, groupName, roleIds);
	}

	private void assignRoles(String accessMemberType, String accessMemberName, Set<String> roleIds) {
		Map<String, Set<String>> accessMembers = new HashMap<>();
		List<UserRoleDto> userRoles = getUserRoles().stream().filter(role -> roleIds.contains(role.getId()))
				.collect(Collectors.toList());
		userRoles.forEach(role -> populateRoleMap(accessMembers, role, accessMemberType, accessMemberName));
		accessMembers.forEach((key, value) -> userRoleDao.updateRoleField(accessMemberType, value, key));
	}

	private void populateRoleMap(Map<String, Set<String>> roleMap, UserRoleDto dto, String accessType,
								 String accessMember){
		Set<String> accessMembers = getAccessMembers(dto, accessType);
		accessMembers.add(accessMember);
		roleMap.put(dto.getId(), accessMembers);
	}

	private Set<String> getAccessMembers(UserRoleDto dto, String accessType){
		Set<String> result = new HashSet<>();
		if(accessType.equals(GROUPS)){
			return !Objects.isNull(dto.getGroups()) ? dto.getGroups() : result;
		}else if(accessType.equals(USERS)){
			return !Objects.isNull(dto.getUsers()) ? dto.getUsers() : result;
		}
		return result;
	}

}
