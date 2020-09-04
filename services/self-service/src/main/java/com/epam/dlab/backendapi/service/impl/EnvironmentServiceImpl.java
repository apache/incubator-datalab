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
import com.epam.dlab.backendapi.annotation.Project;
import com.epam.dlab.backendapi.annotation.ProjectAdmin;
import com.epam.dlab.backendapi.annotation.User;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.UserSettingsDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.epam.dlab.backendapi.resources.dto.UserResourceInfo;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.model.ResourceEnum;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.epam.dlab.backendapi.resources.dto.UserDTO.Status.ACTIVE;
import static com.epam.dlab.backendapi.resources.dto.UserDTO.Status.NOT_ACTIVE;
import static com.epam.dlab.dto.UserInstanceStatus.CREATING;
import static com.epam.dlab.dto.UserInstanceStatus.CREATING_IMAGE;
import static com.epam.dlab.dto.UserInstanceStatus.RUNNING;
import static com.epam.dlab.dto.UserInstanceStatus.STARTING;
import static com.epam.dlab.rest.contracts.ComputationalAPI.AUDIT_MESSAGE;
import static java.util.stream.Collectors.toList;

@Singleton
@Slf4j
public class EnvironmentServiceImpl implements EnvironmentService {
	private static final String ERROR_MSG_FORMAT = "Can not %s environment because on of user resource is in status CREATING or STARTING";
	private static final String AUDIT_QUOTA_MESSAGE = "Billing quota reached";
	private static final String DLAB_SYSTEM_USER = "DLab system user";

	private final EnvDAO envDAO;
	private final UserSettingsDAO settingsDAO;
	private final ExploratoryDAO exploratoryDAO;
	private final ExploratoryService exploratoryService;
	private final ComputationalService computationalService;
	private final SecurityService securityService;
	private final ProjectService projectService;

	@Inject
	public EnvironmentServiceImpl(EnvDAO envDAO, UserSettingsDAO settingsDAO, ExploratoryDAO exploratoryDAO,
								  ExploratoryService exploratoryService, ComputationalService computationalService,
								  SecurityService securityService, ProjectService projectService) {
		this.envDAO = envDAO;
		this.settingsDAO = settingsDAO;
		this.exploratoryDAO = exploratoryDAO;
		this.exploratoryService = exploratoryService;
		this.computationalService = computationalService;
		this.securityService = securityService;
		this.projectService = projectService;
	}

	@Override
	public List<UserDTO> getUsers() {
		final Set<String> activeUsers = envDAO.fetchActiveEnvUsers();
		log.trace("Active users: {}", activeUsers);
		final Set<String> notActiveUsers = envDAO.fetchUsersNotIn(activeUsers);
		log.trace("Not active users: {}", notActiveUsers);
		final Stream<UserDTO> activeUsersStream = activeUsers
				.stream()
				.map(u -> toUserDTO(u, ACTIVE));
		final Stream<UserDTO> notActiveUsersStream = notActiveUsers
				.stream()
				.map(u -> toUserDTO(u, NOT_ACTIVE));
		return Stream.concat(activeUsersStream, notActiveUsersStream)
				.collect(toList());
	}

	@Override
	public List<UserResourceInfo> getAllEnv(UserInfo user) {
		log.debug("Getting all user's environment...");
		List<UserInstanceDTO> expList = exploratoryDAO.getInstances();
		return projectService.getProjects(user)
				.stream()
				.map(projectDTO -> getProjectEnv(projectDTO, expList))
				.flatMap(Collection::stream)
				.collect(toList());
	}

	@Override
	public void stopAll() {
		log.debug("Stopping environment for all users...");
		projectService.getProjects()
				.stream()
				.map(ProjectDTO::getName)
				.forEach(this::stopProjectEnvironment);
	}

	@Override
	public void stopEnvironmentWithServiceAccount(String user) {
		log.debug("Stopping environment for user {} by scheduler", user);
		checkState(user, "stop");
		exploratoryDAO.fetchRunningExploratoryFields(user)
				.forEach(this::stopNotebookWithServiceAccount);
	}

	@Override
	public void stopProjectEnvironment(String project) {
		log.debug("Stopping environment for project {}", project);
		checkProjectResourceConditions(project, "stop");
		exploratoryDAO.fetchRunningExploratoryFieldsForProject(project)
				.forEach(this::stopNotebookWithServiceAccount);

		projectService.get(project).getEndpoints()
				.stream()
				.filter(e -> RUNNING == e.getStatus())
                .forEach(endpoint -> projectService.stop(securityService.getServiceAccountInfo(DLAB_SYSTEM_USER),
                        endpoint.getName(), project, AUDIT_QUOTA_MESSAGE));
	}

