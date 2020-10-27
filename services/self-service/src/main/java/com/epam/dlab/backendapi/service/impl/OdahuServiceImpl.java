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
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.OdahuCreateDTO;
import com.epam.dlab.backendapi.domain.OdahuDTO;
import com.epam.dlab.backendapi.domain.OdahuFieldsDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.OdahuService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.odahu.OdahuResult;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class OdahuServiceImpl implements OdahuService {

    private static final String CREATE_ODAHU_API = "infrastructure/odahu";
    private static final String START_ODAHU_API = "infrastructure/odahu/start";
    private static final String STOP_ODAHU_API = "infrastructure/odahu/stop";
    private static final String TERMINATE_ODAHU_API = "infrastructure/odahu/terminate";

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

    @Override//TODO:method should be refactored
    public Optional<OdahuDTO> get(String project, String endpoint) {
        return odahuDAO.getByProjectEndpoint(project, endpoint);
    }

    @BudgetLimited
    @Override
    public void create(@Project String project, OdahuCreateDTO odahuCreateDTO, UserInfo user) {
        boolean activeCluster = odahuDAO.findOdahuClusters(odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint()).stream()
                .noneMatch(o -> !o.getStatus().equals(UserInstanceStatus.FAILED) && !o.getStatus().equals(UserInstanceStatus.TERMINATED));
        if (!activeCluster) {
            throw new ResourceConflictException(String.format("Odahu cluster already exist in system for project %s " +
                    "and endpoint %s", odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint()));
        }
        ProjectDTO projectDTO = projectService.get(project);
        boolean isAdded = odahuDAO.create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
                        odahuCreateDTO.getEndpoint(), UserInstanceStatus.CREATING, getTags(odahuCreateDTO)));

        if (isAdded) {
            String url = null;
            EndpointDTO endpointDTO = endpointService.get(odahuCreateDTO.getEndpoint());
            try {
                url = endpointDTO.getUrl() + CREATE_ODAHU_API;
                String uuid =
                        provisioningService.post(url, user.getAccessToken(),
                                requestBuilder.newOdahuCreate(user, odahuCreateDTO, projectDTO, endpointDTO), String.class);
                requestId.put(user.getName(), uuid);
            } catch (Exception e) {
                log.error("Can not perform {} due to: {}, {}", url, e.getMessage(), e);
                odahuDAO.updateStatus(odahuCreateDTO.getName(), odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint(),
                        UserInstanceStatus.FAILED);
            }
        } else {
            throw new DlabException("The odahu project " + project + " can not be created.");
        }
    }

    @BudgetLimited
    @Override
    public void start(String name, @Project String project, String endpoint, UserInfo user) {
        odahuDAO.updateStatus(name, project, endpoint, UserInstanceStatus.STARTING);
        actionOnCloud(user, START_ODAHU_API, name, project, endpoint);
    }

    @Override
    public void stop(String name, String project, String endpoint, UserInfo user) {
        odahuDAO.updateStatus(name, project, endpoint, UserInstanceStatus.STOPPING);
        actionOnCloud(user, STOP_ODAHU_API, name, project, endpoint);
    }

    @Override
    public void terminate(String name, String project, String endpoint, UserInfo user) {
        odahuDAO.findOdahuClusters(project, endpoint).stream()
                .filter(odahuDTO -> name.equals(odahuDTO.getName())
                        && !odahuDTO.getStatus().equals(UserInstanceStatus.FAILED))
                .forEach(odahuDTO -> {
                    if (UserInstanceStatus.RUNNING == odahuDTO.getStatus()) {
                        odahuDAO.updateStatus(name, project, endpoint, UserInstanceStatus.TERMINATING);
                        actionOnCloud(user, TERMINATE_ODAHU_API, name, project, endpoint);
                    } else {
                        log.error("Cannot terminate odahu cluster {}", odahuDTO);
                        throw new DlabException(String.format("Cannot terminate odahu cluster %s", odahuDTO));
                    }
                });
    }

    @Override
    public void updateStatus(OdahuResult result, UserInstanceStatus status) {
        if (Objects.nonNull(result.getResourceUrls()) && !result.getResourceUrls().isEmpty()) {
            odahuDAO.updateStatusAndUrls(result, status);
        } else {
            odahuDAO.updateStatus(result.getName(), result.getProjectName(), result.getEndpointName(), status);
        }
    }

    @Override
    public boolean inProgress(String project, String endpoint) {
        return get(project, endpoint)
                .filter(odahu -> Arrays.asList(UserInstanceStatus.CREATING, UserInstanceStatus.STARTING,
                        UserInstanceStatus.STOPPING, UserInstanceStatus.TERMINATING).contains(odahu.getStatus()))
                .isPresent();
    }

    private void actionOnCloud(UserInfo user, String uri, String name, String project, String endpoint) {
        String url = null;
        EndpointDTO endpointDTO = endpointService.get(endpoint);
        ProjectDTO projectDTO = projectService.get(project);
        try {
            OdahuFieldsDTO fields = odahuDAO.getFields(name, project, endpoint);
            url = endpointDTO.getUrl() + uri;
            String uuid =
                    provisioningService.post(url, user.getAccessToken(),
                            requestBuilder.newOdahuAction(user, name, projectDTO, endpointDTO, fields), String.class);
            requestId.put(user.getName(), uuid);
        } catch (Exception e) {
            log.error("Can not perform {} due to: {}, {}", url, e.getMessage(), e);
            odahuDAO.updateStatus(name, project, project, UserInstanceStatus.FAILED);
        }
    }

    private Map<String, String> getTags(OdahuCreateDTO odahuCreateDTO) {
        Map<String, String> tags = new HashMap<>();
        tags.put("custom_tag", odahuCreateDTO.getCustomTag());
        tags.put("project_tag", odahuCreateDTO.getProject());
        tags.put("endpoint_tag", odahuCreateDTO.getEndpoint());
        return tags;
    }
}
