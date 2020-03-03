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
import com.epam.dlab.backendapi.domain.BillingReport;
import com.epam.dlab.backendapi.domain.BillingReportLine;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.resources.dto.BillingFilter;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.BillingServiceNew;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.util.BillingUtils;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.billing.BillingData;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

import javax.ws.rs.core.GenericType;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BillingServiceImplNew implements BillingServiceNew {
    private final ProjectService projectService;
    private final EndpointService endpointService;
    private final ExploratoryService exploratoryService;
    private final SelfServiceApplicationConfiguration configuration;
    private final RESTService provisioningService;

    @Inject
    public BillingServiceImplNew(ProjectService projectService, EndpointService endpointService,
                                 ExploratoryService exploratoryService, SelfServiceApplicationConfiguration configuration,
                                 @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService) {
        this.projectService = projectService;
        this.endpointService = endpointService;
        this.exploratoryService = exploratoryService;
        this.configuration = configuration;
        this.provisioningService = provisioningService;
    }

    @Override
    public BillingReport getBillingReport(UserInfo user, BillingFilter filter) {
        List<BillingReportLine> billingReportLines = getBillingReportLines(user, filter);
        LocalDate min = billingReportLines.stream().min(Comparator.comparing(BillingReportLine::getUsageDateFrom)).map(BillingReportLine::getUsageDateFrom).orElse(null);
        LocalDate max = billingReportLines.stream().max(Comparator.comparing(BillingReportLine::getUsageDateTo)).map(BillingReportLine::getUsageDateTo).orElse(null);
        double sum = billingReportLines.stream().mapToDouble(BillingReportLine::getCost).sum();
        String currency = billingReportLines.stream().map(BillingReportLine::getCurrency).distinct().count() == 1 ? billingReportLines.get(0).getCurrency() : null;
        return BillingReport.builder()
                .sbn(configuration.getServiceBaseName())
                .reportLines(billingReportLines)
                .usageDateFrom(min)
                .usageDateTo(max)
                .totalCost(new BigDecimal(sum).setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue())
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
            throw new DlabException("Cannot write billing file due to" + e.getMessage());
        }
    }

    @Override
    public List<BillingReportLine> getBillingReportLines(UserInfo user, BillingFilter filter) {
        setUserFilter(user, filter);
        final String serviceBaseName = configuration.getServiceBaseName();
        final Stream<BillingReportLine> ssnBillingDataStream = BillingUtils.ssnBillingDataStream(serviceBaseName);
        final Stream<BillingReportLine> billableUserInstances = exploratoryService.findAll()
                .stream()
                .filter(userInstance -> Objects.nonNull(userInstance.getExploratoryId()))
                .flatMap(BillingUtils::exploratoryBillingDataStream);
        final Stream<BillingReportLine> billableEdges = projectService.getProjects()
                .stream()
                .collect(Collectors.toMap(ProjectDTO::getName, ProjectDTO::getEndpoints))
                .entrySet()
                .stream()
                .flatMap(e -> projectEdges(serviceBaseName, e.getKey(), e.getValue()));

        final Map<String, BillingReportLine> billableResources = Stream.of(billableUserInstances, billableEdges, ssnBillingDataStream)
                .flatMap(s -> s)
                .collect(Collectors.toMap(BillingReportLine::getDlabId, b -> b));
        log.debug("Billable resources are: {}", billableResources);

        List<BillingReportLine> billingReport = getRemoteBillingData(user)
                .stream()
                .filter(getBillingDataFilter(filter))
                .map(bd -> toBillingData(bd, getOrDefault(billableResources, bd.getTag())))
                .filter(getBillingReportFilter(filter))
                .collect(Collectors.toList());
        log.debug("Billing report: {}", billingReport);

        return billingReport;
    }

    private Stream<BillingReportLine> projectEdges(String serviceBaseName, String projectName, List<ProjectEndpointDTO> endpoints) {
        return endpoints
                .stream()
                .flatMap(endpoint -> BillingUtils.edgeBillingDataStream(projectName, serviceBaseName, endpoint.getName(),
                        endpoint.getStatus().toString()));
    }

    private BillingReportLine getOrDefault(Map<String, BillingReportLine> billableResources, String tag) {
        return billableResources.getOrDefault(tag, BillingReportLine.builder().dlabId(tag).build());
    }

    private List<BillingData> getRemoteBillingData(UserInfo userInfo) {
        List<EndpointDTO> endpoints = endpointService.getEndpoints();
        ExecutorService executor = Executors.newFixedThreadPool(endpoints.size());
        List<Callable<List<BillingData>>> callableTasks = new ArrayList<>();
        endpoints.forEach(e -> callableTasks.add(getTask(userInfo, getBillingUrl(e.getUrl()))));

        List<BillingData> billingData;
        try {
            log.debug("Trying to retrieve billing info for {}", endpoints);
            billingData = executor.invokeAll(callableTasks)
                    .stream()
                    .map(this::getBillingReportDTOS)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            executor.shutdown();
            log.error("Cannot retrieve billing information {}", e.getMessage(), e);
            throw new DlabException("Cannot retrieve billing information");
        }
        executor.shutdown();
        return billingData;
    }

    private List<BillingData> getBillingReportDTOS(Future<List<BillingData>> s) {
        try {
            return s.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Cannot retrieve billing information {}", e.getMessage(), e);
            throw new DlabException("Cannot retrieve billing information");
        }
    }

    private String getBillingUrl(String endpointUrl) {
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
                .setPort(8081)
                .setPath("/api/billing/report")
                .toString();
    }

    private Callable<List<BillingData>> getTask(UserInfo userInfo, String url) {
        return () -> provisioningService.get(url, userInfo.getAccessToken(), new GenericType<List<BillingData>>() {
        });
    }

    private Predicate<BillingReportLine> getBillingReportFilter(BillingFilter filter) {
        return br -> (filter.getUsers().isEmpty() || filter.getUsers().contains(br.getUser())) &&
                (filter.getProjects().isEmpty() || filter.getProjects().contains(br.getProject())) &&
                (filter.getResourceTypes().isEmpty() || filter.getResourceTypes().contains(br.getResourceType().name())) &&
                (filter.getStatuses().isEmpty() || filter.getStatuses().contains(br.getStatus())) &&
                (filter.getShapes().isEmpty() || filter.getShapes().contains(br.getShape()));
    }

    private Predicate<BillingData> getBillingDataFilter(BillingFilter filter) {
        return br -> (filter.getDlabId().isEmpty() || filter.getDlabId().equalsIgnoreCase(br.getTag())) &&
                (filter.getDateStart().isEmpty() || LocalDate.parse(filter.getDateStart()).isEqual(br.getUsageDateFrom()) || LocalDate.parse(filter.getDateStart()).isBefore(br.getUsageDateFrom())) &&
                (filter.getDateEnd().isEmpty() || LocalDate.parse(filter.getDateEnd()).isEqual(br.getUsageDateTo()) || LocalDate.parse(filter.getDateEnd()).isAfter(br.getUsageDateTo())) &&
                (filter.getProducts().isEmpty() || filter.getProducts().contains(br.getProduct()));
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

    private BillingReportLine toBillingData(BillingData billingData, BillingReportLine billingReportLine) {
        return BillingReportLine.builder()
                .cost(billingData.getCost())
                .currency(billingData.getCurrency())
                .product(billingData.getProduct())
                .project(billingReportLine.getProject())
                .usageDateTo(billingData.getUsageDateTo())
                .usageDateFrom(billingData.getUsageDateFrom())
                .usageType(billingData.getUsageType())
                .user(billingReportLine.getUser())
                .dlabId(billingData.getTag())
                .resourceType(billingReportLine.getResourceType())
                .resourceName(billingReportLine.getResourceName())
                .status(billingReportLine.getStatus())
                .shape(billingReportLine.getShape())
                .build();
    }
}
