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
import com.epam.datalab.backendapi.dao.BillingDAO;
import com.epam.datalab.backendapi.dao.ImageExploratoryDAO;
import com.epam.datalab.backendapi.dao.ProjectDAO;
import com.epam.datalab.backendapi.domain.BillingReport;
import com.epam.datalab.backendapi.domain.BillingReportLine;
import com.epam.datalab.backendapi.domain.BudgetDTO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectEndpointDTO;
import com.epam.datalab.backendapi.resources.TestBase;
import com.epam.datalab.backendapi.resources.dto.BillingFilter;
import com.epam.datalab.backendapi.resources.dto.ExportBillingFilter;
import com.epam.datalab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.datalab.backendapi.resources.dto.QuotaUsageDTO;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.ExploratoryService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.dto.billing.BillingData;
import com.epam.datalab.dto.billing.BillingResourceType;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.dto.exploratory.ImageStatus;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.GenericType;
import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BillingServiceImplTest extends TestBase {

    private static final String PROJECT = "project";
    private static final String PROJECT_2 = "project2";
    private static final String ENDPOINT = "endpoint";
    private static final String USAGE_DATE = "2020-06-00";
    private static final String USAGE_DATE_FORMATTED = "2020-06-00";
    private static final String SERVICE_BASE_NAME = "sbn";
    private static final String IMAGE_NAME = "image_name";
    private static final String IMAGE_DESCRIPTION = "imageDescription";
    private static final String IMAGE_APPLICATION = "image_application";
    private static final String TEMPLATE_NAME = "template_name";
    private static final String IMAGE_FULL_NAME = "imageFullName";
    private static final String BILLING_URL = "http://localhost:8088/api/billing";
    private static final String EXPLORATORY_NAME = "exploratoryName";
    private static final String COMPUTE_NAME = "computeName";
    private static final String COMPUTE_NAME_2 = "computeName2";
    private static final String CURRENCY = "currency";
    private static final String PRODUCT = "product";
    private static final String SHAPE = "shape";

    private static final String SHARED_RESOURCE = "Shared resource";
    private static final String SSN_ID = SERVICE_BASE_NAME + "-ssn";
    private static final String SSN_VOLUME_ID = SERVICE_BASE_NAME + "-ssn" + "-volume-primary";
    private static final String EDGE_ID_1 = SERVICE_BASE_NAME + "-" + PROJECT + "-" + ENDPOINT + "-edge";
    private static final String EDGE_VOLUME_ID_1 = SERVICE_BASE_NAME + "-" + PROJECT + "-" + ENDPOINT + "-edge-volume-primary";
    private static final String ENDPOINT_BUCKET_ID = SERVICE_BASE_NAME + "-" + PROJECT + "-" + ENDPOINT + "-bucket";
    private static final String PROJECT_ENDPOINT_BUCKET_ID_1 = SERVICE_BASE_NAME + "-" + ENDPOINT + "-shared-bucket";
    private static final String ENDPOINT_ID_1 = SERVICE_BASE_NAME + "-" + ENDPOINT + "-endpoint";
    private static final String EXPLORATORY_ID = "exploratory_id";
    private static final String EXPLORATORY_VOLUME_PRIMARY_ID_1 = EXPLORATORY_ID + "-volume-primary";
    private static final String EXPLORATORY_SECONDARY_ID_1 = EXPLORATORY_ID + "-volume-secondary";
    private static final String COMPUTE_ID = "compute_id";
    private static final String COMPUTE_VOLUME_PRIMARY_ID = COMPUTE_ID + "-volume-primary";
    private static final String COMPUTE_VOLUME_SECONDARY_ID = COMPUTE_ID + "-volume-secondary";
    private static final String COMPUTE_MASTER_VOLUME_PRIMARY_ID = COMPUTE_ID + "-m" + "-volume-primary";
    private static final String COMPUTE_MASTER_VOLUME_SECONDARY_ID = COMPUTE_ID + "-m" + "-volume-secondary";
    private static final String COMPUTE_SLAVE_1_VOLUME_PRIMARY_ID = COMPUTE_ID + "-s1" + "-volume-primary";
    private static final String COMPUTE_SLAVE_1_VOLUME_SECONDARY_ID = COMPUTE_ID + "-s1" + "-volume-secondary";
    private static final String COMPUTE_SLAVE_2_VOLUME_PRIMARY_ID = COMPUTE_ID + "-s2" + "-volume-primary";
    private static final String COMPUTE_SLAVE_2_VOLUME_SECONDARY_ID = COMPUTE_ID + "-s2" + "-volume-secondary";
    private static final String IMAGE_ID = SERVICE_BASE_NAME + "-" + PROJECT + "-" + ENDPOINT + "-" + IMAGE_APPLICATION + "-" + IMAGE_NAME;
    private static final Integer BILLING_PORT = 8088;

    @Mock
    private SelfServiceApplicationConfiguration configuration;
    @Mock
    private ProjectDAO projectDAO;
    @Mock
    private ProjectService projectService;
    @Mock
    private BillingDAO billingDAO;
    @Mock
    private EndpointService endpointService;
    @Mock
    private RESTService provisioningService;
    @Mock
    private ExploratoryService exploratoryService;
    @Mock
    private ImageExploratoryDAO imageExploratoryDAO;

    @InjectMocks
    private BillingServiceImpl billingService;

    @Test
    public void getBillingReport() {
        when(billingDAO.aggregateBillingData(any(BillingFilter.class))).thenReturn(getBillingReportLineWithCost());
        when(projectService.get(anyString())).thenReturn(getProjectDTO(Boolean.FALSE));
        when(configuration.getServiceBaseName()).thenReturn(SERVICE_BASE_NAME);
        when(exploratoryService.getUserInstance(anyString(), anyString(), anyString())).thenReturn(Optional.of(getUserInstanceDTO()));
        when(exploratoryService.getUserInstance(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(Optional.of(getUserInstanceDTOWithCompute()));

        BillingReport actualBillingReport = billingService.getBillingReport(getUserInfo(), new BillingFilter());

        assertEquals("reports should be equal", getExpectedBillingReport(), actualBillingReport);
        verify(billingDAO).aggregateBillingData(new BillingFilter());
        verify(projectService).get(PROJECT);
        verify(exploratoryService).getUserInstance(USER, PROJECT, EXPLORATORY_NAME);
        verify(exploratoryService,times(2)).getUserInstance(USER, PROJECT, EXPLORATORY_NAME, Boolean.TRUE);
        verifyNoMoreInteractions(billingDAO);
    }

    @Test
    public void getBillingReportWithNullCurrency() {
        when(billingDAO.aggregateBillingData(any(BillingFilter.class))).thenReturn(getBillingReportLineWithDifferentCurrency());
        when(configuration.getServiceBaseName()).thenReturn(SERVICE_BASE_NAME);

        BillingReport actualBillingReport = billingService.getBillingReport(getUserInfo(), new BillingFilter());

        assertEquals("reports should be equal", getExpectedBillingReportWithNullCurrency(), actualBillingReport);
        verify(billingDAO).aggregateBillingData(new BillingFilter());
        verifyNoMoreInteractions(billingDAO);
    }

    @Test
    public void downloadReport() {
        when(billingDAO.aggregateBillingData(any(BillingFilter.class))).thenReturn(getBillingReportLineWithCost());
        when(projectService.get(anyString())).thenReturn(getProjectDTO(Boolean.FALSE));
        when(configuration.getServiceBaseName()).thenReturn(SERVICE_BASE_NAME);
        when(exploratoryService.getUserInstance(anyString(), anyString(), anyString())).thenReturn(Optional.of(getUserInstanceDTO()));
        when(exploratoryService.getUserInstance(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(Optional.of(getUserInstanceDTOWithCompute()));

        String actualBillingReport = billingService.downloadReport(getUserInfo(), new ExportBillingFilter(), "en-US");
        char[] chars1 = getDownloadReport().toCharArray();
        char[] chars2 = actualBillingReport.toCharArray();
        for (int i = 0; i < getDownloadReport().length(); i++) {
            if (chars1[i] != chars2[i])
                System.out.println(chars1[i] + " = " + chars2[i] + "  i = " + i);
        }

        assertEquals("reports should be equal", getDownloadReport(), actualBillingReport);
        verify(billingDAO).aggregateBillingData(new ExportBillingFilter());
        verify(projectService).get(PROJECT);
        verify(exploratoryService).getUserInstance(USER, PROJECT, EXPLORATORY_NAME);
        verify(exploratoryService,times(2)).getUserInstance(USER, PROJECT, EXPLORATORY_NAME, Boolean.TRUE);
        verifyNoMoreInteractions(billingDAO);
    }

    @Test
    public void getExploratoryBillingData() {
        when(billingDAO.findBillingData(anyString(), anyString(), anyListOf(String.class))).thenReturn(getBillingReportLineWithCost());

        BillingReport actualReport = billingService.getExploratoryBillingData(PROJECT, ENDPOINT, EXPLORATORY_NAME, Arrays.asList(COMPUTE_NAME, COMPUTE_NAME_2));

        assertEquals("reports should be equal", getReport(), actualReport);
        verify(billingDAO).findBillingData(PROJECT, ENDPOINT, Arrays.asList(COMPUTE_NAME, COMPUTE_NAME_2, EXPLORATORY_NAME));
        verifyNoMoreInteractions(billingDAO);
    }

    @Test
    public void getExploratoryBillingDataWithNullCurrency() {
        when(billingDAO.findBillingData(anyString(), anyString(), anyListOf(String.class))).thenReturn(getBillingReportLineWithDifferentCurrency());

        BillingReport actualReport = billingService.getExploratoryBillingData(PROJECT, ENDPOINT, EXPLORATORY_NAME, Arrays.asList(COMPUTE_NAME, COMPUTE_NAME_2));

        assertEquals("reports should be equal", getReportWithNullCurrency(), actualReport);
        verify(billingDAO).findBillingData(PROJECT, ENDPOINT, Arrays.asList(COMPUTE_NAME, COMPUTE_NAME_2, EXPLORATORY_NAME));
        verifyNoMoreInteractions(billingDAO);
    }

    @Test
    public void updateGCPRemoteBillingData() {
        when(configuration.getServiceBaseName()).thenReturn(SERVICE_BASE_NAME);
        when(configuration.getBillingPort()).thenReturn(BILLING_PORT);
        when(configuration.getMaxSparkInstanceCount()).thenReturn(2);
        when(endpointService.getEndpoints()).thenReturn(getGCPEndpointDTO());
        when(provisioningService.get(anyString(), anyString(), any(GenericType.class))).thenReturn(getBillingData());
        when(projectService.getProjects()).thenReturn(getProjectDTOs());
        when(exploratoryService.findAll(anySet())).thenReturn(getUserInstanceDTOs());
        when(imageExploratoryDAO.getImagesForProject(anyString())).thenReturn(getImageInfoRecords());

        billingService.updateRemoteBillingData(getUserInfo());

        verify(endpointService).getEndpoints();
        verify(provisioningService).get(BILLING_URL, TOKEN, new GenericType<List<BillingData>>() {
        });
        verify(projectService).getProjects();
        verify(exploratoryService).findAll(new HashSet<>(getProjectDTOs()));
        verify(imageExploratoryDAO).getImagesForProject(PROJECT);
        verify(billingDAO).deleteByUsageDateRegex(ENDPOINT, USAGE_DATE_FORMATTED);
        verify(billingDAO).save(getBillingReportLine());
        verifyNoMoreInteractions(endpointService, provisioningService, billingDAO, projectService, exploratoryService, imageExploratoryDAO);
    }

    @Test
    public void updateAWSRemoteBillingData() {
        when(configuration.getServiceBaseName()).thenReturn(SERVICE_BASE_NAME);
        when(configuration.getBillingPort()).thenReturn(BILLING_PORT);
        when(configuration.getMaxSparkInstanceCount()).thenReturn(2);
        when(endpointService.getEndpoints()).thenReturn(getAWSEndpointDTO());
        when(provisioningService.get(anyString(), anyString(), any(GenericType.class))).thenReturn(getBillingData());
        when(projectService.getProjects()).thenReturn(getProjectDTOs());
        when(exploratoryService.findAll(anySet())).thenReturn(getUserInstanceDTOs());
        when(imageExploratoryDAO.getImagesForProject(anyString())).thenReturn(getImageInfoRecords());

        billingService.updateRemoteBillingData(getUserInfo());

        verify(endpointService).getEndpoints();
        verify(provisioningService).get(BILLING_URL, TOKEN, new GenericType<List<BillingData>>() {
        });
        verify(projectService).getProjects();
        verify(exploratoryService).findAll(new HashSet<>(getProjectDTOs()));
        verify(imageExploratoryDAO).getImagesForProject(PROJECT);
        verify(billingDAO).deleteByUsageDate(ENDPOINT, USAGE_DATE);
        verify(billingDAO).save(getBillingReportLine());
        verifyNoMoreInteractions(endpointService, provisioningService, billingDAO, projectService, exploratoryService, imageExploratoryDAO);
    }

    @Test
    public void updateAzureRemoteBillingData() {
        when(configuration.getServiceBaseName()).thenReturn(SERVICE_BASE_NAME);
        when(configuration.getBillingPort()).thenReturn(BILLING_PORT);
        when(configuration.getMaxSparkInstanceCount()).thenReturn(2);
        when(endpointService.getEndpoints()).thenReturn(getAzureEndpointDTO());
        when(provisioningService.get(anyString(), anyString(), any(GenericType.class))).thenReturn(getBillingData());
        when(projectService.getProjects()).thenReturn(getProjectDTOs());
        when(exploratoryService.findAll(anySet())).thenReturn(getUserInstanceDTOs());
        when(imageExploratoryDAO.getImagesForProject(anyString())).thenReturn(getImageInfoRecords());

        billingService.updateRemoteBillingData(getUserInfo());

        verify(endpointService).getEndpoints();
        verify(provisioningService).get(BILLING_URL, TOKEN, new GenericType<List<BillingData>>() {
        });
        verify(projectService).getProjects();
        verify(exploratoryService).findAll(new HashSet<>(getProjectDTOs()));
        verify(imageExploratoryDAO).getImagesForProject(PROJECT);
        verify(billingDAO).save(getBillingReportLine());
        verifyNoMoreInteractions(endpointService, provisioningService, billingDAO, projectService, exploratoryService, imageExploratoryDAO);
    }

    @Test(expected = DatalabException.class)
    public void updateRemoteBillingDataWithException1() {
        when(endpointService.getEndpoints()).thenReturn(Collections.emptyList());

        billingService.updateRemoteBillingData(getUserInfo());
    }

    @Test
    public void updateRemoteBillingDataWithException2() {
        when(configuration.getBillingPort()).thenReturn(BILLING_PORT);
        when(endpointService.getEndpoints()).thenReturn(getAWSEndpointDTO());
        when(provisioningService.get(anyString(), anyString(), any(GenericType.class))).thenThrow(new DatalabException("Exception message"));

        billingService.updateRemoteBillingData(getUserInfo());

        verify(endpointService).getEndpoints();
        verify(provisioningService).get(BILLING_URL, TOKEN, new GenericType<List<BillingData>>() {
        });
        verifyNoMoreInteractions(endpointService, provisioningService, billingDAO, projectService, exploratoryService, imageExploratoryDAO);
    }

    @Test
    public void updateRemoteBillingDataWithException3() {
        when(endpointService.getEndpoints()).thenReturn(getEndpointDTOWithWrongUrl());

        billingService.updateRemoteBillingData(getUserInfo());

        verify(endpointService).getEndpoints();
        verifyNoMoreInteractions(endpointService, provisioningService, billingDAO, projectService, exploratoryService, imageExploratoryDAO);
    }

    @Test
    public void getQuotas() {
        when(billingDAO.getBillingQuoteUsed()).thenReturn(50);
        when(projectService.getProjects(any(UserInfo.class))).thenReturn(getProjectDTOs(Boolean.FALSE));
        when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
        when(projectService.get(anyString())).thenReturn(getProjectDTO(Boolean.FALSE));
        when(billingDAO.getOverallProjectCost(anyString())).thenReturn(5d);

        QuotaUsageDTO quotas = billingService.getQuotas(getUserInfo());

        assertEquals("QuotaUsageDTO should be equal", getQuotaUsageDTO(), quotas);
        verify(billingDAO).getBillingQuoteUsed();
        verify(projectService).getProjects(getUserInfo());
        verify(projectDAO).getAllowedBudget(PROJECT);
        verify(projectDAO).getAllowedBudget(PROJECT_2);
        verify(projectService).get(PROJECT);
        verify(projectService).get(PROJECT_2);
        verify(billingDAO).getOverallProjectCost(PROJECT);
        verify(billingDAO).getOverallProjectCost(PROJECT_2);
        verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
    }

    @Test
    public void getMonthlyQuotas() {
        when(billingDAO.getBillingQuoteUsed()).thenReturn(50);
        when(projectService.getProjects(any(UserInfo.class))).thenReturn(getProjectDTOs(Boolean.FALSE));
        when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
        when(projectService.get(anyString())).thenReturn(getProjectDTO(Boolean.TRUE));
        when(billingDAO.getMonthlyProjectCost(anyString(), any(LocalDate.class))).thenReturn(5d);

        QuotaUsageDTO quotas = billingService.getQuotas(getUserInfo());

        assertEquals("QuotaUsageDTO should be equal", getQuotaUsageDTO(), quotas);
        verify(billingDAO).getBillingQuoteUsed();
        verify(projectService).getProjects(getUserInfo());
        verify(projectDAO).getAllowedBudget(PROJECT);
        verify(projectDAO).getAllowedBudget(PROJECT_2);
        verify(projectService).get(PROJECT);
        verify(projectService).get(PROJECT_2);
        verify(billingDAO).getMonthlyProjectCost(PROJECT, LocalDate.now());
        verify(billingDAO).getMonthlyProjectCost(PROJECT_2, LocalDate.now());
        verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
    }

    @Test
    public void isProjectQuoteReached() {
        when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
        when(projectService.get(anyString())).thenReturn(getProjectDTO(Boolean.FALSE));
        when(billingDAO.getOverallProjectCost(anyString())).thenReturn(5d);

        final boolean projectQuoteReached = billingService.isProjectQuoteReached(PROJECT);

        assertEquals("quotes should be equal", Boolean.FALSE, projectQuoteReached);
        verify(projectDAO).getAllowedBudget(PROJECT);
        verify(projectService).get(PROJECT);
        verify(billingDAO).getOverallProjectCost(PROJECT);
        verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
    }

    @Test
    public void isProjectQuoteNullReached() {
        when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
        when(projectService.get(anyString())).thenReturn(getProjectQuoteNullDTO());
        when(billingDAO.getOverallProjectCost(anyString())).thenReturn(5d);

        final boolean projectQuoteReached = billingService.isProjectQuoteReached(PROJECT);

        assertEquals("quotes should be equal", Boolean.FALSE, projectQuoteReached);
        verify(projectDAO).getAllowedBudget(PROJECT);
        verify(projectService).get(PROJECT);
        verify(billingDAO).getOverallProjectCost(PROJECT);
        verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
    }

    @Test
    public void isProjectMonthlyQuoteReached() {
        when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
        when(projectService.get(anyString())).thenReturn(getProjectDTO(Boolean.TRUE));
        when(billingDAO.getMonthlyProjectCost(anyString(), any(LocalDate.class))).thenReturn(5d);

        final boolean projectQuoteReached = billingService.isProjectQuoteReached(PROJECT);

        assertEquals("quotes should be equal", Boolean.FALSE, projectQuoteReached);
        verify(projectDAO).getAllowedBudget(PROJECT);
        verify(projectService).get(PROJECT);
        verify(billingDAO).getMonthlyProjectCost(PROJECT, LocalDate.now());
        verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
    }

    @Test
    public void getBillingProjectQuoteUsed() {
        when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
        when(projectService.get(anyString())).thenReturn(getProjectDTO(Boolean.FALSE));
        when(billingDAO.getOverallProjectCost(anyString())).thenReturn(5d);

        final int billingProjectQuoteUsed = billingService.getBillingProjectQuoteUsed(PROJECT);

        assertEquals("quotes should be equal", 50, billingProjectQuoteUsed);
        verify(projectDAO).getAllowedBudget(PROJECT);
        verify(projectService).get(PROJECT);
        verify(billingDAO).getOverallProjectCost(PROJECT);
        verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
    }

    @Test
    public void getBillingProjectQuoteNullUsed() {
        when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
        when(projectService.get(anyString())).thenReturn(getProjectQuoteNullDTO());
        when(billingDAO.getOverallProjectCost(anyString())).thenReturn(5d);

        final int billingProjectQuoteUsed = billingService.getBillingProjectQuoteUsed(PROJECT);

        assertEquals("quotes should be equal", 50, billingProjectQuoteUsed);
        verify(projectDAO).getAllowedBudget(PROJECT);
        verify(projectService).get(PROJECT);
        verify(billingDAO).getOverallProjectCost(PROJECT);
        verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
    }

    @Test
    public void getBillingProjectMonthlyQuoteUsed() {
        when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
        when(projectService.get(anyString())).thenReturn(getProjectDTO(Boolean.TRUE));
        when(billingDAO.getMonthlyProjectCost(anyString(), any(LocalDate.class))).thenReturn(5d);

        final int billingProjectQuoteUsed = billingService.getBillingProjectQuoteUsed(PROJECT);

        assertEquals("quotes should be equal", 50, billingProjectQuoteUsed);
        verify(projectDAO).getAllowedBudget(PROJECT);
        verify(projectService).get(PROJECT);
        verify(billingDAO).getMonthlyProjectCost(PROJECT, LocalDate.now());
        verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
    }

    private ProjectDTO getProjectDTO(boolean isMonthlyBudget) {
        return ProjectDTO.builder()
                .name(PROJECT)
                .budget(new BudgetDTO(10, isMonthlyBudget))
                .endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT, UserInstanceStatus.RUNNING, null)))
                .build();
    }

    private List<ProjectDTO> getProjectDTOs(boolean isMonthlyBudget) {
        ProjectDTO project1 = ProjectDTO.builder()
                .name(PROJECT)
                .budget(new BudgetDTO(10, isMonthlyBudget))
                .endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT, UserInstanceStatus.RUNNING, null)))
                .build();
        ProjectDTO project2 = ProjectDTO.builder()
                .name(PROJECT_2)
                .budget(new BudgetDTO(20, isMonthlyBudget))
                .endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT, UserInstanceStatus.RUNNING, null)))
                .build();
        return Arrays.asList(project1, project2);
    }

    private ProjectDTO getProjectQuoteNullDTO() {
        return ProjectDTO.builder()
                .name(PROJECT)
                .endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT, UserInstanceStatus.RUNNING, null)))
                .build();
    }

    private List<ProjectDTO> getProjectDTOs() {
        return Collections.singletonList(ProjectDTO.builder()
                .name(PROJECT)
                .endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT, UserInstanceStatus.RUNNING, null)))
                .build());
    }

    private QuotaUsageDTO getQuotaUsageDTO() {
        Map<String, Integer> projectQuotas = new HashMap<>();
        projectQuotas.put(PROJECT, 50);
        projectQuotas.put(PROJECT_2, 50);
        return QuotaUsageDTO.builder()
                .totalQuotaUsed(50)
                .projectQuotas(projectQuotas)
                .build();
    }

    private List<BillingData> getBillingData() {
        BillingData ssnID = BillingData.builder().tag(SSN_ID).usageDate(USAGE_DATE).build();
        BillingData ssnVolumeID = BillingData.builder().tag(SSN_VOLUME_ID).usageDate(USAGE_DATE).build();

        BillingData edgeID = BillingData.builder().tag(EDGE_ID_1).usageDate(USAGE_DATE).build();
        BillingData edgeVolumeId = BillingData.builder().tag(EDGE_VOLUME_ID_1).usageDate(USAGE_DATE).build();
        BillingData endpointBucketId = BillingData.builder().tag(ENDPOINT_BUCKET_ID).usageDate(USAGE_DATE).build();

        BillingData projectEndpointBucketId = BillingData.builder().tag(PROJECT_ENDPOINT_BUCKET_ID_1).usageDate(USAGE_DATE).build();
        BillingData endpointId = BillingData.builder().tag(ENDPOINT_ID_1).usageDate(USAGE_DATE).build();

        BillingData exploratoryId = BillingData.builder().tag(EXPLORATORY_ID).usageDate(USAGE_DATE).build();
        BillingData exploratoryPrimaryVolumeId = BillingData.builder().tag(EXPLORATORY_VOLUME_PRIMARY_ID_1).usageDate(USAGE_DATE).build();
        BillingData exploratorySecondaryVolumeId = BillingData.builder().tag(EXPLORATORY_SECONDARY_ID_1).usageDate(USAGE_DATE).build();

        BillingData computeId = BillingData.builder().tag(COMPUTE_ID).usageDate(USAGE_DATE).build();
        BillingData computePrimaryVolumeId = BillingData.builder().tag(COMPUTE_VOLUME_PRIMARY_ID).usageDate(USAGE_DATE).build();
        BillingData computeSecondaryVolumeId = BillingData.builder().tag(COMPUTE_VOLUME_SECONDARY_ID).usageDate(USAGE_DATE).build();
        BillingData computeMasterPrimaryVolumeId = BillingData.builder().tag(COMPUTE_MASTER_VOLUME_PRIMARY_ID).usageDate(USAGE_DATE).build();
        BillingData computeMasterSecondaryVolumeId = BillingData.builder().tag(COMPUTE_MASTER_VOLUME_SECONDARY_ID).usageDate(USAGE_DATE).build();
        BillingData computeSlave1PrimaryVolumeId = BillingData.builder().tag(COMPUTE_SLAVE_1_VOLUME_PRIMARY_ID).usageDate(USAGE_DATE).build();
        BillingData computeSlave1SecondaryVolumeId = BillingData.builder().tag(COMPUTE_SLAVE_1_VOLUME_SECONDARY_ID).usageDate(USAGE_DATE).build();
        BillingData computeSlave2PrimaryVolumeId = BillingData.builder().tag(COMPUTE_SLAVE_2_VOLUME_PRIMARY_ID).usageDate(USAGE_DATE).build();
        BillingData computeSlave2SecondaryVolumeId = BillingData.builder().tag(COMPUTE_SLAVE_2_VOLUME_SECONDARY_ID).usageDate(USAGE_DATE).build();

        BillingData imageId = BillingData.builder().tag(IMAGE_ID).usageDate(USAGE_DATE).build();


        return Arrays.asList(ssnID, ssnVolumeID, edgeID, edgeVolumeId, endpointBucketId, projectEndpointBucketId, endpointId, exploratoryId, exploratoryPrimaryVolumeId,
                exploratorySecondaryVolumeId, computeId, computePrimaryVolumeId, computeSecondaryVolumeId, computeMasterPrimaryVolumeId, computeMasterSecondaryVolumeId,
                computeSlave1PrimaryVolumeId, computeSlave1SecondaryVolumeId, computeSlave2PrimaryVolumeId, computeSlave2SecondaryVolumeId, imageId);
    }

    private List<BillingReportLine> getBillingReportLine() {
        BillingReportLine ssn = BillingReportLine.builder().datalabId(SSN_ID).usageDate(USAGE_DATE).application(ENDPOINT).resourceName("SSN")
                .user(SHARED_RESOURCE).project(SHARED_RESOURCE).resourceType(BillingResourceType.SSN).build();
        BillingReportLine ssnVolume = BillingReportLine.builder().datalabId(SSN_VOLUME_ID).usageDate(USAGE_DATE).application(ENDPOINT).resourceName("SSN Volume")
                .user(SHARED_RESOURCE).project(SHARED_RESOURCE).resourceType(BillingResourceType.VOLUME).build();

        BillingReportLine edge = BillingReportLine.builder().datalabId(EDGE_ID_1).usageDate(USAGE_DATE).application(ENDPOINT).resourceName(ENDPOINT)
                .user(SHARED_RESOURCE).project(PROJECT).resourceType(BillingResourceType.EDGE).build();
        BillingReportLine edgeVolumeId = BillingReportLine.builder().datalabId(EDGE_VOLUME_ID_1).usageDate(USAGE_DATE).application(ENDPOINT).resourceName("EDGE volume")
                .user(SHARED_RESOURCE).project(PROJECT).resourceType(BillingResourceType.VOLUME).build();
        BillingReportLine endpointBucketId = BillingReportLine.builder().datalabId(ENDPOINT_BUCKET_ID).usageDate(USAGE_DATE).application(ENDPOINT).resourceName("Project endpoint shared bucket")
                .user(SHARED_RESOURCE).project(PROJECT).resourceType(BillingResourceType.BUCKET).build();

        BillingReportLine projectEndpointBucket = BillingReportLine.builder().datalabId(PROJECT_ENDPOINT_BUCKET_ID_1).usageDate(USAGE_DATE).application(ENDPOINT).resourceName("Endpoint shared bucket")
                .user(SHARED_RESOURCE).project(SHARED_RESOURCE).resourceType(BillingResourceType.BUCKET).build();
        BillingReportLine endpoint = BillingReportLine.builder().datalabId(ENDPOINT_ID_1).usageDate(USAGE_DATE).application(ENDPOINT).resourceName("Endpoint")
                .user(SHARED_RESOURCE).project(SHARED_RESOURCE).resourceType(BillingResourceType.ENDPOINT).build();

        BillingReportLine exploratory = BillingReportLine.builder().datalabId(EXPLORATORY_ID).usageDate(USAGE_DATE).application(ENDPOINT)
                .user(USER).project(PROJECT).endpoint(ENDPOINT).resourceName(EXPLORATORY_NAME).resourceType(BillingResourceType.EXPLORATORY).build();
        BillingReportLine exploratoryPrimaryVolume = BillingReportLine.builder().datalabId(EXPLORATORY_VOLUME_PRIMARY_ID_1).usageDate(USAGE_DATE).application(ENDPOINT)
                .user(USER).project(PROJECT).endpoint(ENDPOINT).resourceName(EXPLORATORY_NAME).resourceType(BillingResourceType.VOLUME).build();
        BillingReportLine exploratorySecondaryVolume = BillingReportLine.builder().datalabId(EXPLORATORY_SECONDARY_ID_1).usageDate(USAGE_DATE).application(ENDPOINT)
                .user(USER).project(PROJECT).endpoint(ENDPOINT).resourceName(EXPLORATORY_NAME).resourceType(BillingResourceType.VOLUME).build();

        BillingReportLine compute = BillingReportLine.builder().datalabId(COMPUTE_ID).usageDate(USAGE_DATE).application(ENDPOINT).user(USER).project(PROJECT)
                .endpoint(ENDPOINT).resourceName(COMPUTE_NAME).resourceType(BillingResourceType.COMPUTATIONAL).shape("Master: " + SHAPE + "Slave: 2 x " + SHAPE).exploratoryName(EXPLORATORY_NAME).build();
        BillingReportLine computePrimaryVolume = BillingReportLine.builder().datalabId(COMPUTE_VOLUME_PRIMARY_ID).usageDate(USAGE_DATE).application(ENDPOINT).user(USER)
                .project(PROJECT).endpoint(ENDPOINT).resourceName(COMPUTE_NAME).resourceType(BillingResourceType.VOLUME).build();
        BillingReportLine computeSecondaryVolume = BillingReportLine.builder().datalabId(COMPUTE_VOLUME_SECONDARY_ID).usageDate(USAGE_DATE).application(ENDPOINT).user(USER)
                .project(PROJECT).endpoint(ENDPOINT).resourceName(COMPUTE_NAME).resourceType(BillingResourceType.VOLUME).build();
        BillingReportLine computeMasterPrimaryVolume = BillingReportLine.builder().datalabId(COMPUTE_MASTER_VOLUME_PRIMARY_ID).usageDate(USAGE_DATE).application(ENDPOINT)
                .user(USER).project(PROJECT).endpoint(ENDPOINT).resourceName(COMPUTE_NAME).resourceType(BillingResourceType.VOLUME).build();
        BillingReportLine computeMasterSecondaryVolume = BillingReportLine.builder().datalabId(COMPUTE_MASTER_VOLUME_SECONDARY_ID).usageDate(USAGE_DATE).application(ENDPOINT)
                .user(USER).project(PROJECT).endpoint(ENDPOINT).resourceName(COMPUTE_NAME).resourceType(BillingResourceType.VOLUME).build();
        BillingReportLine computeSlave1PrimaryVolume = BillingReportLine.builder().datalabId(COMPUTE_SLAVE_1_VOLUME_PRIMARY_ID).usageDate(USAGE_DATE).application(ENDPOINT)
                .user(USER).project(PROJECT).endpoint(ENDPOINT).resourceName(COMPUTE_NAME).resourceType(BillingResourceType.VOLUME).build();
        BillingReportLine computeSlave1SecondaryVolume = BillingReportLine.builder().datalabId(COMPUTE_SLAVE_1_VOLUME_SECONDARY_ID).usageDate(USAGE_DATE).application(ENDPOINT)
                .user(USER).project(PROJECT).endpoint(ENDPOINT).resourceName(COMPUTE_NAME).resourceType(BillingResourceType.VOLUME).build();
        BillingReportLine computeSlave2PrimaryVolume = BillingReportLine.builder().datalabId(COMPUTE_SLAVE_2_VOLUME_PRIMARY_ID).usageDate(USAGE_DATE).application(ENDPOINT)
                .user(USER).project(PROJECT).endpoint(ENDPOINT).resourceName(COMPUTE_NAME).resourceType(BillingResourceType.VOLUME).build();
        BillingReportLine computeSlave2SecondaryVolume = BillingReportLine.builder().datalabId(COMPUTE_SLAVE_2_VOLUME_SECONDARY_ID).usageDate(USAGE_DATE).application(ENDPOINT)
                .user(USER).project(PROJECT).endpoint(ENDPOINT).resourceName(COMPUTE_NAME).resourceType(BillingResourceType.VOLUME).build();

        BillingReportLine image = BillingReportLine.builder().datalabId(IMAGE_ID).usageDate(USAGE_DATE).application(ENDPOINT)
                .user(USER).project(PROJECT).resourceName(IMAGE_NAME).resourceType(BillingResourceType.IMAGE).build();

        return Arrays.asList(ssn, ssnVolume, edge, edgeVolumeId, endpointBucketId, projectEndpointBucket, endpoint, exploratory, exploratoryPrimaryVolume,
                exploratorySecondaryVolume, compute, computePrimaryVolume, computeSecondaryVolume, computeMasterPrimaryVolume, computeMasterSecondaryVolume,
                computeSlave1PrimaryVolume, computeSlave1SecondaryVolume, computeSlave2PrimaryVolume, computeSlave2SecondaryVolume, image);
    }

    private List<BillingReportLine> getBillingReportLineWithCost() {
        BillingReportLine line1 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-01-01")).usageDateTo(LocalDate.parse("2020-03-01")).cost(1.999)
                .currency(CURRENCY).datalabId(EDGE_ID_1).user(USER).product(PRODUCT).shape(SHAPE).resourceType(BillingResourceType.EDGE).project(PROJECT)
                .resourceName(ENDPOINT).build();
        BillingReportLine line2 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-02-01")).usageDateTo(LocalDate.parse("2020-05-01")).cost(1.0)
                .currency(CURRENCY).datalabId(EXPLORATORY_ID).product(PRODUCT).shape(SHAPE).user(USER).resourceType(BillingResourceType.EXPLORATORY).project(PROJECT)
                .resourceName(EXPLORATORY_NAME).build();
        BillingReportLine line3 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-03-01")).usageDateTo(LocalDate.parse("2020-04-01")).cost(1.0)
                .currency(CURRENCY).datalabId(COMPUTE_ID).product(PRODUCT).shape(SHAPE).user(USER).resourceType(BillingResourceType.COMPUTATIONAL).project(PROJECT)
                .resourceName(COMPUTE_NAME)
                .exploratoryName(EXPLORATORY_NAME).build();

        return Arrays.asList(line1, line2, line3);
    }

    private List<BillingReportLine> getBillingReportLineWithDifferentCurrency() {
        BillingReportLine line1 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-01-01")).usageDateTo(LocalDate.parse("2020-03-01")).cost(1.999)
                .currency(CURRENCY).build();
        BillingReportLine line2 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-02-01")).usageDateTo(LocalDate.parse("2020-05-01")).cost(1.0)
                .currency("currency2").build();
        BillingReportLine line3 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-03-01")).usageDateTo(LocalDate.parse("2020-04-01")).cost(1.0)
                .currency(CURRENCY).build();

        return Arrays.asList(line1, line2, line3);
    }

    private BillingReport getReport() {
        BillingReportLine line1 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-01-01")).usageDateTo(LocalDate.parse("2020-03-01")).cost(2.0)
                .currency(CURRENCY).datalabId(EDGE_ID_1).user(USER).product(PRODUCT).shape(SHAPE).resourceType(BillingResourceType.EDGE).project(PROJECT)
                .resourceName(ENDPOINT).build();
        BillingReportLine line2 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-02-01")).usageDateTo(LocalDate.parse("2020-05-01")).cost(1.0)
                .currency(CURRENCY).datalabId(EXPLORATORY_ID).product(PRODUCT).shape(SHAPE).user(USER).resourceType(BillingResourceType.EXPLORATORY).project(PROJECT)
                .resourceName(EXPLORATORY_NAME).build();
        BillingReportLine line3 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-03-01")).usageDateTo(LocalDate.parse("2020-04-01")).cost(1.0)
                .currency(CURRENCY).datalabId(COMPUTE_ID).product(PRODUCT).shape(SHAPE).user(USER).resourceType(BillingResourceType.COMPUTATIONAL).project(PROJECT)
                .resourceName(COMPUTE_NAME).exploratoryName(EXPLORATORY_NAME).build();
        List<BillingReportLine> billingReportLines = Arrays.asList(line1, line2, line3);

        return BillingReport.builder()
                .name(EXPLORATORY_NAME)
                .reportLines(billingReportLines)
                .totalCost(4.0)
                .currency(CURRENCY)
                .build();
    }

    private BillingReport getReportWithNullCurrency() {
        BillingReportLine line1 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-01-01")).usageDateTo(LocalDate.parse("2020-03-01")).cost(2.0)
                .currency(CURRENCY).build();
        BillingReportLine line2 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-02-01")).usageDateTo(LocalDate.parse("2020-05-01")).cost(1.0)
                .currency("currency2").build();
        BillingReportLine line3 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-03-01")).usageDateTo(LocalDate.parse("2020-04-01")).cost(1.0)
                .currency(CURRENCY).build();
        List<BillingReportLine> billingReportLines = Arrays.asList(line1, line2, line3);

        return BillingReport.builder()
                .name(EXPLORATORY_NAME)
                .reportLines(billingReportLines)
                .totalCost(4.0)
                .currency(null)
                .build();
    }

    private BillingReport getExpectedBillingReport() {
        BillingReportLine line1 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-01-01")).usageDateTo(LocalDate.parse("2020-03-01")).cost(1.999)
                .currency(CURRENCY).datalabId(EDGE_ID_1).user(USER).product(PRODUCT).shape(SHAPE).resourceType(BillingResourceType.EDGE).project(PROJECT)
                .resourceName(ENDPOINT).status(UserInstanceStatus.RUNNING).build();
        BillingReportLine line2 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-02-01")).usageDateTo(LocalDate.parse("2020-05-01")).cost(1.0)
                .currency(CURRENCY).datalabId(EXPLORATORY_ID).product(PRODUCT).shape(SHAPE).user(USER).resourceType(BillingResourceType.EXPLORATORY).project(PROJECT)
                .resourceName(EXPLORATORY_NAME).status(UserInstanceStatus.FAILED).build();
        BillingReportLine line3 = BillingReportLine.builder().usageDateFrom(LocalDate.parse("2020-03-01")).usageDateTo(LocalDate.parse("2020-04-01")).cost(1.0)
                .currency(CURRENCY).datalabId(COMPUTE_ID).product(PRODUCT).shape("Master: " + 1 + " x " + SHAPE + " Slave: " + 2 + " x " + SHAPE).user(USER).resourceType(BillingResourceType.COMPUTATIONAL).project(PROJECT)
                .resourceName(COMPUTE_NAME).exploratoryName(EXPLORATORY_NAME).status(UserInstanceStatus.CREATING).build();
        List<BillingReportLine> billingReportLines = Arrays.asList(line1, line2, line3);

        return BillingReport.builder()
                .name("Billing report")
                .sbn(SERVICE_BASE_NAME)
                .reportLines(billingReportLines)
                .usageDateFrom(LocalDate.parse("2020-01-01"))
                .usageDateTo(LocalDate.parse("2020-05-01"))
                .totalCost(4.0)
                .currency(CURRENCY)
                .isReportHeaderCompletable(Boolean.TRUE)
                .build();
    }

    private BillingReport getExpectedBillingReportWithNullCurrency() {
        return BillingReport.builder()
                .name("Billing report")
                .sbn(SERVICE_BASE_NAME)
                .reportLines(getBillingReportLineWithDifferentCurrency())
                .usageDateFrom(LocalDate.parse("2020-01-01"))
                .usageDateTo(LocalDate.parse("2020-05-01"))
                .totalCost(4.0)
                .currency(null)
                .isReportHeaderCompletable(Boolean.TRUE)
                .build();
    }

    private String getDownloadReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\"Service base name: ").append(SERVICE_BASE_NAME).append(". Available reporting period from: ").append("Jan 1, 2020")
                .append(" to: ").append("May 1, 2020").append("\"").append(System.lineSeparator());

        sb.append(new StringJoiner(",").add("DataLab ID").add("User").add("Project").add("DataLab Resource Type").add("Status").add("Shape").add("Product")
                .add("Cost" + System.lineSeparator()));

        sb.append(new StringJoiner(",").add(EDGE_ID_1).add(USER).add(PROJECT).add("Edge").add("running").add(SHAPE).add(PRODUCT).add(1.999 + System.lineSeparator()));
        sb.append(new StringJoiner(",").add(EXPLORATORY_ID).add(USER).add(PROJECT).add("Exploratory").add("failed").add(SHAPE).add(PRODUCT).add(1.0 + System.lineSeparator()));
        sb.append(new StringJoiner(",").add(COMPUTE_ID).add(USER).add(PROJECT).add("Computational").add("creating").add("Master: " + 1 + " x " + SHAPE + " Slave: " + 2 + " x " + SHAPE).add(PRODUCT).add(1.0 + System.lineSeparator()));

        sb.append(",,,,,,,Total: 4.0 currency");
        sb.append(System.lineSeparator());

        return sb.toString();
    }

    private List<EndpointDTO> getGCPEndpointDTO() {
        return Collections.singletonList(new EndpointDTO(ENDPOINT, ENDPOINT_URL, ENDPOINT_ACCOUNT, ENDPOINT_TAG, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.GCP));
    }

    private List<EndpointDTO> getAWSEndpointDTO() {
        return Collections.singletonList(new EndpointDTO(ENDPOINT, ENDPOINT_URL, ENDPOINT_ACCOUNT, ENDPOINT_TAG, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.AWS));
    }

    private List<EndpointDTO> getAzureEndpointDTO() {
        return Collections.singletonList(new EndpointDTO(ENDPOINT, ENDPOINT_URL, ENDPOINT_ACCOUNT, ENDPOINT_TAG, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.AZURE));
    }

    private List<EndpointDTO> getEndpointDTOWithWrongUrl() {
        return Collections.singletonList(new EndpointDTO(ENDPOINT, "wrong url", ENDPOINT_ACCOUNT, ENDPOINT_TAG, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.AZURE));
    }

    private List<UserInstanceDTO> getUserInstanceDTOs() {
        return Collections.singletonList(
                new UserInstanceDTO().withExploratoryId(EXPLORATORY_ID).withUser(USER).withProject(PROJECT).withExploratoryName(EXPLORATORY_NAME).withEndpoint(ENDPOINT)
                        .withResources(Collections.singletonList(getCompute())));
    }

    private UserInstanceDTO getUserInstanceDTO() {
        return new UserInstanceDTO().withExploratoryId(EXPLORATORY_ID).withUser(USER).withProject(PROJECT).withExploratoryName(EXPLORATORY_NAME).withEndpoint(ENDPOINT)
                .withStatus("failed");
    }

    private UserInstanceDTO getUserInstanceDTOWithCompute() {
        return new UserInstanceDTO().withExploratoryId(EXPLORATORY_ID).withUser(USER).withProject(PROJECT).withExploratoryName(EXPLORATORY_NAME).withEndpoint(ENDPOINT)
                .withStatus("failed").withResources(Collections.singletonList(getCompute()));
    }

    private UserComputationalResource getCompute() {
        UserComputationalResource resource = new UserComputationalResource();
        resource.setComputationalId(COMPUTE_ID);
        resource.setComputationalName(COMPUTE_NAME);
        resource.setImageName(DataEngineType.SPARK_STANDALONE.getName());
        resource.setTotalInstanceCount(3);
        resource.setMasterNodeShape(SHAPE);
        resource.setSlaveNodeShape(SHAPE);
        resource.setStatus("creating");
        resource.setTemplateName("template_name");

        return resource;
    }

    private List<ImageInfoRecord> getImageInfoRecords() {
        return Collections.singletonList(
                new ImageInfoRecord(
                        IMAGE_NAME,
                        new Date(),
                        IMAGE_DESCRIPTION,
                        PROJECT,
                        ENDPOINT,
                        USER,
                        IMAGE_APPLICATION,
                        TEMPLATE_NAME,
                        EXPLORATORY_NAME,
                        CloudProvider.GENERAL,
                        "dockerImage",
                        IMAGE_FULL_NAME,
                        ImageStatus.ACTIVE,
                        null,
                        null,
                        null,
                        null,
                        null)
        );
    }
}