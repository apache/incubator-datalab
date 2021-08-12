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

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.edge.EdgeInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProjectDAO {
    List<ProjectDTO> getProjects();

    List<ProjectDTO> getProjectsWithEndpointStatusNotIn(UserInstanceStatus... statuses);

    List<ProjectDTO> getUserProjects(UserInfo userInfo, boolean active);

    void create(ProjectDTO projectDTO);

    void updateStatus(String projectName, ProjectDTO.Status status);

    void updateEdgeStatus(String projectName, String endpoint, UserInstanceStatus status);

    void updateEdgeInfo(String projectName, String endpointName, EdgeInfo edgeInfo);

    Optional<ProjectDTO> get(String name);

    List<ProjectDTO> getProjectsByEndpoint(String endpointName);

    boolean update(ProjectDTO projectDTO);

    void remove(String name);

    Optional<Integer> getAllowedBudget(String project);

    void updateBudget(String project, Integer budget, boolean monthlyBudget);

    boolean isAnyProjectAssigned(Set<String> groups);
}
