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


import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.annotation.Info;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.resources.dto.ExploratoryCreatePopUp;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.model.exploratory.Exploratory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ExploratoryService {

    String start(UserInfo userInfo, String exploratoryName, String project, String auditInfo);

    String stop(UserInfo userInfo, String resourceCreator, String project, String exploratoryName, String auditInfo);

    String terminate(UserInfo userInfo, String resourceCreator, String project, String exploratoryName, String auditInfo);

    String create(UserInfo userInfo, Exploratory exploratory, String project, String exploratoryName);

    void updateProjectExploratoryStatuses(UserInfo userInfo, String project, String endpoint, UserInstanceStatus status);

    void updateProjectExploratoryStatuses(String project, String endpoint, UserInstanceStatus status);

    void updateClusterConfig(UserInfo userInfo, String project, String exploratoryName, List<ClusterConfig> config);

    Optional<UserInstanceDTO> getUserInstance(String user, String project, String exploratoryName);

    Optional<UserInstanceDTO> getUserInstance(String user, String project, String exploratoryName, boolean includeCompResources);

    List<UserInstanceDTO> findAll();

    List<UserInstanceDTO> findAll(Set<ProjectDTO> projects);

    List<ClusterConfig> getClusterConfig(UserInfo user, String project, String exploratoryName);

    ExploratoryCreatePopUp getUserInstances(UserInfo user);

    void updateAfterStatusCheck(UserInfo userInfo, String project, String endpoint, String name, String instanceID, UserInstanceStatus status, String auditInfo);
}
