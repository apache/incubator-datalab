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
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.UserSettingsDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.epam.dlab.backendapi.resources.dto.UserResourceInfo;
import com.epam.dlab.backendapi.service.*;
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
	private KeyDAO keyDAO;
	@Inject
	private EdgeService edgeService;
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
				.map(projectDTO -> getProjectEnv(projectDTO, expList)).flatMap(Collection::stream)
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
	public void stopEnvironment(String user) {
		log.debug("Stopping environment for user {}", user);
		checkState(user, "stop");
		exploratoryDAO.fetchRunningExploratoryFields(user)
				.forEach(this::stopNotebook);
		stopEdge(user);
	}

	@Override
	public void stopProjectEnvironment(String project) {
		log.debug("Stopping environment for project {}", project);
		checkProjectResourceConditions(project, "stop");
		exploratoryDAO.fetchRunningExploratoryFieldsForProject(project)
				.forEach(this::stopNotebook);
		if (projectService.get(project).getStatus() == ProjectDTO.Status.ACTIVE) {
			projectService.stop(securityService.getUserInfoOffline("admin"), project);
		}
	}

	@Override
	public void stopEdge(String user) {
		if (UserInstanceStatus.RUNNING.toString().equals(keyDAO.getEdgeStatus(user))) {
			edgeService.stop(securityService.getUserInfoOffline(user));
		}
	}

	@Override
	public void stopExploratory(String user, String exploratoryName) {
		stopNotebook(new UserInstanceDTO().withUser(user).withExploratoryName(exploratoryName));
	}

	@Override
	public void stopComputational(String user, String exploratoryName, String computationalName) {
		stopDataengine(user, exploratoryName, computationalName);
	}

	@Override
	public void terminateAll() {
		log.debug("Terminating environment for all users...");
		getUserNames().forEach(this::terminateEnvironment);
	}

	@Override
	public void terminateEnvironment(String user) {
		log.debug("Terminating environment for user {}", user);
		checkState(user, "terminate");
		if (!terminateEdge(user)) {
			exploratoryDAO.fetchUserExploratoriesWhereStatusNotIn(user, UserInstanceStatus.TERMINATED,
					UserInstanceStatus.FAILED, UserInstanceStatus.TERMINATING)
					.forEach(this::terminateNotebook);
		}
	}

	@Override
	public void terminateExploratory(String user, String exploratoryName) {
		terminateNotebook(new UserInstanceDTO().withUser(user).withExploratoryName(exploratoryName));
	}

	@Override
	public void terminateComputational(String user, String exploratoryName, String computationalName) {
		terminateCluster(user, exploratoryName, computationalName);
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
		if (UserInstanceStatus.STARTING.toString().equals(keyDAO.getEdgeStatus(user)) || !userInstances.isEmpty()) {
			log.error(String.format(ERROR_MSG_FORMAT, action));
			throw new ResourceConflictException(String.format(ERROR_MSG_FORMAT, action));
		}
	}

	private void stopNotebook(UserInstanceDTO instance) {
		final UserInfo userInfo = securityService.getUserInfoOffline(instance.getUser());
		exploratoryService.stop(userInfo, instance.getExploratoryName());
	}

	private void stopDataengine(String user, String exploratoryName, String computationalName) {
		final UserInfo userInfo = securityService.getUserInfoOffline(user);
		computationalService.stopSparkCluster(userInfo, exploratoryName, computationalName);
	}

	private boolean terminateEdge(String user) {
		final boolean nodeExists = keyDAO.edgeNodeExist(user);
		if (nodeExists) {
			edgeService.terminate(securityService.getUserInfoOffline(user));
			exploratoryService.updateExploratoryStatuses(user, UserInstanceStatus.TERMINATING);
		}
		return nodeExists;
	}

	private void terminateNotebook(UserInstanceDTO instance) {
		final UserInfo userInfo = securityService.getUserInfoOffline(instance.getUser());
		exploratoryService.terminate(userInfo, instance.getExploratoryName());
	}

	private void terminateCluster(String user, String exploratoryName, String computationalName) {
		final UserInfo userInfo = securityService.getUserInfoOffline(user);
		computationalService.terminateComputational(userInfo, exploratoryName, computationalName);
	}

	private List<UserResourceInfo> getProjectEnv(ProjectDTO projectDTO, List<UserInstanceDTO> allInstances) {
		final Stream<UserResourceInfo> userResources = allInstances.stream()
				.filter(instance -> instance.getProject().equals(projectDTO.getName())).map(this::toUserResourceInfo);
		if (projectDTO.getEdgeInfo() != null) {
			UserResourceInfo edgeResource = new UserResourceInfo().withResourceType(ResourceEnum.EDGE_NODE)
					.withResourceStatus(ProjectDTO.Status.from(projectDTO.getStatus()).toString())
					.withProject(projectDTO.getName())
					.withIp(projectDTO.getEdgeInfo().getPublicIp());
			return Stream.concat(Stream.of(edgeResource), userResources)
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
				.withProject(userInstance.getProject());
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
