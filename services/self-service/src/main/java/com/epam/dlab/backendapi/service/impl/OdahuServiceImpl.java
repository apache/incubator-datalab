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
import com.epam.dlab.backendapi.annotation.BudgetLimited;
import com.epam.dlab.backendapi.annotation.Project;
import com.epam.dlab.backendapi.dao.OdahuDAO;
import com.epam.dlab.backendapi.domain.CreateOdahuDTO;
import com.epam.dlab.backendapi.domain.OdahuDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.OdahuService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.List;
import java.util.Optional;

public class OdahuServiceImpl implements OdahuService {

    private static final String CREATE_ODAHU_API = "infrastructure/odahu";

    private final ProjectService projectService;
    private final EndpointService endpointService;
    private final OdahuDAO odahuDAO;
    private final RESTService provisioningService;
    private final RequestBuilder requestBuilder;
    private final RequestId requestId;

    @Inject
    public OdahuServiceImpl(ProjectService projectService, EndpointService endpointService, OdahuDAO odahuDAO,
                            @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
                            RequestBuilder requestBuilder, RequestId requestId) {
        this.projectService = projectService;
        this.endpointService = endpointService;
        this.odahuDAO = odahuDAO;
        this.provisioningService = provisioningService;
        this.requestBuilder = requestBuilder;
        this.requestId = requestId;
    }


    @Override
    public List<OdahuDTO> findOdahu() {
        return odahuDAO.findOdahuClusters();
    }

    @BudgetLimited
    @Override
    public void create(@Project String project, CreateOdahuDTO createOdahuDTO, UserInfo userInfo) {
        Optional<OdahuDTO> odahuDTO = odahuDAO.getByProjectEndpoint(createOdahuDTO.getProject(), createOdahuDTO.getEndpoint());
        if (odahuDTO.isPresent()) {
            throw new ResourceConflictException(String.format("Odahu cluster already exist in system for project %s " +
                    "and endpoint %s", createOdahuDTO.getProject(), createOdahuDTO.getEndpoint()));
        }
        ProjectDTO projectDTO = projectService.get(project);
        boolean isAdded = odahuDAO.create(new OdahuDTO(createOdahuDTO.getName(), createOdahuDTO.getProject(),
                createOdahuDTO.getEndpoint(), UserInstanceStatus.CREATING));

        if (isAdded) {
            createOnCloud(userInfo, createOdahuDTO, projectDTO);
        }
    }

    private void createOnCloud(UserInfo user, CreateOdahuDTO createOdahuDTO, ProjectDTO projectDTO) {
        String uuid =
                provisioningService.post(endpointService.get(createOdahuDTO.getEndpoint()).getUrl() + CREATE_ODAHU_API,
                        user.getAccessToken(),
                        requestBuilder.newOdahuCreate(user, createOdahuDTO, projectDTO), String.class);
        requestId.put(user.getName(), uuid);
    }
}
