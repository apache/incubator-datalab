/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EnvStatusDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Singleton
@Slf4j
public class EnvironmentServiceImpl implements EnvironmentService {

	private static final String ERROR_MSG_FORMAT = "Can not %s environment because on of user resource is in status " +
			"CREATING or STARTING";
	@Inject
	private EnvStatusDAO envStatusDAO;
	@Inject
	private ExploratoryDAO exploratoryDAO;
	@Inject
	private ExploratoryService exploratoryService;
	@Inject
	private ComputationalService computationalService;
	@Inject
	private SystemUserInfoService systemUserInfoService;
	@Inject
	private KeyDAO keyDAO;
	@Inject
	private EdgeService edgeService;

	@Override
	public Set<String> getActiveUsers() {
		log.debug("Getting users that have at least 1 running instance");
		return envStatusDAO.fetchActiveEnvUsers();
	}

	@Override
	public Set<String> getAllUsers() {
		log.debug("Getting all users...");
		return envStatusDAO.fetchAllUsers();
	}

	@Override
	public void stopAll() {
		log.debug("Stopping environment for all users...");
		getAllUsers().forEach(this::stopEnvironment);
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
	public void stopEdge(String user) {
		if (UserInstanceStatus.RUNNING.toString().equals(keyDAO.getEdgeStatus(user))) {
			edgeService.stop(systemUserInfoService.create(user));
		}
	}

	@Override
	public void stopExploratory(String user, String exploratoryName) {
		stopNotebook(exploratoryDAO.fetchExploratoryFields(user, exploratoryName));
	}

	@Override
	public void stopComputational(String user, String exploratoryName, String computationalName) {
		stopSparkCluster(user, exploratoryName, computationalName);
	}

	@Override
	public void terminateAll() {
		log.debug("Terminating environment for all users...");
		getAllUsers().forEach(this::terminateEnvironment);
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
		terminateNotebook(exploratoryDAO.fetchExploratoryFields(user, exploratoryName));
	}

	@Override
	public void terminateComputational(String user, String exploratoryName, String computationalName) {
		terminateCluster(user, exploratoryName, computationalName);
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

	private boolean terminateEdge(String user) {
		final boolean nodeExists = keyDAO.edgeNodeExist(user);
		if (nodeExists) {
			edgeService.terminate(systemUserInfoService.create(user));
			exploratoryService.updateExploratoryStatuses(user, UserInstanceStatus.TERMINATING);
		}
		return nodeExists;
	}

	private void stopNotebook(UserInstanceDTO instance) {
		final UserInfo userInfo = systemUserInfoService.create(instance.getUser());
		exploratoryService.stop(userInfo, instance.getExploratoryName());
	}

	private void stopSparkCluster(String user, String exploratoryName, String computationalName) {
		final UserInfo userInfo = systemUserInfoService.create(user);
		computationalService.stopSparkCluster(userInfo, exploratoryName, computationalName);
	}

	private void terminateNotebook(UserInstanceDTO instance) {
		final UserInfo userInfo = systemUserInfoService.create(instance.getUser());
		exploratoryService.terminate(userInfo, instance.getExploratoryName());
	}

	private void terminateCluster(String user, String exploratoryName, String computationalName) {
		final UserInfo userInfo = systemUserInfoService.create(user);
		computationalService.terminateComputationalEnvironment(userInfo, exploratoryName, computationalName);
	}
}
