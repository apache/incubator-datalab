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
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.dao.ImageExploratoryDao;
import com.epam.dlab.backendapi.domain.BillingReport;
import com.epam.dlab.backendapi.domain.BillingReportLine;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.resources.dto.BillingFilter;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.BillingService;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.util.BillingUtils;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.billing.BillingData;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.client.utils.URIBuilder;

import javax.ws.rs.core.GenericType;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BillingServiceImpl implements BillingService {
    private static final String BILLING_PATH = "/api/billing";
    private static final String USAGE_DATE_FORMAT = "yyyy-MM";

    private final ProjectService projectService;
    private final EndpointService endpointService;
    private final ExploratoryService exploratoryService;
    private final SelfServiceApplicationConfiguration configuration;
    private final RESTService provisioningService;
    private final ImageExploratoryDao imageExploratoryDao;
    private final BillingDAO billingDAO;
    private final String sbn;

    @Inject
    public BillingServiceImpl(ProjectService projectService, EndpointService endpointService,
                              ExploratoryService exploratoryService, SelfServiceApplicationConfiguration configuration,
                              @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService, ImageExploratoryDao imageExploratoryDao,
                              BillingDAO billingDAO) {
        this.projectService = projectService;
        this.endpointService = endpointService;
        this.exploratoryService = exploratoryService;
        this.configuration = configuration;
        this.provisioningService = provisioningService;
        this.imageExploratoryDao = imageExploratoryDao;
        this.billingDAO = billingDAO;
        sbn = configuration.getServiceBaseName();
    }

    @Override
    public BillingReport getBillingReport(UserInfo user, BillingFilter filter) {
        setUserFilter(user, filter);
        List<BillingReportLine> billingReportLines = billingDAO.aggregateBillingData(filter);
        LocalDate min = billingReportLines.stream().min(Comparator.comparing(BillingReportLine::getUsageDateFrom)).map(BillingReportLine::getUsageDateFrom).orElse(null);
        LocalDate max = billingReportLines.stream().max(Comparator.comparing(BillingReportLine::getUsageDateTo)).map(BillingReportLine::getUsageDateTo).orElse(null);
        double sum = billingReportLines.stream().mapToDouble(BillingReportLine::getCost).sum();
        String currency = billingReportLines.stream().map(BillingReportLine::getCurrency).distinct().count() == 1 ? billingReportLines.get(0).getCurrency() : null;
        return BillingReport.builder()
                .sbn(sbn)
                .reportLines(billingReportLines)
                .usageDateFrom(min)
                .usageDateTo(max)
                .totalCost(new BigDecimal(sum).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())
                .currency(currency)
                .isFull(isFullReport(user))
                .build();
    }

    @Override
    public String downloadReport(UserInfo user, BillingFilter filter) {
        BillingReport report = getBillingReport(user, filter);
        StringBuilder builder = new StringBuilder(BillingUtils.getFirstLine(report.getSbn(), report.getUsageDateFrom(), report.getUsageDateTo()));
        builder.append(BillingUtils.getHeader());
        try {
            report.getReportLines().forEach(r -> builder.append(BillingUtils.printLine(r)));
            builder.append(BillingUtils.getTotal(report.getTotalCost(), report.getCurrency()));
            return builder.toString();
        } catch (Exception e) {
            log.error("Cannot write billing data ", e);
            throw new DlabException("Cannot write billing file ", e);
        }
    }

    public List<BillingReportLine> getExploratoryBillingData(String exploratoryId, List<String> resources) {
        List<String> dlabIds = null;
        try {
            dlabIds = Stream.concat(
                    BillingUtils.getExploratoryIds(exploratoryId).stream(),
                    resources
                            .stream()
                            .map(BillingUtils::getComputationalIds)
                            .flatMap(Collection::stream)
            )
                    .collect(Collectors.toList());

            return billingDAO.findBillingData(dlabIds);
        } catch (Exception e) {
            log.error("Cannot retrieve billing information for {} {}", dlabIds, e.getMessage());
            return Collections.emptyList();
        }
    }

    public void updateRemoteBillingData(UserInfo userInfo) {
        List<EndpointDTO> endpoints = endpointService.getEndpoints();
        if (CollectionUtils.isEmpty(endpoints)) {
            log.error("Cannot update billing info. There are no endpoints");
            throw new DlabException("Cannot update billing info. There are no endpoints");
        }

        Map<EndpointDTO, List<BillingData>> billingDataMap = endpoints
                .stream()
                .collect(Collectors.toMap(e -> e, e -> getBillingData(userInfo, e)));

        billingDataMap.forEach((endpointDTO, billingData) -> {
            log.info("Updating billing information for endpoint {}", endpointDTO.getName());
            updateBillingData(userInfo, endpointDTO, billingData);
        });
    }

    private Map<String, BillingReportLine> getBillableResources(UserInfo userInfo) {
        Set<ProjectDTO> projects;
        if (isFullReport(userInfo)) {
            projects = new HashSet<>(projectService.getProjects());
        } else {
            projects = new HashSet<>(projectService.getProjects(userInfo));
            projects.addAll(projectService.getUserProjects(userInfo, false));
        }

        final Stream<BillingReportLine> ssnBillingDataStream = BillingUtils.ssnBillingDataStream(sbn);
        final Stream<BillingReportLine> billableEdges = projects
                .stream()
                .collect(Collectors.toMap(ProjectDTO::getName, ProjectDTO::getEndpoints))
                .entrySet()
                .stream()
                .flatMap(e -> projectEdges(sbn, e.getKey(), e.getValue()));
        final Stream<BillingReportLine> billableSharedEndpoints = endpointService.getEndpoints()
                .stream()
                .flatMap(endpoint -> BillingUtils.sharedEndpointBillingDataStream(endpoint.getName(), sbn));
        final Stream<BillingReportLine> billableUserInstances = exploratoryService.findAll(projects)
                .stream()
                .filter(userInstance -> Objects.nonNull(userInstance.getExploratoryId()))
                .flatMap(ui -> BillingUtils.exploratoryBillingDataStream(ui, configuration.getMaxSparkInstanceCount(), sbn));
        final Stream<BillingReportLine> customImages = projects
                .stream()
                .map(p -> imageExploratoryDao.getImagesForProject(p.getName()))
                .flatMap(Collection::stream)
                .flatMap(i -> BillingUtils.customImageBillingDataStream(i, sbn));

        final Map<String, BillingReportLine> billableResources = Stream.of(ssnBillingDataStream, billableEdges, billableSharedEndpoints, billableUserInstances, customImages)
                .flatMap(s -> s)
                .collect(Collectors.toMap(BillingReportLine::getDlabId, b -> b));
        log.debug("Billable resources are: {}", billableResources);

        return billableResources;
    }

    private Stream<BillingReportLine> projectEdges(String serviceBaseName, String projectName, List<ProjectEndpointDTO> endpoints) {
        return endpoints
                .stream()
                .flatMap(endpoint -> BillingUtils.edgeBillingDataStream(projectName, serviceBaseName, endpoint.getName(),
                        endpoint.getStatus().toString()));
    }

    private void updateBillingData(UserInfo userInfo, EndpointDTO endpointDTO, List<BillingData> billingData) {
        final String endpointName = endpointDTO.getName();
        final CloudProvider cloudProvider = endpointDTO.getCloudProvider();
        final Map<String, BillingReportLine> billableResources = getBillableResources(userInfo);

        final Stream<BillingReportLine> billingReportLineStream = billingData
                .stream()
                .filter(bd -> billableResources.containsKey(bd.getTag()))
                .peek(bd -> bd.setApplication(endpointName))
                .map(bd -> toBillingReport(bd, billableResources.get(bd.getTag())));

        if (cloudProvider == CloudProvider.GCP) {
            final Map<String, List<BillingReportLine>> gcpBillingData = billingReportLineStream
                    .collect(Collectors.groupingBy(bd -> bd.getUsageDate().substring(0, USAGE_DATE_FORMAT.length())));
            updateGcpBillingData(endpointName, gcpBillingData);
        } else if (cloudProvider == CloudProvider.AZURE) {
            List<BillingReportLine> billingReportLines = billingReportLineStream
                    .collect(Collectors.toList());
            updateAzureBillingData(billingReportLines);
        }
    }

    private void updateGcpBillingData(String endpointName, Map<String, List<BillingReportLine>> billingData) {
        billingData.forEach((usageDate, billingReportLines) -> {
            billingDAO.deleteByUsageDate(endpointName, usageDate);
            billingDAO.save(billingReportLines);
        });
    }

    private void updateAzureBillingData(List<BillingReportLine> billingReportLines) {
        billingDAO.save(billingReportLines);
    }

    private List<BillingData> getBillingData(UserInfo userInfo, EndpointDTO e) {
        try {
            return provisioningService.get(getBillingUrl(e.getUrl(), BILLING_PATH), userInfo.getAccessToken(),
                    new GenericType<List<BillingData>>() {
                    });
        } catch (Exception ex) {
            log.error("Cannot retrieve billing information for {}. {}", e.getName(), ex.getMessage());
            return Collections.emptyList();
        }
    }

    private String getBillingUrl(String endpointUrl, String path) {
        URI uri;
        try {
            uri = new URI(endpointUrl);
        } catch (URISyntaxException e) {
            log.error("Wrong URI syntax {}", e.getMessage(), e);
            throw new DlabException("Wrong URI syntax");
        }
        return new URIBuilder()
                .setScheme(uri.getScheme())
                .setHost(uri.getHost())
                .setPort(8088)
                .setPath(path)
                .toString();
    }

    private boolean isFullReport(UserInfo userInfo) {
        return UserRoles.checkAccess(userInfo, RoleType.PAGE, "/api/infrastructure_provision/billing",
                userInfo.getRoles());
    }

    private void setUserFilter(UserInfo userInfo, BillingFilter filter) {
        if (!isFullReport(userInfo)) {
            filter.setUsers(Lists.newArrayList(userInfo.getName()));
        }
    }

    private BillingReportLine toBillingReport(BillingData billingData, BillingReportLine billingReportLine) {
        return BillingReportLine.builder()
                .application(billingData.getApplication())
                .cost(BigDecimal.valueOf(billingData.getCost()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())
                .currency(billingData.getCurrency())
                .product(billingData.getProduct())
                .project(billingReportLine.getProject())
                .usageDateFrom(billingData.getUsageDateFrom())
                .usageDateTo(billingData.getUsageDateTo())
                .usageDate(billingData.getUsageDate())
                .usageType(billingData.getUsageType())
                .user(billingReportLine.getUser())
                .dlabId(billingData.getTag())
                .resourceType(billingReportLine.getResourceType())
                .resourceName(billingReportLine.getResourceName())
                .shape(billingReportLine.getShape())
                .build();
    }
}
