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
import com.epam.datalab.backendapi.dao.ComputationalDAO;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.dao.GitCredsDAO;
import com.epam.datalab.backendapi.dao.ImageExploratoryDAO;
import com.epam.datalab.backendapi.domain.*;
import com.epam.datalab.backendapi.resources.dto.ExploratoryCreatePopUp;
import com.epam.datalab.backendapi.service.*;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.StatusEnvBaseDTO;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.dto.exploratory.*;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.model.exploratory.Exploratory;
import com.epam.datalab.model.library.Library;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.epam.datalab.backendapi.domain.AuditActionEnum.*;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.INSTANCE;
import static com.epam.datalab.dto.UserInstanceStatus.*;
import static com.epam.datalab.rest.contracts.ExploratoryAPI.*;

@Slf4j
@Singleton
public class ExploratoryServiceImpl implements ExploratoryService {
    private final ProjectService projectService;
    private final ExploratoryDAO exploratoryDAO;
    private final ComputationalDAO computationalDAO;
    private final GitCredsDAO gitCredsDAO;
    private final ImageExploratoryDAO imageExploratoryDao;
    private final RESTService provisioningService;
    private final RequestBuilder requestBuilder;
    private final RequestId requestId;
    private final TagService tagService;
    private final EndpointService endpointService;
    private final AuditService auditService;
    private final SelfServiceApplicationConfiguration configuration;

    @Inject
    public ExploratoryServiceImpl(ProjectService projectService, ExploratoryDAO exploratoryDAO, ComputationalDAO computationalDAO, GitCredsDAO gitCredsDAO,
                                  ImageExploratoryDAO imageExploratoryDao, @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
                                  RequestBuilder requestBuilder, RequestId requestId, TagService tagService, EndpointService endpointService, AuditService auditService,
                                  SelfServiceApplicationConfiguration configuration) {
        this.projectService = projectService;
        this.exploratoryDAO = exploratoryDAO;
        this.computationalDAO = computationalDAO;
        this.gitCredsDAO = gitCredsDAO;
        this.imageExploratoryDao = imageExploratoryDao;
        this.provisioningService = provisioningService;
        this.requestBuilder = requestBuilder;
        this.requestId = requestId;
        this.tagService = tagService;
        this.endpointService = endpointService;
        this.auditService = auditService;
        this.configuration = configuration;
    }

    @BudgetLimited
    @Audit(action = START, type = INSTANCE)
    @Override
    public String start(@User UserInfo userInfo, @ResourceName String exploratoryName, @Project String project, @Info String auditInfo) {
        return action(userInfo, userInfo.getName(), project, exploratoryName, EXPLORATORY_START, STARTING);
    }

    @Audit(action = STOP, type = INSTANCE)
    @Override
    public String stop(@User UserInfo userInfo, String resourceCreator, @Project String project, @ResourceName String exploratoryName, @Info String auditInfo) {
        return action(userInfo, resourceCreator, project, exploratoryName, EXPLORATORY_STOP, STOPPING);
    }

    @Audit(action = TERMINATE, type = INSTANCE)
    @Override
    public String terminate(@User UserInfo userInfo, String resourceCreator, @Project String project, @ResourceName String exploratoryName, @Info String auditInfo) {
        return action(userInfo, resourceCreator, project, exploratoryName, EXPLORATORY_TERMINATE, TERMINATING);
    }

