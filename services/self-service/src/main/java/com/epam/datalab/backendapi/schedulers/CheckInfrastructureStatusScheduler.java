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
import com.epam.datalab.backendapi.schedulers.internal.Scheduled;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.InfrastructureInfoService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.service.SecurityService;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.dto.status.EnvResource;
import com.epam.datalab.model.ResourceType;
import com.google.inject.Inject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Scheduled("checkInfrastructureStatusScheduler")
public class CheckInfrastructureStatusScheduler implements Job {

	private static final List<UserInstanceStatus> statusesToCheck = Arrays.asList(UserInstanceStatus.RUNNING, UserInstanceStatus.STOPPED);
	private final InfrastructureInfoService infrastructureInfoService;
	private final SecurityService securityService;
	private final EndpointService endpointService;
	private final ExploratoryDAO exploratoryDAO;
	private final ProjectService projectService;

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
		UserInfo serviceUser = securityService.getServiceAccountInfo("admin");

		List<String> activeEndpoints = endpointService.getEndpointsWithStatus(EndpointDTO.EndpointStatus.ACTIVE)
				.stream()
				.map(EndpointDTO::getName)
				.collect(Collectors.toList());

		List<UserInstanceDTO> userInstanceDTOS = exploratoryDAO.fetchExploratoriesByEndpointWhereStatusIn(activeEndpoints, statusesToCheck, Boolean.TRUE);

		Map<String, List<EnvResource>> hostInstanceIds = userInstanceDTOS
				.stream()
				.collect(Collectors.toMap(UserInstanceDTO::getEndpoint, this::getHostInstanceIds));

		activeEndpoints.forEach(e ->
				hostInstanceIds.merge(e, getEdgeInstanceIds(e), (v1, v2) ->
						Stream.of(v1, v2)
								.flatMap(Collection::stream)
								.collect(Collectors.toList())
				)
		);

		hostInstanceIds.forEach((endpoint, ids) ->
				infrastructureInfoService.updateInfrastructureStatuses(serviceUser, endpoint, ids));
	}

	private List<EnvResource> getHostInstanceIds(UserInstanceDTO userInstanceDTO) {
		List<EnvResource> instanceIds = userInstanceDTO.getResources()
				.stream()
				.filter(c -> DataEngineType.SPARK_STANDALONE == DataEngineType.fromDockerImageName(c.getImageName()))
				.filter(c -> statusesToCheck.contains(UserInstanceStatus.of(c.getStatus())))
				.map(r -> new EnvResource()
						.withId(r.getInstanceId())
						.withName(r.getComputationalName())
						.withResourceType(ResourceType.COMPUTATIONAL))
				.collect(Collectors.toList());

		instanceIds.add(new EnvResource()
				.withId(userInstanceDTO.getInstanceId())
				.withName(userInstanceDTO.getExploratoryName())
				.withResourceType(ResourceType.EXPLORATORY));

		return instanceIds;
	}

	private List<EnvResource> getEdgeInstanceIds(String endpoint) {
		return projectService.getProjectsByEndpoint(endpoint)
				.stream()
				.map(ProjectDTO::getEndpoints)
				.flatMap(Collection::stream)
				.filter(e -> statusesToCheck.contains(e.getStatus()))
				.filter(e -> e.getName().equals(endpoint))
				.filter(e -> Objects.nonNull(e.getEdgeInfo()))
				.map(e -> new EnvResource()
						.withId(e.getEdgeInfo().getInstanceId())
						.withName(e.getName())
						.withResourceType(ResourceType.EDGE)
				)
				.collect(Collectors.toList());
	}
}
