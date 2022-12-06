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
import com.epam.datalab.backendapi.annotation.*;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.dao.ProjectDAO;
import com.epam.datalab.backendapi.dao.UserGroupDAO;
import com.epam.datalab.backendapi.domain.*;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.ExploratoryService;
import com.epam.datalab.backendapi.service.OdahuService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.exceptions.ResourceConflictException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.epam.datalab.backendapi.domain.AuditActionEnum.*;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.EDGE_NODE;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.PROJECT;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private static final String CREATE_PRJ_API = "infrastructure/project/create";
    private static final String TERMINATE_PRJ_API = "infrastructure/project/terminate";
    private static final String START_PRJ_API = "infrastructure/project/start";
    private static final String STOP_PRJ_API = "infrastructure/project/stop";
    private static final String RECREATE_PRJ_API = "infrastructure/project/recreate";
    private static final String STOP_ACTION = "stop";
    private static final String TERMINATE_ACTION = "terminate";
    private static final String TOTAL_BUDGET_PERIOD = "Total";
    private static final String MONTHLY_BUDGET_PERIOD = "Monthly";

    private static final String AUDIT_ADD_ENDPOINT = "Add endpoint(s): %s\n";
    private static final String AUDIT_ADD_GROUP = "Add group(s): %s\n";
    private static final String AUDIT_REMOVE_GROUP = "Remove group(s): %s\n";
    private static final String AUDIT_UPDATE_BUDGET = "Update quota: %d->%d\nUpdate period: %s->%s";
    private static final String AUDIT_ADD_EDGE_NODE = "Create edge node for endpoint %s, requested in project %s";
    private static final String AUDIT_RECREATE_EDGE_NODE = "Recreate edge node for endpoint %s, requested in project %s";

    private final ProjectDAO projectDAO;
    private final UserGroupDAO userGroupDAO;
    private final ExploratoryDAO exploratoryDAO;
    private final ExploratoryService exploratoryService;
    private final RESTService provisioningService;
    private final EndpointService endpointService;
    private final RequestId requestId;
    private final RequestBuilder requestBuilder;
    private final SelfServiceApplicationConfiguration configuration;
    private final OdahuService odahuService;


    @Inject
    public ProjectServiceImpl(ProjectDAO projectDAO, ExploratoryService exploratoryService,
                              UserGroupDAO userGroupDAO,
                              @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
                              RequestId requestId, RequestBuilder requestBuilder, EndpointService endpointService,
                              ExploratoryDAO exploratoryDAO, SelfServiceApplicationConfiguration configuration,
                              OdahuService odahuService) {
        this.projectDAO = projectDAO;
        this.exploratoryService = exploratoryService;
        this.userGroupDAO = userGroupDAO;
        this.provisioningService = provisioningService;
        this.requestId = requestId;
        this.requestBuilder = requestBuilder;
        this.endpointService = endpointService;
        this.exploratoryDAO = exploratoryDAO;
        this.configuration = configuration;
        this.odahuService = odahuService;
    }

    @Override
    public List<ProjectDTO> getProjects() {
        return projectDAO.getProjects();
    }

    @Override
    public List<ProjectDTO> getProjects(UserInfo user) {
        return projectDAO.getProjects()
                .stream()
                .filter(project -> UserRoles.isProjectAdmin(user, project.getGroups()) || UserRoles.isAdmin(user))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectDTO> getUserProjects(UserInfo userInfo, boolean active) {
        return projectDAO.getUserProjects(userInfo, active);
    }

    @Override
    public List<ProjectDTO> getProjectsByEndpoint(String endpointName) {
        return projectDAO.getProjectsByEndpoint(endpointName);
    }
    
    @BudgetLimited
    @Override
    public void create(UserInfo user, ProjectDTO projectDTO, String projectName) {
        if (!projectDAO.get(projectDTO.getName()).isPresent()) {
            projectDAO.create(projectDTO);
            createProjectOnCloud(user, projectDTO);
        } else {
            throw new ResourceConflictException("Project with passed name already exist in system");
        }
    }

    @BudgetLimited
    @Override
    public void recreate(UserInfo userInfo, String endpoint, @Project String name) {
        projectDAO.updateEdgeStatus(name, endpoint, UserInstanceStatus.CREATING);
        recreateEndpoint(userInfo, get(name), endpoint, name, String.format(AUDIT_RECREATE_EDGE_NODE, endpoint, name));
    }

    @Override
    public ProjectDTO get(String name) {
        return projectDAO.get(name)
                .orElseThrow(projectNotFound());
    }

    @Audit(action = TERMINATE, type = EDGE_NODE)
    @Override
    public void terminateEndpoint(@User UserInfo userInfo, @ResourceName String endpoint, @Project String name) {
        projectActionOnCloud(userInfo, name, TERMINATE_PRJ_API, endpoint);
        projectDAO.updateEdgeStatus(name, endpoint, UserInstanceStatus.TERMINATING);
        exploratoryService.updateProjectExploratoryStatuses(userInfo, name, endpoint, UserInstanceStatus.TERMINATING);
        odahuService.get(name, endpoint)
                .filter(o -> UserInstanceStatus.RUNNING == o.getStatus())
                .ifPresent(odahu -> odahuService.terminate(odahu.getName(), name, endpoint, userInfo));
    }

    @ProjectAdmin
    @Override
    public void terminateEndpoint(@User UserInfo userInfo, List<String> endpoints, @Project String name) {
        List<ProjectEndpointDTO> endpointDTOs = getProjectEndpointDTOS(endpoints, name);
        checkProjectRelatedResourcesInProgress(name, endpointDTOs, TERMINATE_ACTION);
        endpoints.forEach(endpoint -> terminateEndpoint(userInfo, endpoint, name));
    }

    @BudgetLimited
    @Audit(action = START, type = EDGE_NODE)
    @Override
    public void start(@User UserInfo userInfo, @ResourceName String endpoint, @Project String name) {
        projectActionOnCloud(userInfo, name, START_PRJ_API, endpoint);
        projectDAO.updateEdgeStatus(name, endpoint, UserInstanceStatus.STARTING);
    }

    @ProjectAdmin
    @Override
    public void start(@User UserInfo userInfo, List<String> endpoints, @Project String name) {
        endpoints.forEach(endpoint -> start(userInfo, endpoint, name));
    }

    @Audit(action = STOP, type = EDGE_NODE)
    @Override
    public void stop(@User UserInfo userInfo, @ResourceName String endpoint, @Project String name, @Info String auditInfo) {
        projectActionOnCloud(userInfo, name, STOP_PRJ_API, endpoint);
        projectDAO.updateEdgeStatus(name, endpoint, UserInstanceStatus.STOPPING);
    }

    @ProjectAdmin
    @Override
    public void stopWithResources(@User UserInfo userInfo, List<String> endpoints,
                                  @ResourceName @Project String projectName) {
        List<ProjectEndpointDTO> endpointDTOs = getProjectEndpointDTOS(endpoints, projectName);
        checkProjectRelatedResourcesInProgress(projectName, endpointDTOs, STOP_ACTION);

        endpointDTOs
                .stream()
                .filter(e -> !Arrays.asList(UserInstanceStatus.TERMINATED, UserInstanceStatus.TERMINATING, UserInstanceStatus.STOPPED,
                        UserInstanceStatus.FAILED).contains(e.getStatus()))
                .forEach(e -> stop(userInfo, e.getName(), projectName, null));

        exploratoryDAO.fetchRunningExploratoryFieldsForProject(projectName,
                endpointDTOs.stream()
                        .map(ProjectEndpointDTO::getName)
                        .collect(Collectors.toList()))
                .forEach(e -> exploratoryService.stop(userInfo, e.getUser(), projectName, e.getExploratoryName(), null));
    }

    @ProjectAdmin
    @Override
    public void update(@User UserInfo userInfo, UpdateProjectDTO projectDTO, @Project String projectName) {
        final ProjectDTO project = projectDAO.get(projectDTO.getName()).orElseThrow(projectNotFound());
        final Set<String> endpoints = project.getEndpoints()
                .stream()
                .map(ProjectEndpointDTO::getName)
                .collect(toSet());
        final Set<String> newEndpoints = new HashSet<>(projectDTO.getEndpoints());
        newEndpoints.removeAll(endpoints);
        final String projectUpdateAudit = updateProjectAudit(projectDTO, project, newEndpoints);
        updateProject(userInfo, projectName, projectDTO, project, newEndpoints, projectUpdateAudit);
    }

    @Audit(action = UPDATE, type = PROJECT)
    public void updateProject(@User UserInfo userInfo, @Project @ResourceName String projectName,
                              UpdateProjectDTO projectDTO, ProjectDTO project, Set<String> newEndpoints,
                              @Info String projectAudit) {
        final List<ProjectEndpointDTO> endpointsToBeCreated = newEndpoints
                .stream()
                .map(e -> new ProjectEndpointDTO(e, UserInstanceStatus.CREATING, null))
                .collect(Collectors.toList());
        project.getEndpoints().addAll(endpointsToBeCreated);
        projectDAO.update(new ProjectDTO(project.getName(), projectDTO.getGroups(), project.getKey(),
                project.getTag(), project.getBudget(), project.getEndpoints(), projectDTO.isSharedImageEnabled()));
        endpointsToBeCreated.forEach(e -> createEndpoint(userInfo, project, e.getName(), projectName, String.format(AUDIT_ADD_EDGE_NODE, e.getName(), project.getName())));
    }

    @Override
    public void updateBudget(UserInfo userInfo, List<UpdateProjectBudgetDTO> dtos) {
        final List<ProjectDTO> projects = dtos
                .stream()
                .map(this::getUpdateProjectDTO)
                .collect(Collectors.toList());

        projects.forEach(p -> updateBudget(userInfo, p.getName(), p.getBudget(), getUpdateBudgetAudit(p)));
    }

    @Audit(action = UPDATE, type = PROJECT)
    public void updateBudget(@User UserInfo userInfo, @Project @ResourceName String name, BudgetDTO budget,
                             @Info String updateBudgetAudit) {
        projectDAO.updateBudget(name, budget.getValue(), budget.isMonthlyBudget());
    }

    @Override
    public boolean isAnyProjectAssigned(UserInfo userInfo) {
        final Set<String> userGroups = concat(userInfo.getRoles().stream(),
                userGroupDAO.getUserGroups(userInfo.getName()).stream())
                .collect(toSet());
        return projectDAO.isAnyProjectAssigned(userGroups);
    }

    @Override
    public boolean checkExploratoriesAndComputationalProgress(String projectName, List<String> endpoints) {
        return exploratoryDAO.fetchProjectEndpointExploratoriesWhereStatusIn(projectName, endpoints, Arrays.asList(
                UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE,
                UserInstanceStatus.CONFIGURING, UserInstanceStatus.RECONFIGURING, UserInstanceStatus.STOPPING,
                UserInstanceStatus.TERMINATING),
                UserInstanceStatus.CREATING, UserInstanceStatus.CONFIGURING, UserInstanceStatus.STARTING,
                UserInstanceStatus.RECONFIGURING, UserInstanceStatus.CREATING_IMAGE, UserInstanceStatus.STOPPING,
                UserInstanceStatus.TERMINATING).isEmpty();
    }

    @Audit(action = UPDATE, type = EDGE_NODE)
    @Override
    public void updateAfterStatusCheck(@User UserInfo userInfo, @Project String project, @ResourceName String endpoint,
                                       String instanceID, UserInstanceStatus status, @Info String auditInfo) {
        projectDAO.updateEdgeStatus(project, endpoint, status);
    }

    private void createProjectOnCloud(UserInfo user, ProjectDTO project) {
        try {
            project.getEndpoints().forEach(e -> createEndpoint(user, project, e.getName(), project.getName(),
                    String.format(AUDIT_ADD_EDGE_NODE, e.getName(), project.getName())));
        } catch (Exception e) {
            log.error("Can not create project due to: {}", e.getMessage(), e);
            projectDAO.updateStatus(project.getName(), ProjectDTO.Status.FAILED);
        }
    }

    @Audit(action = CREATE, type = EDGE_NODE)
    public void createEndpoint(@User UserInfo user, ProjectDTO projectDTO, @ResourceName String endpointName,
                               @Project String projectName, @Info String auditInfo) {
        createEdgeNode(user, projectDTO, endpointName, CREATE_PRJ_API);
    }

    @Audit(action = RECREATE, type = EDGE_NODE)
    public void recreateEndpoint(@User UserInfo user, ProjectDTO projectDTO, @ResourceName String endpointName,
                                 @Project String projectName, @Info String auditInfo) {
        createEdgeNode(user, projectDTO, endpointName, RECREATE_PRJ_API);
    }

    private void createEdgeNode(UserInfo user, ProjectDTO projectDTO, String endpointName, String provisioningApiUri) {
        EndpointDTO endpointDTO = endpointService.get(endpointName);
        String uuid = provisioningService.post(endpointDTO.getUrl() + provisioningApiUri, user.getAccessToken(),
                requestBuilder.newProjectCreate(user, projectDTO, endpointDTO), String.class);
        requestId.put(user.getName(), uuid);
    }

    private void projectActionOnCloud(UserInfo user, String projectName, String provisioningApiUri, String endpoint) {
        try {
            EndpointDTO endpointDTO = endpointService.get(endpoint);
            String uuid = provisioningService.post(endpointDTO.getUrl() + provisioningApiUri, user.getAccessToken(),
                    requestBuilder.newProjectAction(user, projectName, endpointDTO), String.class);
            requestId.put(user.getName(), uuid);
        } catch (Exception e) {
            log.error("Can not post to {} project due to: {}", provisioningApiUri, e.getMessage(), e);
            projectDAO.updateStatus(projectName, ProjectDTO.Status.FAILED);
        }
    }

    private void checkProjectRelatedResourcesInProgress(String projectName, List<ProjectEndpointDTO> endpoints, String action) {
        boolean edgeAndOdahuProgress = endpoints
                .stream()
                .anyMatch(e ->
                        Arrays.asList(UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.STOPPING,
                                UserInstanceStatus.TERMINATING).contains(e.getStatus())
                                || odahuService.inProgress(projectName, e.getName()));

        List<String> endpointNames = endpoints
                .stream()
                .map(ProjectEndpointDTO::getName)
                .collect(Collectors.toList());
        if (edgeAndOdahuProgress || !checkExploratoriesAndComputationalProgress(projectName, endpointNames)) {
            throw new ResourceConflictException((String.format("Can not %s environment because one of project " +
                    "resource is in processing stage", action)));
        }
    }

    private String updateProjectAudit(UpdateProjectDTO projectDTO, ProjectDTO project, Set<String> newEndpoints) {
        if (!configuration.isAuditEnabled()) {
            return null;
        }
        StringBuilder audit = new StringBuilder();
        final Set<String> newGroups = new HashSet<>(projectDTO.getGroups());
        newGroups.removeAll(project.getGroups());
        final Set<String> removedGroups = new HashSet<>(project.getGroups());
        removedGroups.removeAll(projectDTO.getGroups());

        if (!newEndpoints.isEmpty()) {
            audit.append(String.format(AUDIT_ADD_ENDPOINT, String.join(", ", newEndpoints)));
        }
        if (!newGroups.isEmpty()) {
            audit.append(String.format(AUDIT_ADD_GROUP, String.join(", ", newGroups)));
        }
        if (!removedGroups.isEmpty()) {
            audit.append(String.format(AUDIT_REMOVE_GROUP, String.join(", ", removedGroups)));
        }
        return audit.toString();
    }

    private String getUpdateBudgetAudit(ProjectDTO p) {
        if (!configuration.isAuditEnabled()) {
            return null;
        }
        ProjectDTO projectDTO = get(p.getName());
        Integer value = Optional.ofNullable(projectDTO.getBudget())
                .map(BudgetDTO::getValue)
                .orElse(null);
        Boolean monthlyBudget = Optional.ofNullable(projectDTO.getBudget())
                .map(BudgetDTO::isMonthlyBudget)
                .orElse(null);
        return String.format(AUDIT_UPDATE_BUDGET, value, p.getBudget().getValue(),
                representBudgetType(monthlyBudget), representBudgetType(p.getBudget().isMonthlyBudget()));
    }

    private String representBudgetType(Boolean isMonthlyPeriod) {
        return (isMonthlyPeriod) ? MONTHLY_BUDGET_PERIOD : TOTAL_BUDGET_PERIOD;
    }

    private List<ProjectEndpointDTO> getProjectEndpointDTOS(List<String> endpoints, @Project String name) {
        return get(name)
                .getEndpoints()
                .stream()
                .filter(projectEndpointDTO -> endpoints.contains(projectEndpointDTO.getName()))
                .collect(Collectors.toList());
    }

    private ProjectDTO getUpdateProjectDTO(UpdateProjectBudgetDTO dto) {
        BudgetDTO budgetDTO = BudgetDTO.builder()
                .value(dto.getBudget())
                .monthlyBudget(dto.isMonthlyBudget())
                .build();
        return ProjectDTO.builder()
                .name(dto.getProject())
                .budget(budgetDTO)
                .build();
    }

    private Supplier<ResourceNotFoundException> projectNotFound() {
        return () -> new ResourceNotFoundException("Project with passed name not found");
    }
}