    @BudgetLimited
    @Audit(action = CREATE, type = INSTANCE)
    @Override
    public String create(@User UserInfo userInfo, Exploratory exploratory, @Project String project, @ResourceName String exploratoryName) {
        boolean isAdded = false;
        try {
            final ProjectDTO projectDTO = projectService.get(project);
            final EndpointDTO endpointDTO = endpointService.get(exploratory.getEndpoint());
            changeImageNameForDeepLearningOnAWSandAzure(exploratory, endpointDTO);
            final UserInstanceDTO userInstanceDTO = getUserInstanceDTO(userInfo, exploratory, project, endpointDTO.getCloudProvider());
            exploratoryDAO.insertExploratory(userInstanceDTO);
            isAdded = true;
            final ExploratoryGitCredsDTO gitCreds = gitCredsDAO.findGitCreds(userInfo.getName());
            log.debug("Created exploratory environment {} for user {}", exploratory.getName(), userInfo.getName());
            final String uuid =
                    provisioningService.post(endpointDTO.getUrl() + EXPLORATORY_CREATE,
                            userInfo.getAccessToken(),
                            requestBuilder.newExploratoryCreate(projectDTO, endpointDTO, exploratory, userInfo,
                                    gitCreds, userInstanceDTO.getTags()),
                            String.class);
            requestId.put(userInfo.getName(), uuid);
            return uuid;
        } catch (Exception t) {
            log.error("Could not update the status of exploratory environment {} with name {} for user {}",
                    exploratory.getDockerImage(), exploratory.getName(), userInfo.getName(), t);
            if (isAdded) {
                updateExploratoryStatusSilent(userInfo.getName(), project, exploratory.getName(), FAILED);
            }
            throw new DatalabException("Could not create exploratory environment " + exploratory.getName() + " for user "
                    + userInfo.getName() + ": " + Optional.ofNullable(t.getCause()).map(Throwable::getMessage).orElse(t.getMessage()), t);
        }
    }

    private void changeImageNameForDeepLearningOnAWSandAzure(Exploratory exploratory, EndpointDTO endpointDTO) {
        if (isDeepLearningOnAwsOrAzure(exploratory, endpointDTO)) {
            if (exploratory.getImageName() == null || exploratory.getImageName().isEmpty())
                exploratory.setImageName(exploratory.getVersion());
        }
    }

    private boolean isDeepLearningOnAwsOrAzure(Exploratory exploratory, EndpointDTO endpointDTO) {
        return exploratory.getVersion().equals("Deep Learning AMI (Ubuntu 18.04) Version 60.2") ||
                exploratory.getVersion().equals("microsoft-dsvm:ubuntu-1804:1804");
    }

    @Override
    public void updateProjectExploratoryStatuses(UserInfo userInfo, String project, String endpoint, UserInstanceStatus status) {
        exploratoryDAO.fetchProjectExploratoriesWhereStatusNotIn(project, endpoint, TERMINATED, FAILED)
                .forEach(ui -> updateExploratoryComputeStatuses(userInfo, project, ui.getExploratoryName(), status, ui.getUser()));
    }

    @Override
    public void updateProjectExploratoryStatuses(String project, String endpoint, UserInstanceStatus status) {
        exploratoryDAO.fetchProjectExploratoriesWhereStatusNotIn(project, endpoint, TERMINATED, FAILED)
                .forEach(ui -> {
                    updateExploratoryStatus(ui.getUser(), project, ui.getExploratoryName(), status);
                    updateComputationalStatuses(ui.getUser(), project, ui.getExploratoryName(), TERMINATED, TERMINATED, TERMINATED, FAILED);
                });
    }

    @Audit(action = RECONFIGURE, type = INSTANCE)
    @Override
    public void updateClusterConfig(@User UserInfo userInfo, @Project String project, @ResourceName String exploratoryName, List<ClusterConfig> config) {
        final String userName = userInfo.getName();
        final String token = userInfo.getAccessToken();
        final UserInstanceDTO userInstanceDTO = exploratoryDAO.fetchRunningExploratoryFields(userName, project, exploratoryName);
        EndpointDTO endpointDTO = endpointService.get(userInstanceDTO.getEndpoint());
        final ExploratoryReconfigureSparkClusterActionDTO updateClusterConfigDTO =
                requestBuilder.newClusterConfigUpdate(userInfo, userInstanceDTO, config, endpointDTO);
        final String uuid = provisioningService.post(endpointDTO.getUrl() + EXPLORATORY_RECONFIGURE_SPARK,
                token, updateClusterConfigDTO,
                String.class);
        requestId.put(userName, uuid);
        exploratoryDAO.updateExploratoryFields(new ExploratoryStatusDTO()
                .withUser(userName)
                .withProject(project)
                .withExploratoryName(exploratoryName)
                .withConfig(config)
                .withStatus(UserInstanceStatus.RECONFIGURING.toString()));
    }

