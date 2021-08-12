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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.annotation.Audit;
import com.epam.datalab.backendapi.annotation.ResourceName;
import com.epam.datalab.backendapi.annotation.User;
import com.epam.datalab.backendapi.dao.EndpointDAO;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.dao.UserRoleDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.EndpointResourcesDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.OdahuService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceConflictException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.epam.datalab.backendapi.domain.AuditActionEnum.CONNECT;
import static com.epam.datalab.backendapi.domain.AuditActionEnum.DISCONNECT;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.ENDPOINT;
import static com.epam.datalab.dto.UserInstanceStatus.TERMINATED;


@Slf4j
public class EndpointServiceImpl implements EndpointService {
    private static final String HEALTH_CHECK = "healthcheck";
    private final EndpointDAO endpointDAO;
    private final ProjectService projectService;
    private final ExploratoryDAO exploratoryDAO;
    private final RESTService provisioningService;
    private final UserRoleDAO userRoleDao;
    private final OdahuService odahuService;

    @Inject
    public EndpointServiceImpl(EndpointDAO endpointDAO, ProjectService projectService, ExploratoryDAO exploratoryDAO,
                               @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
                               UserRoleDAO userRoleDao, OdahuService odahuService) {

        this.endpointDAO = endpointDAO;
        this.projectService = projectService;
        this.exploratoryDAO = exploratoryDAO;
        this.provisioningService = provisioningService;
        this.userRoleDao = userRoleDao;
        this.odahuService = odahuService;
    }

    @Override
    public List<EndpointDTO> getEndpoints() {
        return endpointDAO.getEndpoints();
    }

    @Override
    public List<EndpointDTO> getEndpointsWithStatus(EndpointDTO.EndpointStatus status) {
        return endpointDAO.getEndpointsWithStatus(status.name());
    }

    @Override
    public EndpointResourcesDTO getEndpointResources(String endpoint) {
        List<UserInstanceDTO> exploratories = exploratoryDAO.fetchExploratoriesByEndpointWhereStatusNotIn(endpoint,
                Arrays.asList(TERMINATED, UserInstanceStatus.FAILED), Boolean.FALSE);

        List<ProjectDTO> projects = projectService.getProjectsByEndpoint(endpoint);

        return new EndpointResourcesDTO(exploratories, projects);
    }

    @Override
    public EndpointDTO get(String name) {
        return endpointDAO.get(name)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint with name " + name + " not found"));
    }

    /**
     * Create new endpoint object in the System.
     * The Endpoint objects should contain Unique values of the 'url' and 'name' fields,
     * i.e two objects with same URLs should not be created in the system.
     *
     * @param userInfo     user properties
     * @param resourceName name of the endpoint
     * @param endpointDTO  object with endpoint fields
     */
    @Audit(action = CONNECT, type = ENDPOINT)
    @Override
    public void create(@User UserInfo userInfo, @ResourceName String resourceName, EndpointDTO endpointDTO) {
        if (endpointDAO.get(endpointDTO.getName()).isPresent()) {
            throw new ResourceConflictException("The Endpoint with this name exists in system");
        }
        if (endpointDAO.getEndpointWithUrl(endpointDTO.getUrl()).isPresent()) {
            throw new ResourceConflictException("The Endpoint URL with this address exists in system");
        }
        CloudProvider cloudProvider = checkUrl(userInfo, endpointDTO.getUrl());
        if (Objects.isNull(cloudProvider)) {
            throw new DatalabException("CloudProvider cannot be null");
        }
        endpointDAO.create(new EndpointDTO(endpointDTO.getName(), endpointDTO.getUrl(), endpointDTO.getAccount(),
                endpointDTO.getTag(), EndpointDTO.EndpointStatus.ACTIVE, cloudProvider));
        userRoleDao.updateMissingRoles(cloudProvider);
    }

    @Override
    public void updateEndpointStatus(String name, EndpointDTO.EndpointStatus status) {
        endpointDAO.updateEndpointStatus(name, status.name());
    }

    @Override
    public void remove(UserInfo userInfo, String name) {
        EndpointDTO endpointDTO = endpointDAO.get(name).orElseThrow(() -> new ResourceNotFoundException(String.format("Endpoint %s does not exist", name)));
        List<ProjectDTO> projects = projectService.getProjectsByEndpoint(name);
        checkProjectEndpointResourcesStatuses(projects, name);
        CloudProvider cloudProvider = endpointDTO.getCloudProvider();
        removeEndpoint(userInfo, name, cloudProvider, projects);
    }

    @Audit(action = DISCONNECT, type = ENDPOINT)
    public void removeEndpoint(@User UserInfo userInfo, @ResourceName String name, CloudProvider cloudProvider,
                               List<ProjectDTO> projects) {
        removeEndpointInAllProjects(userInfo, name, projects);
        endpointDAO.remove(name);
        List<CloudProvider> remainingProviders = endpointDAO.getEndpoints()
                .stream()
                .map(EndpointDTO::getCloudProvider)
                .collect(Collectors.toList());
        userRoleDao.removeUnnecessaryRoles(cloudProvider, remainingProviders);
    }

    @Override
    public void removeEndpointInAllProjects(UserInfo userInfo, String endpointName, List<ProjectDTO> projects) {
        projects.stream()
                .filter(p -> p.getEndpoints().stream()
                        .noneMatch(e -> e.getName().equals(endpointName) && e.getStatus() == TERMINATED))
                .forEach(project -> projectService.terminateEndpoint(userInfo, endpointName, project.getName()));
    }

    @Override
    public CloudProvider checkUrl(UserInfo userInfo, String url) {
        Response response;
        CloudProvider cloudProvider;
        try {
            response = provisioningService.get(url + HEALTH_CHECK, userInfo.getAccessToken(), Response.class);
            cloudProvider = response.readEntity(CloudProvider.class);
        } catch (Exception e) {
            log.error("Cannot connect to url '{}'. {}", url, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot connect to url '%s'.", url));
        }
        if (response.getStatus() != 200) {
            log.warn("Endpoint url {} is not valid", url);
            throw new ResourceNotFoundException(String.format("Endpoint url '%s' is not valid", url));
        }
        return cloudProvider;
    }

    private void checkProjectEndpointResourcesStatuses(List<ProjectDTO> projects, String endpoint) {
        boolean isTerminationEnabled = projects
                .stream()
                .anyMatch(p ->
                        odahuService.inProgress(p.getName(), endpoint) ||
                                !projectService.checkExploratoriesAndComputationalProgress(p.getName(), Collections.singletonList(endpoint)) ||
                                p.getEndpoints()
                                        .stream()
                                        .anyMatch(e -> e.getName().equals(endpoint) &&
                                                Arrays.asList(UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.STOPPING,
                                                        UserInstanceStatus.TERMINATING).contains(e.getStatus())));

        if (isTerminationEnabled) {
            throw new ResourceConflictException(("Can not terminate resources of endpoint because one of project " +
                    "resource is in processing stage"));
        }
    }
}
