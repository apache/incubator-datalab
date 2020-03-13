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

package com.epam.dlab.backendapi.service;


import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.ExploratoryCreatePopUp;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.aws.computational.ClusterConfig;
import com.epam.dlab.model.exploratory.Exploratory;

import java.util.List;
import java.util.Optional;

public interface ExploratoryService {

    String start(UserInfo userInfo, String exploratoryName, String project);

    String stop(UserInfo userInfo, String project, String exploratoryName);

    String terminate(UserInfo userInfo, String project, String exploratoryName);

    String create(UserInfo userInfo, Exploratory exploratory, String project);

    void updateProjectExploratoryStatuses(String project, String endpoint, UserInstanceStatus status);

    void updateClusterConfig(UserInfo userInfo, String project, String exploratoryName, List<ClusterConfig> config);

    Optional<UserInstanceDTO> getUserInstance(String user, String project, String exploratoryName);

    List<ClusterConfig> getClusterConfig(UserInfo user, String project, String exploratoryName);

    ExploratoryCreatePopUp getUserInstances(UserInfo user);
}
