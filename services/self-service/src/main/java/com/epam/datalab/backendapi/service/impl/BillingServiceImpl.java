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
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.dao.ImageExploratoryDAO;
import com.epam.datalab.backendapi.dao.ProjectDAO;
import com.epam.datalab.backendapi.domain.*;
import com.epam.datalab.backendapi.resources.dto.BillingFilter;
import com.epam.datalab.backendapi.resources.dto.ExportBillingFilter;
import com.epam.datalab.backendapi.resources.dto.QuotaUsageDTO;
import com.epam.datalab.backendapi.roles.RoleType;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.BillingService;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.ExploratoryService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.util.BillingUtils;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.billing.BillingData;
import com.epam.datalab.dto.billing.BillingResourceType;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.client.utils.URIBuilder;

import javax.ws.rs.core.GenericType;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BillingServiceImpl implements BillingService {
    private static final String BILLING_PATH = "/api/billing";
    private static final String USAGE_DATE_FORMAT = "yyyy-MM";

    private final ProjectService projectService;
    private final ProjectDAO projectDAO;
    private final EndpointService endpointService;
    private final ExploratoryService exploratoryService;
    private final SelfServiceApplicationConfiguration configuration;
    private final RESTService billingService;
    private final ImageExploratoryDAO imageExploratoryDao;
    private final BillingDAO billingDAO;
    private final ExploratoryDAO exploratoryDAO;

    @Inject
    public BillingServiceImpl(ProjectService projectService, ProjectDAO projectDAO, EndpointService endpointService,
                              ExploratoryService exploratoryService, SelfServiceApplicationConfiguration configuration,
                              @Named(ServiceConsts.BILLING_SERVICE_NAME) RESTService billingService,
                              ImageExploratoryDAO imageExploratoryDao, BillingDAO billingDAO, ExploratoryDAO exploratoryDAO) {
        this.projectService = projectService;
        this.projectDAO = projectDAO;
        this.endpointService = endpointService;
        this.exploratoryService = exploratoryService;
        this.configuration = configuration;
        this.billingService = billingService;
        this.imageExploratoryDao = imageExploratoryDao;
        this.billingDAO = billingDAO;
        this.exploratoryDAO = exploratoryDAO;
    }

    @Override
    public BillingReport getBillingReport(UserInfo user, BillingFilter filter) {
        setUserFilter(user, filter);
        List<BillingReportLine> billingReportLines = billingDAO.aggregateBillingData(filter)
                .stream()
                .peek(this::appendStatuses)
                .peek(this::appendShapes)
                .filter(bd -> CollectionUtils.isEmpty(filter.getStatuses()) || filter.getStatuses().contains(bd.getStatus()))
                .peek(bd -> { if (bd.getShape() != null && bd.getShape().contains("null")) bd.setShape(null);})
                .collect(Collectors.toList());
        
        final LocalDate min = billingReportLines.stream().min(Comparator.comparing(BillingReportLine::getUsageDateFrom)).map(BillingReportLine::getUsageDateFrom).orElse(null);
        final LocalDate max = billingReportLines.stream().max(Comparator.comparing(BillingReportLine::getUsageDateTo)).map(BillingReportLine::getUsageDateTo).orElse(null);
        final double sum = billingReportLines.stream().mapToDouble(BillingReportLine::getCost).sum();
        final String currency = billingReportLines.stream().map(BillingReportLine::getCurrency).distinct().count() == 1 ? billingReportLines.get(0).getCurrency() : null;
        return BillingReport.builder()
                .name("Billing report")
                .sbn(configuration.getServiceBaseName())
                .reportLines(billingReportLines)
                .usageDateFrom(min)
                .usageDateTo(max)
                .totalCost(BigDecimal.valueOf(sum).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())
                .currency(currency)
                .isReportHeaderCompletable(hasUserBillingRole(user))
                .build();
    }

    @Override
    public String downloadReport(UserInfo user, ExportBillingFilter filter, String locale) {
        BillingReport report = getBillingReport(user, filter);
        boolean isReportComplete = report.isReportHeaderCompletable();
        StringBuilder reportHead = new StringBuilder(BillingUtils.getFirstLine(report.getSbn(), report.getUsageDateFrom(), report.getUsageDateTo(), locale));
        String stringOfAdjustedHeader = BillingUtils.getHeader(isReportComplete);
        reportHead.append(stringOfAdjustedHeader);
        report.getReportLines().forEach(r -> reportHead.append(BillingUtils.printLine(r, isReportComplete)));
        reportHead.append(BillingUtils.getTotal(report.getTotalCost(), report.getCurrency(), stringOfAdjustedHeader));
        return reportHead.toString();
    }

    @Override
    public BillingReport getExploratoryBillingData(String project, String endpoint, String exploratoryName, List<String> compNames) {
        List<String> resourceNames = new ArrayList<>(compNames);
        resourceNames.add(exploratoryName);
        List<BillingReportLine> billingReportLines = billingDAO.findBillingData(project, endpoint, resourceNames);
        final double sum = billingReportLines.stream().mapToDouble(BillingReportLine::getCost).sum();
        List<BillingReportLine> billingData = billingReportLines
                .stream()
                .peek(bd -> bd.setCost(BigDecimal.valueOf(bd.getCost()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()))
                .collect(Collectors.toList());
        ;
        final String currency = billingData.stream().map(BillingReportLine::getCurrency).distinct().count() == 1 ? billingData.get(0).getCurrency() : null;
        return BillingReport.builder()
                .name(exploratoryName)
                .reportLines(billingData)
                .totalCost(BigDecimal.valueOf(sum).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())
                .currency(currency)
                .build();
    }

    @Override
    public void updateRemoteBillingData(UserInfo userInfo) {
        List<EndpointDTO> endpoints = endpointService.getEndpoints();
        if (CollectionUtils.isEmpty(endpoints)) {
            log.error("Cannot update billing info. There are no endpoints");
            throw new DatalabException("Cannot update billing info. There are no endpoints");
        }

        Map<EndpointDTO, List<BillingData>> billingDataMap = endpoints
                .stream()
                .collect(Collectors.toMap(e -> e, e -> getBillingData(userInfo, e)));

        billingDataMap.forEach((endpointDTO, billingData) -> {
            log.info("Updating billing information for endpoint {}. Billing data {}", endpointDTO.getName(), billingData);
            if (!billingData.isEmpty()) {
                updateBillingData(endpointDTO, billingData, endpoints);
                log.info("Updating billing information for endpoint {}. Billing data {} success", endpointDTO.getName(), billingData);
            }
        });
    }

    @Override
    public QuotaUsageDTO getQuotas(UserInfo userInfo) {
        int totalQuota = billingDAO.getBillingQuoteUsed();
        Map<String, Integer> projectQuotas = projectService.getProjects(userInfo)
                .stream()
                .collect(Collectors.toMap(ProjectDTO::getName, p -> getBillingProjectQuoteUsed(p.getName())));
        return QuotaUsageDTO.builder()
                .totalQuotaUsed(totalQuota)
                .projectQuotas(projectQuotas)
                .build();
    }

    @Override
    public boolean isProjectQuoteReached(String project) {
        final Double projectCost = getProjectCost(project);
        return projectDAO.getAllowedBudget(project)
                .filter(allowedBudget -> projectCost.intValue() != 0 && allowedBudget <= projectCost)
                .isPresent();
    }

    @Override
    public int getBillingProjectQuoteUsed(String project) {
        return toPercentage(() -> projectDAO.getAllowedBudget(project), getProjectCost(project));
    }

    private Double getProjectCost(String project) {
        final boolean monthlyBudget = Optional.ofNullable(projectService.get(project).getBudget())
                .map(BudgetDTO::isMonthlyBudget)
                .orElse(Boolean.FALSE);
        return monthlyBudget ? billingDAO.getMonthlyProjectCost(project, LocalDate.now()) : billingDAO.getOverallProjectCost(project);
    }

    private Map<String, BillingReportLine> getBillableResources(List<EndpointDTO> endpoints) {
        Set<ProjectDTO> projects = new HashSet<>(projectService.getProjects());
        final Stream<BillingReportLine> ssnBillingDataStream = BillingUtils.ssnBillingDataStream(configuration.getServiceBaseName());
        final Stream<BillingReportLine> billableEdges = projects
                .stream()
                .collect(Collectors.toMap(ProjectDTO::getName, ProjectDTO::getEndpoints))
                .entrySet()
                .stream()
                .flatMap(e -> projectEdges(configuration.getServiceBaseName(), e.getKey(), e.getValue()));
        final Stream<BillingReportLine> billableSharedEndpoints = endpoints
                .stream()
                .flatMap(endpoint -> BillingUtils.sharedEndpointBillingDataStream(endpoint.getName(), configuration.getServiceBaseName()));

        final Stream<BillingReportLine> billableUserInstances = exploratoryService.findAll(projects)
                .stream()
                .filter(userInstance -> Objects.nonNull(userInstance.getExploratoryId()))
                .flatMap(ui -> BillingUtils.exploratoryBillingDataStream(ui, configuration.getMaxSparkInstanceCount()));

        final Stream<BillingReportLine> customImages = projects
                .stream()
                .map(p -> imageExploratoryDao.getImagesForProject(p.getName()))
                .flatMap(Collection::stream)
                .flatMap(i -> BillingUtils.customImageBillingDataStream(i, configuration.getServiceBaseName()));

        final Map<String, BillingReportLine> billableResources = Stream.of(ssnBillingDataStream, billableEdges, billableSharedEndpoints, billableUserInstances, customImages)
                .flatMap(s -> s)
                .collect(Collectors.toMap(BillingReportLine::getDatalabId, b -> b));
        log.info("Billable resources are: {}", billableResources);

        return billableResources;
    }

    private Stream<BillingReportLine> projectEdges(String serviceBaseName, String projectName, List<ProjectEndpointDTO> endpoints) {
        return endpoints
                .stream()
                .flatMap(endpoint -> BillingUtils.edgeBillingDataStream(projectName, serviceBaseName, endpoint.getName()));
    }

    private void updateBillingData(EndpointDTO endpointDTO, List<BillingData> billingData, List<EndpointDTO> endpoints) {
        final String endpointName = endpointDTO.getName();
        final CloudProvider cloudProvider = endpointDTO.getCloudProvider();
        final Map<String, BillingReportLine> billableResources = getBillableResources(endpoints);
        final Stream<BillingReportLine> billingReportLineStream = billingData
                .stream()
                .peek(bd -> bd.setApplication(endpointName))
                .map(bd -> toBillingReport(bd, getOrDefault(billableResources, bd.getTag())));
        if (cloudProvider == CloudProvider.GCP) {
            final Map<String, List<BillingReportLine>> gcpBillingData = billingReportLineStream
//                    .collect(Collectors.groupingBy(bd -> bd.getUsageDate().substring(0, USAGE_DATE_FORMAT.length())));
                    .collect(Collectors.groupingBy(BillingReportLine::getUsageDate));
            updateGcpBillingData(endpointName, gcpBillingData);
        } else if (cloudProvider == CloudProvider.AWS) {
            final Map<String, List<BillingReportLine>> awsBillingData = billingReportLineStream
                    .collect(Collectors.groupingBy(BillingReportLine::getUsageDate));
            updateAwsBillingData(endpointName, awsBillingData);
        } else if (cloudProvider == CloudProvider.AZURE) {
            final List<BillingReportLine> billingReportLines = billingReportLineStream
                    .collect(Collectors.toList());
            updateAzureBillingData(billingReportLines);
        }
    }

    private BillingReportLine getOrDefault(Map<String, BillingReportLine> billableResources, String tag) {
        return billableResources.getOrDefault(tag, BillingReportLine.builder().datalabId(tag).build());
    }

    private void updateGcpBillingData(String endpointName, Map<String, List<BillingReportLine>> billingData) {
        log.info("!!!TEST OUT!!! BillingReportLine: {}", billingData);
        log.info("!!!TEST OUT!!! endpointName: {}", endpointName);


        billingData.forEach((usageDate, billingReportLines) -> {
            billingDAO.deleteByUsageDateRegex(endpointName, usageDate);
            billingDAO.save(billingReportLines);
        });
    }

    private void updateAwsBillingData(String endpointName, Map<String, List<BillingReportLine>> billingData) {
        billingData.forEach((usageDate, billingReportLines) -> {
            billingDAO.deleteByUsageDate(endpointName, usageDate);
            billingDAO.save(billingReportLines);
        });
    }

    private void updateAzureBillingData(List<BillingReportLine> billingReportLines) {
        billingDAO.save(billingReportLines);
    }

    private List<BillingData> getBillingData(UserInfo userInfo, EndpointDTO endpointDTO) {
        try {
            return billingService.get(getBillingUrl(endpointDTO.getUrl(), BILLING_PATH), userInfo.getAccessToken(),
                    new GenericType<List<BillingData>>() {
                    });
        } catch (Exception e) {
            log.error("Cannot retrieve billing information for {} . Reason {}.", endpointDTO.getName(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String getBillingUrl(String endpointUrl, String path) {
        URI uri;
        try {
            uri = new URI(endpointUrl);
        } catch (URISyntaxException e) {
            log.error("Wrong URI syntax {}", e.getMessage(), e);
            throw new DatalabException("Wrong URI syntax");
        }
        return new URIBuilder()
                .setScheme(uri.getScheme())
                .setHost(uri.getHost())
                .setPort(configuration.getBillingPort())
                .setPath(path)
                .toString();
    }

    private void appendStatuses(BillingReportLine br) {
        BillingResourceType resourceType = br.getResourceType();
        if (BillingResourceType.EDGE == resourceType) {
            projectService.get(br.getProject()).getEndpoints()
                    .stream()
                    .filter(e -> e.getName().equals(br.getResourceName()))
                    .findAny()
                    .ifPresent(e -> br.setStatus(e.getStatus()));
        } else if (BillingResourceType.EXPLORATORY == resourceType) {
            exploratoryService.getUserInstance(br.getUser(), br.getProject(), br.getResourceName())
                    .ifPresent(ui -> br.setStatus(UserInstanceStatus.of(ui.getStatus())));
        } else if (BillingResourceType.COMPUTATIONAL == resourceType) {
            exploratoryService.getUserInstance(br.getUser(), br.getProject(), br.getExploratoryName(), true)
                    .flatMap(ui -> ui.getResources()
                            .stream()
                            .filter(cr -> cr.getComputationalName().equals(br.getResourceName()))
                            .findAny())
                    .ifPresent(cr -> br.setStatus(UserInstanceStatus.of(cr.getStatus())));
        }
    }

    /**
     * Appends shapes for computational resources
     * @param br Billing report info for certain resource
     */
    private void appendShapes(BillingReportLine br) {
        BillingResourceType resourceType = br.getResourceType();
        if (BillingResourceType.COMPUTATIONAL == resourceType) {
            String shape = "Master: 1 x %s Slave: %s x %s";
            int numberOfMasterNodes = 1;
            exploratoryService.getUserInstance(br.getUser(), br.getProject(), br.getExploratoryName(), true)
                    .flatMap(ui -> ui.getResources()
                            .stream()
                            .filter(cr -> cr.getComputationalName().equals(br.getResourceName()))
                            .findAny())
                    .ifPresent(cr -> br.setShape(
                            String.format(shape, cr.getMasterNodeShape(), cr.getTotalInstanceCount() - numberOfMasterNodes, cr.getSlaveNodeShape())
                    ));
        }
    }

    /**
     * @param userInfo user's properties for current session
     * @return true, if user has be billing role
     */
    private boolean hasUserBillingRole(UserInfo userInfo) {
        return UserRoles.checkAccess(userInfo, RoleType.PAGE, "/api/infrastructure_provision/billing", userInfo.getRoles());
    }

    private void setUserFilter(UserInfo userInfo, BillingFilter filter) {
        if (!hasUserBillingRole(userInfo)) {
            filter.setUsers(Lists.newArrayList(userInfo.getName()));
        }
    }

    private BillingReportLine toBillingReport(BillingData billingData, BillingReportLine billingReportLine) {
        return BillingReportLine.builder()
                .application(billingData.getApplication())
                .cost(billingData.getCost())
                .currency(billingData.getCurrency())
                .product(billingData.getProduct())
                .project(billingReportLine.getProject())
                .endpoint(billingReportLine.getEndpoint())
                .usageDateFrom(billingData.getUsageDateFrom())
                .usageDateTo(billingData.getUsageDateTo())
                .usageDate(billingData.getUsageDate())
                .usageType(billingData.getUsageType())
                .user(billingReportLine.getUser())
                .datalabId(billingData.getTag())
                .resourceType(billingReportLine.getResourceType())
                .resourceName(billingReportLine.getResourceName())
                .shape(billingReportLine.getShape())
                .exploratoryName(billingReportLine.getExploratoryName())
                .build();
    }

    private Integer toPercentage(Supplier<Optional<Integer>> allowedBudget, Double totalCost) {
        return allowedBudget.get()
                .map(userBudget -> (totalCost * 100) / userBudget)
                .map(Double::intValue)
                .orElse(BigDecimal.ZERO.intValue());
    }

    @Data
    @AllArgsConstructor
    private class ProjectResources {
        private String resName;
        private String project;
        private BillingReportLine billingReportLine;
    }
}