    /**
     * Returns user instance's data by it's name.
     *
     * @param user            user.
     * @param project
     * @param exploratoryName name of exploratory.
     * @return corresponding user instance's data or empty data if resource doesn't exist.
     */
    @Override
    public Optional<UserInstanceDTO> getUserInstance(String user, String project, String exploratoryName) {
        try {
            return Optional.of(exploratoryDAO.fetchExploratoryFields(user, project, exploratoryName));
        } catch (DatalabException e) {
            log.warn("User instance with exploratory {}, project {} for user {} not found.", exploratoryName, project, user, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<UserInstanceDTO> getUserInstance(String user, String project, String exploratoryName, boolean includeCompResources) {
        try {
            return Optional.of(exploratoryDAO.fetchExploratoryFields(user, project, exploratoryName, includeCompResources));
        } catch (DatalabException e) {
            log.warn("User instance with exploratory {}, project {} for user {} not found.", exploratoryName, project, user, e);
        }
        return Optional.empty();
    }

    @Override
    public List<UserInstanceDTO> findAll() {
        return exploratoryDAO.getInstances();
    }

    @Override
    public List<UserInstanceDTO> findAll(Set<ProjectDTO> projects) {
        List<String> projectNames = projects
                .stream()
                .map(ProjectDTO::getName)
                .collect(Collectors.toList());
        return exploratoryDAO.fetchExploratoryFieldsForProjectWithComp(projectNames);
    }

    @Override
    public List<ClusterConfig> getClusterConfig(UserInfo user, String project, String exploratoryName) {
        return exploratoryDAO.getClusterConfig(user.getName(), project, exploratoryName);
    }

    @Override
    public ExploratoryCreatePopUp getUserInstances(UserInfo user) {
        List<ProjectDTO> userProjects = projectService.getUserProjects(user, false);
        Map<String, List<String>> collect = userProjects.stream()
                .collect(Collectors.toMap(ProjectDTO::getName, this::getProjectExploratoryNames));
        return new ExploratoryCreatePopUp(userProjects, collect);
    }

    @Audit(action = UPDATE, type = INSTANCE)
    @Override
    public void updateAfterStatusCheck(@User UserInfo userInfo, @Project String project, String endpoint, @ResourceName String name,
                                       String instanceID, UserInstanceStatus status, @Info String auditInfo) {
        exploratoryDAO.updateExploratoryStatus(project, endpoint, name, instanceID, status);
    }

    private List<String> getProjectExploratoryNames(ProjectDTO project) {
        return exploratoryDAO.fetchExploratoryFieldsForProject(project.getName()).stream()
                .map(UserInstanceDTO::getExploratoryName)
                .collect(Collectors.toList());
    }

    /**
     * Sends the post request to the provisioning service and update the status of exploratory environment.
     *
     * @param userInfo        user info.
     * @param resourceCreator username of person who has created the resource
     * @param project         name of project
     * @param exploratoryName name of exploratory environment.
     * @param action          action for exploratory environment.
     * @param status          status for exploratory environment.
     * @return Invocation request as JSON string.
     */
    private String action(UserInfo userInfo, String resourceCreator, String project, String exploratoryName, String action, UserInstanceStatus status) {
        try {
            updateExploratoryComputeStatuses(userInfo.getName(), project, exploratoryName, status, resourceCreator);

            UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(resourceCreator, project, exploratoryName);
            EndpointDTO endpointDTO = endpointService.get(userInstance.getEndpoint());
            final String uuid =
                    provisioningService.post(endpointDTO.getUrl() + action, userInfo.getAccessToken(),
                            getExploratoryActionDto(userInfo, resourceCreator, status, userInstance, endpointDTO), String.class);
            requestId.put(resourceCreator, uuid);
            return uuid;
        } catch (Exception t) {
            log.error("Could not {} exploratory environment {} for user {}",
                    StringUtils.substringAfter(action, "/"), exploratoryName, resourceCreator, t);
            updateExploratoryStatusSilent(resourceCreator, project, exploratoryName, FAILED);
            final String errorMsg = String.format("Could not %s exploratory environment %s: %s",
                    StringUtils.substringAfter(action, "/"), exploratoryName,
                    Optional.ofNullable(t.getCause()).map(Throwable::getMessage).orElse(t.getMessage()));
            throw new DatalabException(errorMsg, t);
        }
    }

    @Audit(action = TERMINATE, type = INSTANCE)
    public void updateExploratoryComputeStatuses(@User UserInfo userInfo, @Project String project, @ResourceName String exploratoryName, UserInstanceStatus status, String resourceCreator) {
        updateExploratoryStatus(resourceCreator, project, exploratoryName, status);
        updateComputationalStatuses(userInfo.getName(), resourceCreator, project, exploratoryName, status);
    }

    private void updateExploratoryComputeStatuses(String user, String project, String exploratoryName, UserInstanceStatus status, String resourceCreator) {
        updateExploratoryStatus(resourceCreator, project, exploratoryName, status);
        updateComputationalStatuses(user, resourceCreator, project, exploratoryName, status);
    }

    private void updateComputationalStatuses(String user, String resourceCreator, String project, String exploratoryName, UserInstanceStatus status) {
        if (status == STOPPING) {
            if (configuration.isAuditEnabled()) {
                saveAudit(user, resourceCreator, project, exploratoryName, STOP, RUNNING);
            }
            updateComputationalStatuses(resourceCreator, project, exploratoryName, STOPPING, TERMINATING, FAILED, TERMINATED, STOPPED);
        } else if (status == TERMINATING) {
            if (configuration.isAuditEnabled()) {
                saveAudit(user, resourceCreator, project, exploratoryName, TERMINATE, RUNNING, STOPPED);
            }
            updateComputationalStatuses(resourceCreator, project, exploratoryName, TERMINATING, TERMINATING, TERMINATED, FAILED);
        }
    }

    private void saveAudit(String user, String resourceCreator, String project, String exploratoryName, AuditActionEnum action, UserInstanceStatus... sparkStatuses) {
        saveAuditForComputational(user, resourceCreator, project, exploratoryName, action, DataEngineType.SPARK_STANDALONE, sparkStatuses);
        saveAuditForComputational(user, resourceCreator, project, exploratoryName, TERMINATE, DataEngineType.CLOUD_SERVICE, RUNNING, STOPPED);
    }

    private void saveAuditForComputational(String user, String resourceCreator, String project, String exploratoryName, AuditActionEnum action, DataEngineType cloudService,
                                           UserInstanceStatus... computationalStatuses) {
        computationalDAO.getComputationalResourcesWhereStatusIn(resourceCreator, project, Collections.singletonList(cloudService),
                exploratoryName, computationalStatuses)
                .forEach(comp -> auditService.save(
                        AuditDTO.builder()
                                .user(user)
                                .resourceName(comp)
                                .project(project)
                                .action(action)
                                .type(AuditResourceTypeEnum.COMPUTE)
                                .build())
                );
    }

    private ExploratoryActionDTO<?> getExploratoryActionDto(UserInfo userInfo, String resourceCreator, UserInstanceStatus status, UserInstanceDTO userInstance,
                                                            EndpointDTO endpointDTO) {
        ExploratoryActionDTO<?> dto;
        if (status != UserInstanceStatus.STARTING) {
            dto = requestBuilder.newExploratoryStop(resourceCreator, userInstance, endpointDTO);
        } else {
            dto = requestBuilder.newExploratoryStart(userInfo, userInstance, endpointDTO, gitCredsDAO.findGitCreds(userInfo.getName()));
        }
        return dto;
    }


    /**
     * Updates the status of exploratory environment.
     *
     * @param user            user name
     * @param project         project name
     * @param exploratoryName name of exploratory environment.
     * @param status          status for exploratory environment.
     */
    private void updateExploratoryStatus(String user, String project, String exploratoryName, UserInstanceStatus status) {
        StatusEnvBaseDTO<?> exploratoryStatus = createStatusDTO(user, project, exploratoryName, status);
        exploratoryDAO.updateExploratoryStatus(exploratoryStatus);
    }

    /**
     * Updates the status of exploratory environment without exceptions. If exception occurred then logging it.
     *
     * @param user            user name
     * @param project         project name
     * @param exploratoryName name of exploratory environment.
     * @param status          status for exploratory environment.
     */
    private void updateExploratoryStatusSilent(String user, String project, String exploratoryName, UserInstanceStatus status) {
        try {
            updateExploratoryStatus(user, project, exploratoryName, status);
        } catch (DatalabException e) {
            log.error("Could not update the status of exploratory environment {} for user {} to {}",
                    exploratoryName, user, status, e);
        }
    }

    private void updateComputationalStatuses(String user, String project, String exploratoryName, UserInstanceStatus
            dataEngineStatus, UserInstanceStatus dataEngineServiceStatus, UserInstanceStatus... excludedStatuses) {
        log.debug("updating status for all computational resources of {} for user {}: DataEngine {}, " +
                "dataengine-service {}", exploratoryName, user, dataEngineStatus, dataEngineServiceStatus);
        computationalDAO.updateComputationalStatusesForExploratory(user, project, exploratoryName,
                dataEngineStatus, dataEngineServiceStatus, excludedStatuses);
    }

    /**
     * Instantiates and returns the descriptor of exploratory environment status.
     *
     * @param user            user name
     * @param project         project
     * @param exploratoryName name of exploratory environment.
     * @param status          status for exploratory environment.
     */
    private StatusEnvBaseDTO<?> createStatusDTO(String user, String project, String exploratoryName, UserInstanceStatus status) {
        return new ExploratoryStatusDTO()
                .withUser(user)
                .withProject(project)
                .withExploratoryName(exploratoryName)
                .withStatus(status);
    }

    private UserInstanceDTO getUserInstanceDTO(UserInfo userInfo, Exploratory exploratory, String project, CloudProvider cloudProvider) {
        final UserInstanceDTO userInstance = new UserInstanceDTO()
                .withUser(userInfo.getName())
                .withExploratoryName(exploratory.getName())
                .withStatus(CREATING.toString())
                .withImageName(exploratory.getDockerImage())
                .withImageVersion(exploratory.getVersion())
                .withTemplateName(exploratory.getTemplateName())
                .withClusterConfig(exploratory.getClusterConfig())
                .withShape(exploratory.getShape())
                .withProject(project)
                .withEndpoint(exploratory.getEndpoint())
                .withCloudProvider(cloudProvider.toString())
                .withTags(tagService.getResourceTags(userInfo, exploratory.getEndpoint(), project,
                        exploratory.getExploratoryTag(), exploratory.getEnabledGPU()))
                .withGPUCount(exploratory.getGpuCount())
                .withGPUEnabled(exploratory.getEnabledGPU())
                .withGPUType(exploratory.getGpuType());
        if (StringUtils.isNotBlank(exploratory.getImageName())) {
            final List<LibInstallDTO> libInstallDtoList = getImageRelatedLibraries(exploratory.getImageName(),
                    project, exploratory.getEndpoint());
            userInstance.withLibs(libInstallDtoList);
        }
        return userInstance;
    }

    private List<LibInstallDTO> getImageRelatedLibraries(String imageName, String project,
                                                         String endpoint) {
        final List<Library> libraries = imageExploratoryDao.getLibraries(imageName, project,
                endpoint, LibStatus.INSTALLED);
        return toLibInstallDtoList(libraries);
    }

    private List<LibInstallDTO> toLibInstallDtoList(List<Library> libraries) {
        return libraries
                .stream()
                .map(this::toLibInstallDto)
                .collect(Collectors.toList());
    }

    private LibInstallDTO toLibInstallDto(Library l) {
        return new LibInstallDTO(l.getGroup(), l.getName(), l.getVersion())
                .withStatus(String.valueOf(l.getStatus()))
                .withAddedPackages(l.getAddedPackages())
                .withErrorMessage(l.getErrorMessage());
    }
}
