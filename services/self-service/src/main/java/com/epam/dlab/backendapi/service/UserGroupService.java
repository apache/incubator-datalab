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
package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.resources.dto.UserGroupDto;

import java.util.List;
import java.util.Set;

public interface UserGroupService {

	void createGroup(String group, Set<String> roleIds, Set<String> users);

	void addUsersToGroup(String group, Set<String> users);

	void updateRolesForGroup(String group, Set<String> roleIds);

	void removeUserFromGroup(String group, String user);

	void removeGroupFromRole(Set<String> groups, Set<String> roleIds);

	List<UserGroupDto> getAggregatedRolesByGroup();
}
