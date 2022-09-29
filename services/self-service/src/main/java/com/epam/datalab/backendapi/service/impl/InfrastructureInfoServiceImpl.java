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
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.domain.*;
import com.epam.datalab.backendapi.resources.dto.HealthStatusEnum;
import com.epam.datalab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.datalab.backendapi.resources.dto.ProjectInfrastructureInfo;
import com.epam.datalab.backendapi.roles.RoleType;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.BillingService;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.InfrastructureInfoService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.InfrastructureMetaInfoDTO;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.aws.edge.EdgeInfoAws;
import com.epam.datalab.dto.azure.edge.EdgeInfoAzure;
import com.epam.datalab.dto.base.edge.EdgeInfo;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.dto.gcp.edge.EdgeInfoGcp;
import com.epam.datalab.dto.status.EnvResource;
import com.epam.datalab.dto.status.EnvResourceList;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jcabi.manifests.Manifests;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class InfrastructureInfoServiceImpl implements InfrastructureInfoService {

    private static final String RELEASE_NOTES_FORMAT = "https://github.com/apache/incubator-datalab/blob/%s/RELEASE_NOTES.md";
    private static final String PERMISSION_VIEW = "/api/bucket/view";
    private static final String PERMISSION_UPLOAD = "/api/bucket/upload";
    private static final String PERMISSION_DOWNLOAD = "/api/bucket/download";
    private static final String PERMISSION_DELETE = "/api/bucket/delete";

    private static final String CONNECTED_PLATFORMS_PERMISSION_VIEW = "/api/connected_platforms/view";
    private static final String CONNECTED_PLATFORMS_PERMISSION_ADD = "/api/connected_platforms/add";
    private static final String CONNECTED_PLATFORMS_PERMISSION_DISCONNECT = "/api/connected_platforms/disconnect";

    private static final String INFRASTRUCTURE_STATUS = "infrastructure/status";

    private final ExploratoryDAO expDAO;
    private final SelfServiceApplicationConfiguration configuration;
    private final ProjectService projectService;
    private final EndpointService endpointService;
    private final BillingService billingService;
    private final RequestBuilder requestBuilder;
    private final RESTService provisioningService;
    private final RequestId requestId;


    @Inject
    public InfrastructureInfoServiceImpl(ExploratoryDAO expDAO, SelfServiceApplicationConfiguration configuration, ProjectService projectService,
                                         EndpointService endpointService, BillingService billingService, RequestBuilder requestBuilder,
                                         @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService, RequestId requestId) {
        this.expDAO = expDAO;
        this.configuration = configuration;
        this.projectService = projectService;
        this.endpointService = endpointService;
        this.billingService = billingService;
        this.requestBuilder = requestBuilder;
        this.provisioningService = provisioningService;
        this.requestId = requestId;
    }

    @Override
    public List<ProjectInfrastructureInfo> getUserResources(UserInfo user) {
        log.debug("Loading list of provisioned resources for user {}", user);
        List<EndpointDTO> allEndpoints = endpointService.getEndpoints();
        return projectService.getUserProjects(user, Boolean.FALSE)
                .stream()
                .map(p -> {
                    List<UserInstanceDTO> exploratories = expDAO.findExploratories(user.getName(), p.getName());
                    return ProjectInfrastructureInfo.builder()
                            .project(p.getName())
                            .billingQuoteUsed(billingService.getBillingProjectQuoteUsed(p.getName()))
                            .shared(getSharedInfo(p.getName()))
                            .exploratory(exploratories)
                            .exploratoryBilling(getExploratoryBillingData(exploratories))
                            .endpoints(getEndpoints(allEndpoints, p))
                            .odahu(p.getOdahu())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public HealthStatusPageDTO getHeathStatus(UserInfo userInfo) {
        log.debug("Request the status of resources for user {}", userInfo.getName());
        return HealthStatusPageDTO.builder()
                .status(HealthStatusEnum.OK.toString())
                .listResources(Collections.emptyList())
                .billingEnabled(configuration.isBillingSchedulerEnabled())
                .auditEnabled(configuration.isAuditEnabled())
                .projectAdmin(UserRoles.isProjectAdmin(userInfo))
                .admin(UserRoles.isAdmin(userInfo))
                .projectAssigned(projectService.isAnyProjectAssigned(userInfo))
                .bucketBrowser(HealthStatusPageDTO.BucketBrowser.builder()
                        .view(checkAccess(userInfo, PERMISSION_VIEW))
                        .upload(checkAccess(userInfo, PERMISSION_UPLOAD))
                        .download(checkAccess(userInfo, PERMISSION_DOWNLOAD))
                        .delete(checkAccess(userInfo, PERMISSION_DELETE))
                        .build())
                .connectedPlatforms(HealthStatusPageDTO.ConnectedPlatforms.builder()
                        .view(checkAccess(userInfo, CONNECTED_PLATFORMS_PERMISSION_VIEW))
                        .add(checkAccess(userInfo, CONNECTED_PLATFORMS_PERMISSION_ADD))
                        .disconnect(checkAccess(userInfo, CONNECTED_PLATFORMS_PERMISSION_DISCONNECT))
                .build())
                .build();
    }

    @Override
    public InfrastructureMetaInfoDTO getInfrastructureMetaInfo() {
        final String branch = Manifests.read("GIT-Branch");
        return InfrastructureMetaInfoDTO.builder()
                .branch(branch)
                .commit(Manifests.read("GIT-Commit"))
                .version(Manifests.read("DataLab-Version"))
                .releaseNotes(String.format(RELEASE_NOTES_FORMAT, branch))
                .build();
    }

    @Override
    public void updateInfrastructureStatuses(UserInfo user, String endpoint, List<EnvResource> hostInstances, List<EnvResource> clusterInstances) {
        EnvResourceList envResourceList = EnvResourceList.builder()
                .hostList(hostInstances)
                .clusterList(clusterInstances)
                .build();

        EndpointDTO endpointDTO = endpointService.get(endpoint);
        if (isEmpty(envResourceList)) {
            log.info("EnvResources is empty: {} , didn't send request to provisioning service", envResourceList);
        } else {
            log.info("Send request to provisioning service:\n POST:{}, with EnvResources: {}", INFRASTRUCTURE_STATUS,
                    envResourceList);
            String uuid = provisioningService.post(endpointDTO.getUrl() + INFRASTRUCTURE_STATUS, user.getAccessToken(),
                    requestBuilder.newInfrastructureStatus(user.getName(), endpointDTO.getCloudProvider(), envResourceList),
                    String.class);
            requestId.put(user.getName(), uuid);
        }
    }

    private boolean isEmpty(EnvResourceList envResourceList) {
        return envResourceList.getClusterList().isEmpty() && envResourceList.getHostList().isEmpty();
    }

    private List<BillingReport> getExploratoryBillingData(List<UserInstanceDTO> exploratories) {
        return exploratories
                .stream()
                .map(exp -> billingService.getExploratoryBillingData(exp.getProject(), exp.getEndpoint(),
                        exp.getExploratoryName(), exp.getResources()
                                .stream()
                                .map(UserComputationalResource::getComputationalName)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    private List<EndpointDTO> getEndpoints(List<EndpointDTO> allEndpoints, ProjectDTO projectDTO) {
        return allEndpoints
                .stream()
                .filter(endpoint -> projectDTO.getEndpoints()
                        .stream()
                        .anyMatch(endpoint1 -> endpoint1.getName().equals(endpoint.getName())))
                .collect(Collectors.toList());
    }

    private Map<String, Map<String, String>> getSharedInfo(String name) {
        return projectService.get(name).getEndpoints()
                .stream()
                .collect(Collectors.toMap(ProjectEndpointDTO::getName, this::getSharedInfo));
    }

    private Map<String, String> getSharedInfo(ProjectEndpointDTO endpointDTO) {
        Optional<EdgeInfo> edgeInfo = Optional.ofNullable(endpointDTO.getEdgeInfo());
        if (!edgeInfo.isPresent()) {
            return Collections.emptyMap();
        }
        EdgeInfo edge = edgeInfo.get();
        Map<String, String> shared = new HashMap<>();

        shared.put("status", endpointDTO.getStatus().toString());
        shared.put("edge_node_ip", edge.getPublicIp());

        if (edge instanceof EdgeInfoAws) {
            EdgeInfoAws edgeInfoAws = (EdgeInfoAws) edge;
            shared.put("user_own_bicket_name", edgeInfoAws.getUserOwnBucketName());
            shared.put("shared_bucket_name", edgeInfoAws.getSharedBucketName());
        } else if (edge instanceof EdgeInfoAzure) {
            EdgeInfoAzure edgeInfoAzure = (EdgeInfoAzure) edge;
            shared.put("user_container_name", edgeInfoAzure.getUserContainerName());
            shared.put("shared_container_name", edgeInfoAzure.getSharedContainerName());
            shared.put("user_storage_account_name", edgeInfoAzure.getUserStorageAccountName());
            shared.put("shared_storage_account_name", edgeInfoAzure.getSharedStorageAccountName());
            shared.put("datalake_name", edgeInfoAzure.getDataLakeName());
            shared.put("datalake_user_directory_name", edgeInfoAzure.getDataLakeDirectoryName());
            shared.put("datalake_shared_directory_name", edgeInfoAzure.getDataLakeSharedDirectoryName());
        } else if (edge instanceof EdgeInfoGcp) {
            EdgeInfoGcp edgeInfoGcp = (EdgeInfoGcp) edge;
            shared.put("user_own_bucket_name", edgeInfoGcp.getUserOwnBucketName());
            shared.put("shared_bucket_name", edgeInfoGcp.getSharedBucketName());
        }

        return shared;
    }

    private boolean checkAccess(UserInfo userInfo, String permission) {
        return UserRoles.checkAccess(userInfo, RoleType.PAGE, permission, userInfo.getRoles());
    }
}
