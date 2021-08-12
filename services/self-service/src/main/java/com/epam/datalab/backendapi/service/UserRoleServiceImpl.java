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
package com.epam.datalab.backendapi.service;

import com.epam.datalab.backendapi.dao.UserRoleDAO;
import com.epam.datalab.backendapi.resources.dto.UserRoleDTO;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Singleton
public class UserRoleServiceImpl implements UserRoleService {
    private static final String ROLE_NOT_FOUND_MSG = "Any of role : %s were not found";

    @Inject
    private UserRoleDAO userRoleDao;

    @Override
    public List<UserRoleDTO> getUserRoles() {
        return userRoleDao.findAll();
    }

    @Override
    public void createRole(UserRoleDTO dto) {
        userRoleDao.insert(dto);
    }

    @Override
    public void updateRole(UserRoleDTO dto) {
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
