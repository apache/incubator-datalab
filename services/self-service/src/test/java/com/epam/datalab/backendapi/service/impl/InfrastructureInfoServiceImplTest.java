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
import com.epam.datalab.backendapi.domain.BillingReport;
import com.epam.datalab.backendapi.domain.BillingReportLine;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectEndpointDTO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.resources.TestBase;
import com.epam.datalab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.datalab.backendapi.resources.dto.ProjectInfrastructureInfo;
import com.epam.datalab.backendapi.service.BillingService;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.InfrastructureMetaInfoDTO;
import com.epam.datalab.dto.UserEnvironmentResources;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.aws.edge.EdgeInfoAws;
import com.epam.datalab.dto.azure.edge.EdgeInfoAzure;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.dto.billing.BillingResourceType;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.dto.gcp.edge.EdgeInfoGcp;
import com.epam.datalab.dto.status.EnvResource;
import com.epam.datalab.dto.status.EnvResourceList;
import com.epam.datalab.rest.client.RESTService;
import com.jcabi.manifests.Manifests;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InfrastructureInfoServiceImplTest extends TestBase {

    private static final String PROJECT = "project";
    private static final String EXPLORATORY_NAME = "exploratoryName";
    private static final String COMPUTE_NAME = "computeName";
    private static final String CURRENCY = "currency";
    private static final String UUID = "uuid";
    private static final String INFRASTRUCTURE_STATUS = "infrastructure/status";

    @Mock
    private ExploratoryDAO expDAO;
    @Mock
    private SelfServiceApplicationConfiguration configuration;
    @Mock
    private ProjectService projectService;
    @Mock
    private EndpointService endpointService;
    @Mock
    private BillingService billingService;
    @Mock
    private RESTService provisioningService;
    @Mock
    private RequestBuilder requestBuilder;
    @Mock
    private RequestId requestId;

    @InjectMocks
    private InfrastructureInfoServiceImpl infoService;

    @Test
    public void getUserResources() {
        when(endpointService.getEndpoints()).thenReturn(Collections.singletonList(getEndpointDTO()));
        when(projectService.getUserProjects(any(UserInfo.class), anyBoolean())).thenReturn(Collections.singletonList(getProjectDTO()));
        when(expDAO.findExploratories(anyString(), anyString())).thenReturn(getUserInstanceDTOs());
        when(billingService.getBillingProjectQuoteUsed(anyString())).thenReturn(10);
        when(projectService.get(anyString())).thenReturn(getProjectDTO());
        when(billingService.getExploratoryBillingData(anyString(), anyString(), anyString(), anyListOf(String.class))).thenReturn(getReport());

        List<ProjectInfrastructureInfo> actualUserResources = infoService.getUserResources(getUserInfo());

        assertEquals("resources should be equal", getProjectInfrastructureInfo(), actualUserResources);
        verify(endpointService).getEndpoints();
        verify(projectService).getUserProjects(getUserInfo(), Boolean.FALSE);
        verify(expDAO).findExploratories(USER.toLowerCase(), PROJECT);
        verify(billingService).getBillingProjectQuoteUsed(PROJECT);
        verify(projectService).get(PROJECT);
        verify(billingService).getExploratoryBillingData(PROJECT, ENDPOINT_NAME, EXPLORATORY_NAME, Collections.singletonList(COMPUTE_NAME));
        verifyNoMoreInteractions(endpointService, projectService, expDAO, billingService);
    }

    @Test
    public void getAwsUserResources() {
        when(endpointService.getEndpoints()).thenReturn(Collections.singletonList(getEndpointDTO()));
        when(projectService.getUserProjects(any(UserInfo.class), anyBoolean())).thenReturn(Collections.singletonList(getAwsProjectDTO()));
        when(expDAO.findExploratories(anyString(), anyString())).thenReturn(getUserInstanceDTOs());
        when(billingService.getBillingProjectQuoteUsed(anyString())).thenReturn(10);
        when(projectService.get(anyString())).thenReturn(getAwsProjectDTO());
        when(billingService.getExploratoryBillingData(anyString(), anyString(), anyString(), anyListOf(String.class))).thenReturn(getReport());

        List<ProjectInfrastructureInfo> actualUserResources = infoService.getUserResources(getUserInfo());

        assertEquals("resources should be equal", getAwsProjectInfrastructureInfo(), actualUserResources);
        verify(endpointService).getEndpoints();
        verify(projectService).getUserProjects(getUserInfo(), Boolean.FALSE);
        verify(expDAO).findExploratories(USER.toLowerCase(), PROJECT);
        verify(billingService).getBillingProjectQuoteUsed(PROJECT);
        verify(projectService).get(PROJECT);
        verify(billingService).getExploratoryBillingData(PROJECT, ENDPOINT_NAME, EXPLORATORY_NAME, Collections.singletonList(COMPUTE_NAME));
        verifyNoMoreInteractions(endpointService, projectService, expDAO, billingService);
    }

    @Test
    public void getAzureUserResources() {
        when(endpointService.getEndpoints()).thenReturn(Collections.singletonList(getEndpointDTO()));
        when(projectService.getUserProjects(any(UserInfo.class), anyBoolean())).thenReturn(Collections.singletonList(getAzureProjectDTO()));
        when(expDAO.findExploratories(anyString(), anyString())).thenReturn(getUserInstanceDTOs());
        when(billingService.getBillingProjectQuoteUsed(anyString())).thenReturn(10);
        when(projectService.get(anyString())).thenReturn(getAzureProjectDTO());
        when(billingService.getExploratoryBillingData(anyString(), anyString(), anyString(), anyListOf(String.class))).thenReturn(getReport());

        List<ProjectInfrastructureInfo> actualUserResources = infoService.getUserResources(getUserInfo());

        assertEquals("resources should be equal", getAzureProjectInfrastructureInfo(), actualUserResources);
        verify(endpointService).getEndpoints();
        verify(projectService).getUserProjects(getUserInfo(), Boolean.FALSE);
        verify(expDAO).findExploratories(USER.toLowerCase(), PROJECT);
        verify(billingService).getBillingProjectQuoteUsed(PROJECT);
        verify(projectService).get(PROJECT);
        verify(billingService).getExploratoryBillingData(PROJECT, ENDPOINT_NAME, EXPLORATORY_NAME, Collections.singletonList(COMPUTE_NAME));
        verifyNoMoreInteractions(endpointService, projectService, expDAO, billingService);
    }

    @Test
    public void getGcpUserResources() {
        when(endpointService.getEndpoints()).thenReturn(Collections.singletonList(getEndpointDTO()));
        when(projectService.getUserProjects(any(UserInfo.class), anyBoolean())).thenReturn(Collections.singletonList(getGcpProjectDTO()));
        when(expDAO.findExploratories(anyString(), anyString())).thenReturn(getUserInstanceDTOs());
        when(billingService.getBillingProjectQuoteUsed(anyString())).thenReturn(10);
        when(projectService.get(anyString())).thenReturn(getGcpProjectDTO());
        when(billingService.getExploratoryBillingData(anyString(), anyString(), anyString(), anyListOf(String.class))).thenReturn(getReport());

        List<ProjectInfrastructureInfo> actualUserResources = infoService.getUserResources(getUserInfo());

        assertEquals("resources should be equal", getGcpProjectInfrastructureInfo(), actualUserResources);
        verify(endpointService).getEndpoints();
        verify(projectService).getUserProjects(getUserInfo(), Boolean.FALSE);
        verify(expDAO).findExploratories(USER.toLowerCase(), PROJECT);
        verify(billingService).getBillingProjectQuoteUsed(PROJECT);
        verify(projectService).get(PROJECT);
        verify(billingService).getExploratoryBillingData(PROJECT, ENDPOINT_NAME, EXPLORATORY_NAME, Collections.singletonList(COMPUTE_NAME));
        verifyNoMoreInteractions(endpointService, projectService, expDAO, billingService);
    }

    @Test
    public void getHeathStatus() {
        when(configuration.isBillingSchedulerEnabled()).thenReturn(Boolean.TRUE);
        when(configuration.isAuditEnabled()).thenReturn(Boolean.TRUE);
        when(projectService.isAnyProjectAssigned(any(UserInfo.class))).thenReturn(Boolean.TRUE);

        HealthStatusPageDTO actualHeathStatus = infoService.getHeathStatus(getUserInfo());

        assertEquals("HealthStatusPageDTO should be equal", getHealthStatusPageDTO(), actualHeathStatus);
        verify(projectService).isAnyProjectAssigned(getUserInfo());
        verify(configuration).isBillingSchedulerEnabled();
        verify(configuration).isAuditEnabled();
        verifyNoMoreInteractions(configuration, projectService);
    }

    @Test
    public void getInfrastructureMetaInfo() {
        Manifests.DEFAULT.put("GIT-Branch", "branch");
        Manifests.DEFAULT.put("GIT-Commit", "commit");
        Manifests.DEFAULT.put("DataLab-Version", "version");

        InfrastructureMetaInfoDTO actualInfrastructureMetaInfo = infoService.getInfrastructureMetaInfo();

        assertEquals("InfrastructureMetaInfoDTO should be equal", getInfrastructureMetaInfoDTO(), actualInfrastructureMetaInfo);
    }

    @Test
    public void updateInfrastructureStatuses() {
        List<EnvResource> envResources = Collections.singletonList(new EnvResource().withId("id"));
        when(endpointService.get(anyString())).thenReturn(getEndpointDTO());
        when(provisioningService.post(anyString(), anyString(), any(UserEnvironmentResources.class), any())).thenReturn(UUID);
        when(requestBuilder.newInfrastructureStatus(anyString(), any(CloudProvider.class), any(EnvResourceList.class))).thenReturn(
                new UserEnvironmentResources());

        infoService.updateInfrastructureStatuses(getUserInfo(), ENDPOINT_NAME, envResources, envResources);

        verify(endpointService).get(ENDPOINT_NAME);
        verify(requestBuilder).newInfrastructureStatus(USER.toLowerCase(), CloudProvider.AWS, EnvResourceList.builder()
                .hostList(envResources)
                .clusterList(envResources)
                .build());
        verify(provisioningService).post(ENDPOINT_URL + INFRASTRUCTURE_STATUS, TOKEN, new UserEnvironmentResources(), String.class);
        verify(requestId).put(USER.toLowerCase(), UUID);
        verifyNoMoreInteractions(endpointService, provisioningService, requestBuilder, requestId);
    }

    private InfrastructureMetaInfoDTO getInfrastructureMetaInfoDTO() {
        return InfrastructureMetaInfoDTO.builder()
                .branch("branch")
                .commit("commit")
                .version("version")
                .releaseNotes("https://github.com/apache/incubator-datalab/blob/branch/RELEASE_NOTES.md")
                .build();
    }

    private HealthStatusPageDTO getHealthStatusPageDTO() {
        return HealthStatusPageDTO.builder()
                .status("ok")
                .listResources(Collections.emptyList())
                .billingEnabled(Boolean.TRUE)
                .auditEnabled(Boolean.TRUE)
                .projectAdmin(Boolean.FALSE)
                .admin(Boolean.FALSE)
                .projectAssigned(Boolean.TRUE)
                .bucketBrowser(HealthStatusPageDTO.BucketBrowser.builder()
                        .view(Boolean.TRUE)
                        .upload(Boolean.TRUE)
                        .download(Boolean.TRUE)
                        .delete(Boolean.TRUE)
                        .build())
                .connectedPlatforms(HealthStatusPageDTO.ConnectedPlatforms.builder()
                        .view(Boolean.TRUE)
                        .add(Boolean.TRUE)
                        .disconnect(Boolean.TRUE)
                        .build())
                .build();
    }

    private List<ProjectInfrastructureInfo> getProjectInfrastructureInfo() {
        List<ProjectInfrastructureInfo> objects = new ArrayList<>();
        objects.add(ProjectInfrastructureInfo.builder()
                .project(PROJECT)
                .billingQuoteUsed(10)
                .shared(Collections.singletonMap(ENDPOINT_NAME, Collections.emptyMap()))
                .exploratory(getUserInstanceDTOs())
                .exploratoryBilling(Collections.singletonList(getReport()))
                .endpoints(Collections.singletonList(getEndpointDTO()))
                .odahu(Collections.emptyList())
                .build());
        return objects;
    }

    private List<ProjectInfrastructureInfo> getAwsProjectInfrastructureInfo() {
        List<ProjectInfrastructureInfo> objects = new ArrayList<>();
        objects.add(ProjectInfrastructureInfo.builder()
                .project(PROJECT)
                .billingQuoteUsed(10)
                .shared(Collections.singletonMap(ENDPOINT_NAME, getAwsEdgeInfo()))
                .exploratory(getUserInstanceDTOs())
                .exploratoryBilling(Collections.singletonList(getReport()))
                .endpoints(Collections.singletonList(getEndpointDTO()))
                .odahu(Collections.emptyList())
                .build());
        return objects;
    }

    private List<ProjectInfrastructureInfo> getAzureProjectInfrastructureInfo() {
        List<ProjectInfrastructureInfo> objects = new ArrayList<>();
        objects.add(ProjectInfrastructureInfo.builder()
                .project(PROJECT)
                .billingQuoteUsed(10)
                .shared(Collections.singletonMap(ENDPOINT_NAME, getAzureEdgeInfo()))
                .exploratory(getUserInstanceDTOs())
                .exploratoryBilling(Collections.singletonList(getReport()))
                .endpoints(Collections.singletonList(getEndpointDTO()))
                .odahu(Collections.emptyList())
                .build());
        return objects;
    }

    private List<ProjectInfrastructureInfo> getGcpProjectInfrastructureInfo() {
        List<ProjectInfrastructureInfo> objects = new ArrayList<>();
        objects.add(ProjectInfrastructureInfo.builder()
                .project(PROJECT)
                .billingQuoteUsed(10)
                .shared(Collections.singletonMap(ENDPOINT_NAME, getGcpEdgeInfo()))
                .exploratory(getUserInstanceDTOs())
                .exploratoryBilling(Collections.singletonList(getReport()))
                .endpoints(Collections.singletonList(getEndpointDTO()))
                .odahu(Collections.emptyList())
                .build());
        return objects;
    }

    private Map<String, String> getAwsEdgeInfo() {
        HashMap<String, String> edge = new HashMap<>();
        edge.put("status", "running");
        edge.put("edge_node_ip", "publicIp");
        edge.put("user_own_bicket_name", "ownBucketName");
        edge.put("shared_bucket_name", "sharedBucketName");
        return edge;
    }

    private Map<String, String> getAzureEdgeInfo() {
        HashMap<String, String> edge = new HashMap<>();
        edge.put("status", "running");
        edge.put("edge_node_ip", "publicIp");
        edge.put("user_container_name", "userContainerName");
        edge.put("shared_container_name", "sharedContainerName");
        edge.put("user_storage_account_name", "userStorageAccountName");
        edge.put("shared_storage_account_name", "sharedStorageAccountName");
        edge.put("datalake_name", "dataLakeName");
        edge.put("datalake_user_directory_name", "dataLakeDirectoryName");
        edge.put("datalake_shared_directory_name", "dataLakeSharedDirectoryName");
        return edge;
    }

    private Map<String, String> getGcpEdgeInfo() {
        HashMap<String, String> edge = new HashMap<>();
        edge.put("status", "running");
        edge.put("edge_node_ip", "publicIp");
        edge.put("user_own_bucket_name", "ownBucketName");
        edge.put("shared_bucket_name", "sharedBucketName");
        return edge;
    }

    private ProjectDTO getProjectDTO() {
        return ProjectDTO.builder()
                .name(PROJECT)
                .endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING, null)))
                .build();
    }

    private ProjectDTO getAwsProjectDTO() {
        EdgeInfoAws edgeInfoAws = new EdgeInfoAws();
        edgeInfoAws.setPublicIp("publicIp");
        edgeInfoAws.setUserOwnBucketName("ownBucketName");
        edgeInfoAws.setSharedBucketName("sharedBucketName");

        return ProjectDTO.builder()
                .name(PROJECT)
                .endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING, edgeInfoAws)))
                .build();
    }

    private ProjectDTO getAzureProjectDTO() {
        EdgeInfoAzure edgeInfoAzure = new EdgeInfoAzure();
        edgeInfoAzure.setPublicIp("publicIp");
        edgeInfoAzure.setUserContainerName("userContainerName");
        edgeInfoAzure.setSharedContainerName("sharedContainerName");
        edgeInfoAzure.setUserStorageAccountName("userStorageAccountName");
        edgeInfoAzure.setSharedStorageAccountName("sharedStorageAccountName");
        edgeInfoAzure.setDataLakeName("dataLakeName");
        edgeInfoAzure.setDataLakeDirectoryName("dataLakeDirectoryName");
        edgeInfoAzure.setDataLakeSharedDirectoryName("dataLakeSharedDirectoryName");

        return ProjectDTO.builder()
                .name(PROJECT)
                .endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING, edgeInfoAzure)))
                .build();
    }

    private ProjectDTO getGcpProjectDTO() {
        EdgeInfoGcp edgeInfoGcp = new EdgeInfoGcp();
        edgeInfoGcp.setPublicIp("publicIp");
        edgeInfoGcp.setUserOwnBucketName("ownBucketName");
        edgeInfoGcp.setSharedBucketName("sharedBucketName");

        return ProjectDTO.builder()
                .name(PROJECT)
                .endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING, edgeInfoGcp)))
                .build();
    }

    private List<UserInstanceDTO> getUserInstanceDTOs() {
        return Collections.singletonList(
                new UserInstanceDTO().withUser(USER).withProject(PROJECT).withExploratoryName(EXPLORATORY_NAME).withEndpoint(ENDPOINT_NAME)
                        .withResources(Collections.singletonList(getCompute()))
        );
    }

    private UserComputationalResource getCompute() {
        UserComputationalResource resource = new UserComputationalResource();
        resource.setComputationalName(COMPUTE_NAME);
        resource.setImageName(DataEngineType.SPARK_STANDALONE.getName());

        return resource;
    }

    private BillingReport getReport() {
        BillingReportLine line1 = BillingReportLine.builder().cost(1.0).user(USER).resourceType(BillingResourceType.EXPLORATORY).project(PROJECT).endpoint(ENDPOINT_NAME)
                .resourceName(EXPLORATORY_NAME).currency(CURRENCY).build();
        BillingReportLine line2 = BillingReportLine.builder().cost(1.0).user(USER).resourceType(BillingResourceType.COMPUTATIONAL).project(PROJECT).endpoint(ENDPOINT_NAME)
                .resourceName(COMPUTE_NAME).exploratoryName(EXPLORATORY_NAME).currency(CURRENCY).build();
        List<BillingReportLine> billingReportLines = Arrays.asList(line1, line2);

        return BillingReport.builder()
                .name(EXPLORATORY_NAME)
                .reportLines(billingReportLines)
                .totalCost(2.0)
                .currency(CURRENCY)
                .build();
    }
}