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
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.EndpointResourcesDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.cloud.CloudProvider;

import java.util.List;

public interface EndpointService {
    List<EndpointDTO> getEndpoints();

    List<EndpointDTO> getEndpointsWithStatus(EndpointDTO.EndpointStatus status);

    EndpointResourcesDTO getEndpointResources(String endpoint);

    EndpointDTO get(String name);

    void create(UserInfo userInfo, String resourceName, EndpointDTO endpointDTO);

    void updateEndpointStatus(String name, EndpointDTO.EndpointStatus status);

    void remove(UserInfo userInfo, String name);

    void removeEndpointInAllProjects(UserInfo userInfo, String endpointName, List<ProjectDTO> projects);

    CloudProvider checkUrl(UserInfo userInfo, String url);
}
