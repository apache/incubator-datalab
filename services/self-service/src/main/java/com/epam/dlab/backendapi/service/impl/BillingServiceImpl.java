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
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class BillingServiceImpl implements BillingService {
    private static final String BILLING_PATH = "/api/billing";
    private static final String BILLING_REPORT_PATH = "/api/billing/report";

    private final ProjectService projectService;
    private final EndpointService endpointService;
    private final ExploratoryService exploratoryService;
    private final SelfServiceApplicationConfiguration configuration;
    private final RESTService provisioningService;
    private final ImageExploratoryDao imageExploratoryDao;
    private final String sbn;

    @Inject
    public BillingServiceImpl(ProjectService projectService, EndpointService endpointService,
                              ExploratoryService exploratoryService, SelfServiceApplicationConfiguration configuration,
                              @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService, ImageExploratoryDao imageExploratoryDao) {
        this.projectService = projectService;
        this.endpointService = endpointService;
        this.exploratoryService = exploratoryService;
        this.configuration = configuration;
        this.provisioningService = provisioningService;
        this.imageExploratoryDao = imageExploratoryDao;
        sbn = configuration.getServiceBaseName();
    }

    @Override
    public BillingReport getBillingReport(UserInfo user, BillingFilter filter) {
        List<BillingReportLine> billingReportLines = getBillingReportLines(user, filter, isFullReport(user));
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

    @Override
    public List<BillingReportLine> getBillingReportLines(UserInfo user, BillingFilter filter, boolean isFullReport) {
        setUserFilter(user, filter);
        Set<ProjectDTO> projects;
        if (isFullReport) {
            projects = new HashSet<>(projectService.getProjects());
        } else {
            projects = new HashSet<>(projectService.getProjects(user));
            projects.addAll(projectService.getUserProjects(user, false));
        }

        final Map<String, BillingReportLine> billableResources = getBillableResources(projects);

        List<BillingReportLine> billingReport = getRemoteBillingData(user, filter)
                .stream()
                .filter(bd -> billableResources.containsKey(bd.getTag()))
                .map(bd -> toBillingData(bd, billableResources.get(bd.getTag())))
                .filter(getBillingReportFilter(filter))
                .collect(Collectors.toList());
        log.debug("Billing report: {}", billingReport);

        return billingReport;
    }

    private Map<String, BillingReportLine> getBillableResources(Set<ProjectDTO> projects) {
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

    public List<BillingData> getExploratoryRemoteBillingData(UserInfo user, String endpoint, List<UserInstanceDTO> userInstanceDTOS) {
        List<String> dlabIds = null;
        try {
            dlabIds = userInstanceDTOS
                    .stream()
                    .map(instance -> Stream.concat(BillingUtils.getExploratoryIds(instance.getExploratoryId()).stream(), instance.getResources()
                            .stream()
                            .map(cr -> BillingUtils.getComputationalIds(cr.getComputationalId()))
                            .flatMap(Collection::stream)
                    ))
                    .flatMap(a -> a)
                    .collect(Collectors.toList());

            EndpointDTO endpointDTO = endpointService.get(endpoint);
            return provisioningService.get(getBillingUrl(endpointDTO.getUrl(), BILLING_PATH), user.getAccessToken(),
                    new GenericType<List<BillingData>>() {
                    }, Collections.singletonMap("dlabIds", String.join(",", dlabIds)));
        } catch (Exception e) {
            log.error("Cannot retrieve billing information for {} {}", dlabIds, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<BillingData> getRemoteBillingData(UserInfo userInfo, BillingFilter filter) {
        List<EndpointDTO> endpoints = endpointService.getEndpoints();
        ExecutorService executor = Executors.newFixedThreadPool(endpoints.size());
        List<Callable<List<BillingData>>> callableTasks = new ArrayList<>();
        endpoints.forEach(e -> callableTasks.add(getTask(userInfo, getBillingUrl(e.getUrl(), BILLING_REPORT_PATH), filter)));

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
            throw new DlabException("Cannot retrieve billing information", e);
        }
        executor.shutdown();
        return billingData;
    }

    private List<BillingData> getBillingReportDTOS(Future<List<BillingData>> s) {
        try {
            return s.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Cannot retrieve billing information {}", e.getMessage(), e);
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

    private Callable<List<BillingData>> getTask(UserInfo userInfo, String url, BillingFilter filter) {
        final String dateStart = Optional.ofNullable(filter.getDateStart()).orElse("");
        final String dateEnd = Optional.ofNullable(filter.getDateEnd()).orElse("");
        final String dlabId = Optional.ofNullable(filter.getDlabId()).orElse("");
        final List<String> products = Optional.ofNullable(filter.getProducts()).orElse(Collections.emptyList());
        return () -> provisioningService.get(url, userInfo.getAccessToken(),
                new GenericType<List<BillingData>>() {
                },
                Stream.of(new String[][]{
                        {"date-start", dateStart},
                        {"date-end", dateEnd},
                        {"dlab-id", dlabId},
                        {"product", String.join(",", products)}
                }).collect(Collectors.toMap(data -> data[0], data -> data[1])));
    }

    private Predicate<BillingReportLine> getBillingReportFilter(BillingFilter filter) {
        return br ->
                (CollectionUtils.isEmpty(filter.getUsers()) || filter.getUsers().contains(br.getUser())) &&
                        (CollectionUtils.isEmpty(filter.getProjects()) || filter.getProjects().contains(br.getProject())) &&
                        (CollectionUtils.isEmpty(filter.getResourceTypes()) || filter.getResourceTypes().contains(String.valueOf(br.getResourceType()))) &&
                        (CollectionUtils.isEmpty(filter.getStatuses()) || filter.getStatuses().contains(br.getStatus())) &&
                        (CollectionUtils.isEmpty(filter.getShapes()) || filter.getShapes().contains(br.getShape()));
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
