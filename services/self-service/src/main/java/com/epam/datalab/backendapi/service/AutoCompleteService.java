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

import com.epam.datalab.backendapi.dao.UserGroupDAO;
import com.epam.datalab.backendapi.dao.UserSettingsDAO;
import com.epam.datalab.backendapi.resources.dto.SharedWithDTO;
import com.google.inject.Inject;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class AutoCompleteService {
    public final UserGroupDAO userGroupDAO;
    public final UserSettingsDAO userSettingsDAO;

    @Inject
    public AutoCompleteService(UserGroupDAO userGroupDAO, UserSettingsDAO userSettingsDAO) {
        this.userGroupDAO = userGroupDAO;
        this.userSettingsDAO = userSettingsDAO;
    }

    public Set<SharedWithDTO> getUsersAndGroupsForSharing(String name){
        Set<SharedWithDTO> sharedWithDTOS = new HashSet<>();
        sharedWithDTOS.addAll(userSettingsDAO.getUserNames(name).stream().map(s -> new SharedWithDTO(SharedWithDTO.Type.USER, s)).collect(Collectors.toSet()));
        sharedWithDTOS.addAll(userGroupDAO.getGroupNames(name).stream().map(s -> new SharedWithDTO(SharedWithDTO.Type.GROUP, s)).collect(Collectors.toSet()));
        return sharedWithDTOS;
    }
}