	@ProjectAdmin
	@Override
	public void stopExploratory(@User UserInfo userInfo, String user, @Project String project, String exploratoryName) {
		exploratoryService.stop(userInfo, user, project, exploratoryName, null);
	}

	@ProjectAdmin
    @Override
    public void stopComputational(@User UserInfo userInfo, String user, @Project String project, String exploratoryName, String computationalName) {
        computationalService.stopSparkCluster(userInfo, user, project, exploratoryName, computationalName,
                String.format(AUDIT_MESSAGE, exploratoryName));
    }

	@ProjectAdmin
	@Override
	public void terminateExploratory(@User UserInfo userInfo, String user, @Project String project, String exploratoryName) {
		exploratoryService.terminate(userInfo, user, project, exploratoryName, null);
	}

	@ProjectAdmin
	@Override
	public void terminateComputational(@User UserInfo userInfo, String user, @Project String project, String exploratoryName, String computationalName) {
        computationalService.terminateComputational(userInfo, user, project, exploratoryName, computationalName, String.format(AUDIT_MESSAGE, exploratoryName));
    }

	private UserDTO toUserDTO(String u, UserDTO.Status status) {
		return new UserDTO(u, settingsDAO.getAllowedBudget(u).orElse(null), status);
	}

	private void checkState(String user, String action) {
		final List<UserInstanceDTO> userInstances = exploratoryDAO.fetchUserExploratoriesWhereStatusIn(user, Arrays.asList(CREATING, STARTING, CREATING_IMAGE),
				CREATING, STARTING, CREATING_IMAGE);
		if (!userInstances.isEmpty()) {
			log.error(String.format(ERROR_MSG_FORMAT, action));
			throw new ResourceConflictException(String.format(ERROR_MSG_FORMAT, action));
		}
	}

	private void stopNotebookWithServiceAccount(UserInstanceDTO instance) {
        final UserInfo userInfo = securityService.getServiceAccountInfo(DLAB_SYSTEM_USER);
        exploratoryService.stop(userInfo, instance.getUser(), instance.getProject(), instance.getExploratoryName(), AUDIT_QUOTA_MESSAGE);
    }

	private List<UserResourceInfo> getProjectEnv(ProjectDTO projectDTO, List<UserInstanceDTO> allInstances) {
		final Stream<UserResourceInfo> userResources = allInstances
				.stream()
				.filter(instance -> instance.getProject().equals(projectDTO.getName()))
				.map(this::toUserResourceInfo);
		if (projectDTO.getEndpoints() != null) {
			final Stream<UserResourceInfo> edges = projectDTO.getEndpoints()
					.stream()
					.map(e -> UserResourceInfo.builder()
							.resourceType(ResourceEnum.EDGE_NODE)
							.resourceStatus(e.getStatus().toString())
							.project(projectDTO.getName())
							.ip(e.getEdgeInfo() != null ? e.getEdgeInfo().getPublicIp() : null)
							.build());
			return Stream.concat(edges, userResources).collect(toList());
		} else {
			return userResources.collect(toList());
		}
	}

	private UserResourceInfo toUserResourceInfo(UserInstanceDTO userInstance) {
		return UserResourceInfo.builder()
				.resourceType(ResourceEnum.NOTEBOOK)
				.resourceName(userInstance.getExploratoryName())
				.resourceShape(userInstance.getShape())
				.resourceStatus(userInstance.getStatus())
				.computationalResources(userInstance.getResources())
				.user(userInstance.getUser())
				.project(userInstance.getProject())
				.cloudProvider(userInstance.getCloudProvider())
				.exploratoryUrls(userInstance.getResourceUrl())
				.build();
	}

	private void checkProjectResourceConditions(String project, String action) {
		final List<UserInstanceDTO> userInstances = exploratoryDAO.fetchProjectExploratoriesWhereStatusIn(project,
				Arrays.asList(CREATING, STARTING, CREATING_IMAGE), CREATING, STARTING, CREATING_IMAGE);

		if (!userInstances.isEmpty()) {
			log.error(String.format(ERROR_MSG_FORMAT, action));
			throw new ResourceConflictException(String.format(ERROR_MSG_FORMAT, action));
		}
	}
}
