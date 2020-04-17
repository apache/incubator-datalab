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
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.UserSettingsDAO;
import com.epam.dlab.backendapi.domain.OdahuDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.epam.dlab.backendapi.resources.dto.UserResourceInfo;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
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
import static java.util.stream.Collectors.toList;

@Singleton
@Slf4j
public class EnvironmentServiceImpl implements EnvironmentService {

	private static final String ERROR_MSG_FORMAT = "Can not %s environment because on of user resource is in status " +
			"CREATING or STARTING";
	@Inject
	private EnvDAO envDAO;
	@Inject
	private ExploratoryDAO exploratoryDAO;
	@Inject
	private ExploratoryService exploratoryService;
	@Inject
	private ComputationalService computationalService;
	@Inject
	private SecurityService securityService;
	@Inject
	private ProjectService projectService;
	@Inject
	private UserSettingsDAO settingsDAO;

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
	public Set<String> getUserNames() {
		log.debug("Getting all users...");
		return envDAO.fetchAllUsers();
	}

	@Override
	public List<UserResourceInfo> getAllEnv() {
		log.debug("Getting all user's environment...");
		List<UserInstanceDTO> expList = exploratoryDAO.getInstances();
		return projectService.getProjects()
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
	public void stopEnvironment(UserInfo userInfo, String user, String project) {
		log.debug("Stopping environment for user {}", user);
		checkState(user, "stop");
		exploratoryDAO.fetchRunningExploratoryFields(user)
				.forEach(e -> stopExploratory(userInfo, user, project, e.getExploratoryName()));
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

		projectService.get(project).getEndpoints().stream()
				.filter(e -> UserInstanceStatus.RUNNING == e.getStatus())
				.forEach(endpoint -> projectService.stop(securityService.getServiceAccountInfo("admin"),
						endpoint.getName(), project));
	}

	@Override
	public void stopExploratory(UserInfo userInfo, String user, String project, String exploratoryName) {
		exploratoryService.stop(new UserInfo(user, userInfo.getAccessToken()), project, exploratoryName);
	}

	@Override
	public void stopComputational(UserInfo userInfo, String user, String project, String exploratoryName, String computationalName) {
		computationalService.stopSparkCluster(new UserInfo(user, userInfo.getAccessToken()), project, exploratoryName,
				computationalName);
	}

	@Override
	public void terminateExploratory(UserInfo userInfo, String user, String project, String exploratoryName) {
		exploratoryService.terminate(new UserInfo(user, userInfo.getAccessToken()), project, exploratoryName);
	}

	@Override
	public void terminateComputational(UserInfo userInfo, String user, String project, String exploratoryName, String computationalName) {
		computationalService.terminateComputational(new UserInfo(user, userInfo.getAccessToken()), project, exploratoryName,
				computationalName);
	}

	private UserDTO toUserDTO(String u, UserDTO.Status status) {
		return new UserDTO(u, settingsDAO.getAllowedBudget(u).orElse(null), status);
	}

	private void checkState(String user, String action) {
		final List<UserInstanceDTO> userInstances = exploratoryDAO
				.fetchUserExploratoriesWhereStatusIn(user,
						Arrays.asList(UserInstanceStatus.CREATING,
								UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE),
						UserInstanceStatus.CREATING,
						UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
		if (!userInstances.isEmpty()) {
			log.error(String.format(ERROR_MSG_FORMAT, action));
			throw new ResourceConflictException(String.format(ERROR_MSG_FORMAT, action));
		}
	}

	private void stopNotebookWithServiceAccount(UserInstanceDTO instance) {
		final UserInfo userInfo = securityService.getServiceAccountInfo(instance.getUser());
		exploratoryService.stop(userInfo, instance.getProject(), instance.getExploratoryName());
	}

	private List<UserResourceInfo> getProjectEnv(ProjectDTO projectDTO, List<UserInstanceDTO> allInstances) {
		final Stream<UserResourceInfo> userResources = allInstances.stream()
				.filter(instance -> instance.getProject().equals(projectDTO.getName()))
				.map(this::toUserResourceInfo);

		Stream<UserResourceInfo> odahuResources = projectDTO.getOdahu().stream()
				.map(this::toUserResourceInfo);

		if (projectDTO.getEndpoints() != null) {
			final Stream<UserResourceInfo> edges = projectDTO.getEndpoints()
					.stream()
					.map(e -> new UserResourceInfo().withResourceType(ResourceEnum.EDGE_NODE)
							.withResourceStatus(e.getStatus().toString())
							.withProject(projectDTO.getName())
							.withIp(e.getEdgeInfo() != null ? e.getEdgeInfo().getPublicIp() : null));
			return Stream.concat(edges, Stream.concat(odahuResources, userResources))
					.collect(toList());
		} else {
			return userResources.collect(toList());
		}
	}

	private UserResourceInfo toUserResourceInfo(UserInstanceDTO userInstance) {
		return new UserResourceInfo().withResourceType(ResourceEnum.NOTEBOOK)
				.withResourceName(userInstance.getExploratoryName())
				.withResourceShape(userInstance.getShape())
				.withResourceStatus(userInstance.getStatus())
				.withCompResources(userInstance.getResources())
				.withUser(userInstance.getUser())
				.withProject(userInstance.getProject())
				.withCloudProvider(userInstance.getCloudProvider());
	}

	private UserResourceInfo toUserResourceInfo(OdahuDTO odahuDTO) {
		return new UserResourceInfo()
				.withResourceType(ResourceEnum.ODAHU)
				.withResourceName(odahuDTO.getName())
				.withResourceStatus(odahuDTO.getStatus().toString())
				.withProject(odahuDTO.getProject());
	}

	private void checkProjectResourceConditions(String project, String action) {
		final List<UserInstanceDTO> userInstances = exploratoryDAO
				.fetchProjectExploratoriesWhereStatusIn(project,
						Arrays.asList(UserInstanceStatus.CREATING, UserInstanceStatus.STARTING,
								UserInstanceStatus.CREATING_IMAGE),
						UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE);
		if (!userInstances.isEmpty()) {
			log.error(String.format(ERROR_MSG_FORMAT, action));
			throw new ResourceConflictException(String.format(ERROR_MSG_FORMAT, action));
		}
	}
}
