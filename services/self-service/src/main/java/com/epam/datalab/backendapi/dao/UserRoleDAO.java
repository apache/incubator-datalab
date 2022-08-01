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

package com.epam.datalab.backendapi.dao;

import com.epam.datalab.backendapi.resources.dto.UserGroupDto;
import com.epam.datalab.backendapi.resources.dto.UserRoleDTO;
import com.epam.datalab.cloud.CloudProvider;

import java.util.List;
import java.util.Set;

public interface UserRoleDAO {
    List<UserRoleDTO> findAll();

    UserRoleDTO findById(String roleId);

    void insert(UserRoleDTO dto);

    void insert(List<UserRoleDTO> roles);

    boolean update(UserRoleDTO dto);

    void updateMissingRoles(CloudProvider cloudProvider);

    boolean addGroupToRole(Set<String> groups, Set<String> roleIds);

    void removeGroupWhenRoleNotIn(String group, Set<String> roleIds);

    void removeUnnecessaryRoles(CloudProvider cloudProviderToBeRemoved, List<CloudProvider> remainingProviders);

    void remove(String roleId);

    boolean removeGroup(String groupId);

    List<UserGroupDto> aggregateRolesByGroup();
}
