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

package com.epam.datalab.backendapi.schedulers;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectEndpointDTO;
import com.epam.datalab.backendapi.schedulers.internal.Scheduled;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.InfrastructureInfoService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.service.SecurityService;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.dto.status.EnvResource;
import com.epam.datalab.model.ResourceType;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.datalab.dto.UserInstanceStatus.*;

@Scheduled("checkInfrastructureStatusScheduler")
@Slf4j
public class CheckInfrastructureStatusScheduler implements Job {

    private static final List<UserInstanceStatus> statusesToCheck =
            Arrays.asList(STARTING, CREATING, RUNNING, STOPPING, RECONFIGURING, STOPPED, TERMINATING, TERMINATED, FAILED);

    private final InfrastructureInfoService infrastructureInfoService;
    private final SecurityService securityService;
    private final EndpointService endpointService;
    private final ExploratoryDAO exploratoryDAO;
    private final ProjectService projectService;
    private static final String AWS_EMR_CLUSTER = "AWS EMR cluster";


    @Inject
    public CheckInfrastructureStatusScheduler(InfrastructureInfoService infrastructureInfoService, SecurityService securityService,
                                              EndpointService endpointService, ExploratoryDAO exploratoryDAO, ProjectService projectService) {
        this.infrastructureInfoService = infrastructureInfoService;
        this.securityService = securityService;
        this.endpointService = endpointService;
        this.exploratoryDAO = exploratoryDAO;
        this.projectService = projectService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Trying to update infrastructure statuses");
        UserInfo serviceUser = securityService.getServiceAccountInfo("admin");

        List<String> activeEndpoints = endpointService.getEndpointsWithStatus(EndpointDTO.EndpointStatus.ACTIVE)
                .stream()
                .map(EndpointDTO::getName)
                .collect(Collectors.toList());

        List<UserInstanceDTO> userInstanceDTOS = exploratoryDAO.fetchExploratoriesByEndpointWhereStatusIn(activeEndpoints, statusesToCheck, Boolean.TRUE)
        .stream().filter(e -> e.getInstanceId() != null).collect(Collectors.toList());

        Map<String, List<EnvResource>> exploratoryAndSparkInstances = userInstanceDTOS
                .stream()
                .map(this::getExploratoryAndSparkInstances)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(EnvResource::getEndpoint));

        Map<String, List<EnvResource>> clusterInstances = userInstanceDTOS
                .stream()
                .map(this::getClusterInstances)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(EnvResource::getEndpoint));
        activeEndpoints.forEach(e -> {
                    List<EnvResource> hostInstances = Stream.of(getEdgeInstances(e),
                            exploratoryAndSparkInstances.getOrDefault(e, Collections.emptyList()))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());

                    infrastructureInfoService.updateInfrastructureStatuses(serviceUser, e, hostInstances,
                            clusterInstances.getOrDefault(e, Collections.emptyList()));
                }
        );
    }

    private List<EnvResource> getExploratoryAndSparkInstances(UserInstanceDTO userInstanceDTO) {
        List<EnvResource> instances = userInstanceDTO.getResources()
                .stream()
                .filter(c -> DataEngineType.SPARK_STANDALONE == DataEngineType.fromDockerImageName(c.getImageName()))
                .filter(c -> statusesToCheck.contains(UserInstanceStatus.of(c.getStatus())))
                .filter(this::noEmrCreating)
                .filter(c -> c.getInstanceId() != null)
                .map(r -> new EnvResource()
                        .withId(r.getInstanceId())
                        .withName(r.getComputationalName())
                        .withProject(userInstanceDTO.getProject())
                        .withEndpoint(userInstanceDTO.getEndpoint())
                        .withStatus(r.getStatus())
                        .withResourceType(ResourceType.COMPUTATIONAL))
                .collect(Collectors.toList());

        instances.add(new EnvResource()
                .withId(userInstanceDTO.getInstanceId())
                .withName(userInstanceDTO.getExploratoryName())
                .withProject(userInstanceDTO.getProject())
                .withEndpoint(userInstanceDTO.getEndpoint())
                .withStatus(userInstanceDTO.getStatus())
                .withResourceType(ResourceType.EXPLORATORY));

        return instances;
    }

    private List<EnvResource> getClusterInstances(UserInstanceDTO userInstanceDTO) {
        return userInstanceDTO.getResources().stream()
                .filter(c -> DataEngineType.CLOUD_SERVICE == DataEngineType.fromDockerImageName(c.getImageName()))
                .filter(c -> statusesToCheck.contains(UserInstanceStatus.of(c.getStatus())))
                .filter(c -> c.getInstanceId() != null)
                .filter(this::noEmrCreating)
                .map(r -> new EnvResource()
                        .withId(r.getInstanceId())
                        .withName(r.getComputationalName())
                        .withProject(userInstanceDTO.getProject())
                        .withEndpoint(userInstanceDTO.getEndpoint())
                        .withStatus(r.getStatus())
                        .withResourceType(ResourceType.COMPUTATIONAL))
                .collect(Collectors.toList());
    }

    private boolean noEmrCreating(UserComputationalResource c) {
        return !(c.getStatus().equals(CREATING.name()) && c.getTemplateName().contains(AWS_EMR_CLUSTER));
    }


    private List<EnvResource> getEdgeInstances(String endpoint) {
        return projectService.getProjectsByEndpoint(endpoint)
                .stream()
                .collect(Collectors.toMap(ProjectDTO::getName, ProjectDTO::getEndpoints))
                .entrySet()
                .stream()
                .map(entry -> getEdgeInstances(endpoint, entry))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<EnvResource> getEdgeInstances(String endpoint, Map.Entry<String, List<ProjectEndpointDTO>> entry) {
        return entry.getValue()
                .stream()
                .filter(e -> statusesToCheck.contains(e.getStatus()))
                .filter(e -> e.getName().equals(endpoint))
                .filter(e -> Objects.nonNull(e.getEdgeInfo()))
                .map(e -> new EnvResource()
                        .withId(e.getEdgeInfo().getInstanceId())
                        .withName(e.getName())
                        .withProject(entry.getKey())
                        .withEndpoint(endpoint)
                        .withStatus(e.getStatus().toString())
                        .withResourceType(ResourceType.EDGE)
                )
                .collect(Collectors.toList());
    }
}
