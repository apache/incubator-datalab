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
import com.epam.datalab.backendapi.annotation.BudgetLimited;
import com.epam.datalab.backendapi.annotation.Project;
import com.epam.datalab.backendapi.dao.OdahuDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.OdahuCreateDTO;
import com.epam.datalab.backendapi.domain.OdahuDTO;
import com.epam.datalab.backendapi.domain.OdahuFieldsDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.OdahuService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.odahu.OdahuResult;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceConflictException;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.epam.datalab.dto.UserInstanceStatus.CONFIGURING;
import static com.epam.datalab.dto.UserInstanceStatus.CREATING;
import static com.epam.datalab.dto.UserInstanceStatus.FAILED;
import static com.epam.datalab.dto.UserInstanceStatus.RUNNING;
import static com.epam.datalab.dto.UserInstanceStatus.STARTING;
import static com.epam.datalab.dto.UserInstanceStatus.STOPPED;
import static com.epam.datalab.dto.UserInstanceStatus.STOPPING;
import static com.epam.datalab.dto.UserInstanceStatus.TERMINATING;

@Slf4j
public class OdahuServiceImpl implements OdahuService {

	private static final String CREATE_ODAHU_API = "infrastructure/odahu";
	private static final String START_ODAHU_API = "infrastructure/odahu/start";
	private static final String STOP_ODAHU_API = "infrastructure/odahu/stop";
	private static final String TERMINATE_ODAHU_API = "infrastructure/odahu/terminate";

	private final ProjectService projectService;
	private final EndpointService endpointService;
	private final OdahuDAO odahuDAO;
	private final RESTService provisioningService;
	private final RequestBuilder requestBuilder;
	private final RequestId requestId;

	@Inject
	public OdahuServiceImpl(ProjectService projectService, EndpointService endpointService, OdahuDAO odahuDAO,
	                        @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
	                        RequestBuilder requestBuilder, RequestId requestId) {
		this.projectService = projectService;
		this.endpointService = endpointService;
		this.odahuDAO = odahuDAO;
		this.provisioningService = provisioningService;
		this.requestBuilder = requestBuilder;
		this.requestId = requestId;
	}


	@Override
	public List<OdahuDTO> findOdahu() {
		return odahuDAO.findOdahuClusters();
	}

	@Override
	public Optional<OdahuDTO> get(String project, String endpoint) {
		return odahuDAO.getByProjectEndpoint(project, endpoint);
	}

	@BudgetLimited
	@Override
	public void create(@Project String project, OdahuCreateDTO odahuCreateDTO, UserInfo user) {
		log.info("Trying to create odahu cluster for project: " + project);
		final boolean activeCluster = odahuDAO.findOdahuClusters(odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint()).stream()
				.anyMatch(o -> Arrays.asList(CREATING, RUNNING, STARTING, STOPPING, STOPPED, CONFIGURING, TERMINATING).contains(o.getStatus()));
		if (activeCluster) {
			throw new ResourceConflictException(String.format("Odahu cluster already exist in system for project %s " +
					"and endpoint %s", odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint()));
		}
		ProjectDTO projectDTO = projectService.get(project);
		boolean isAdded = odahuDAO.create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
				odahuCreateDTO.getEndpoint(), CREATING, getTags(odahuCreateDTO)));
		if (isAdded) {
			String url = null;
			EndpointDTO endpointDTO = endpointService.get(odahuCreateDTO.getEndpoint());
			try {
				url = endpointDTO.getUrl() + CREATE_ODAHU_API;
				String uuid =
						provisioningService.post(url, user.getAccessToken(),
								requestBuilder.newOdahuCreate(user.getName(), odahuCreateDTO, projectDTO, endpointDTO), String.class);
				requestId.put(user.getName(), uuid);
			} catch (Exception e) {
				log.error("Can not perform {} due to: {}, {}", url, e.getMessage(), e);
				odahuDAO.updateStatus(odahuCreateDTO.getName(), odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint(), FAILED);
			}
		} else {
			throw new DatalabException(String.format("The odahu fields of the %s can not be updated in DB.", project));
		}
	}

	@BudgetLimited
	@Override
	public void start(String name, @Project String project, String endpoint, UserInfo user) {
		log.info("Trying to start odahu cluster for project: " + project);
		odahuDAO.updateStatus(name, project, endpoint, STARTING);
		actionOnCloud(user, START_ODAHU_API, name, project, endpoint);
	}

	@Override
	public void stop(String name, String project, String endpoint, UserInfo user) {
		log.info("Trying to stop odahu cluster for project: " + project);
		odahuDAO.updateStatus(name, project, endpoint, STOPPING);
		actionOnCloud(user, STOP_ODAHU_API, name, project, endpoint);
	}

	@Override
	public void terminate(String name, String project, String endpoint, UserInfo user) {
		log.info("Trying to terminate odahu cluster for project: " + project);
		odahuDAO.findOdahuClusters(project, endpoint).stream()
				.filter(odahuDTO -> name.equals(odahuDTO.getName())
						&& !odahuDTO.getStatus().equals(FAILED))
				.forEach(odahuDTO -> {
					if (RUNNING == odahuDTO.getStatus()) {
						odahuDAO.updateStatus(name, project, endpoint, TERMINATING);
						actionOnCloud(user, TERMINATE_ODAHU_API, name, project, endpoint);
					} else {
						log.error("Cannot terminate odahu cluster {}", odahuDTO);
						throw new DatalabException(String.format("Cannot terminate odahu cluster %s", odahuDTO));
					}
				});
	}

	@Override
	public void updateStatus(OdahuResult result, UserInstanceStatus status) {
		if (Objects.nonNull(result.getResourceUrls()) && !result.getResourceUrls().isEmpty()) {
			odahuDAO.updateStatusAndUrls(result, status);
		} else {
			odahuDAO.updateStatus(result.getName(), result.getProjectName(), result.getEndpointName(), status);
		}
	}

	@Override
	public boolean inProgress(String project, String endpoint) {
		return get(project, endpoint)
				.filter(odahu -> Arrays.asList(CREATING, STARTING, STOPPING, TERMINATING).contains(odahu.getStatus()))
				.isPresent();
	}

	private void actionOnCloud(UserInfo user, String uri, String name, String project, String endpoint) {
		String url = null;
		EndpointDTO endpointDTO = endpointService.get(endpoint);
		ProjectDTO projectDTO = projectService.get(project);
		try {
			OdahuFieldsDTO fields = odahuDAO.getFields(name, project, endpoint);
			url = endpointDTO.getUrl() + uri;
			String uuid =
					provisioningService.post(url, user.getAccessToken(),
							requestBuilder.newOdahuAction(user.getName(), name, projectDTO, endpointDTO, fields), String.class);
			requestId.put(user.getName(), uuid);
		} catch (Exception e) {
			log.error("Can not perform {} due to: {}, {}", url, e.getMessage(), e);
			odahuDAO.updateStatus(name, project, project, FAILED);
		}
	}

	private Map<String, String> getTags(OdahuCreateDTO odahuCreateDTO) {
		Map<String, String> tags = new HashMap<>();
		tags.put("custom_tag", odahuCreateDTO.getCustomTag());
		tags.put("project_tag", odahuCreateDTO.getProject());
		tags.put("endpoint_tag", odahuCreateDTO.getEndpoint());
		return tags;
	}
}
