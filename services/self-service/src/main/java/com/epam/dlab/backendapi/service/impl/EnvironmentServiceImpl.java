package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EnvStatusDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Singleton
@Slf4j
public class EnvironmentServiceImpl implements EnvironmentService {

	@Inject
	private EnvStatusDAO envStatusDAO;
	@Inject
	private ExploratoryDAO exploratoryDAO;
	@Inject
	private ExploratoryService exploratoryService;
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
	public void stopEnvironment(String user) {
		log.debug("Stopping environment for user {}", user);
		exploratoryDAO.fetchRunningExploratoryFields(user)
				.forEach(this::stopNotebook);
		stopEdge(user);
	}

	@Override
	public void terminateEnvironment(String user) {
		log.debug("Terminating environment for user {}", user);
		if (!terminateEdge(user)) {
			exploratoryDAO.fetchNotTerminatedExploratoryFields(user).forEach(this::terminateNotebook);
		}
	}

	private boolean terminateEdge(String user) {
		final boolean nodeExists = keyDAO.edgeNodeExist(user);
		if (nodeExists) {
			edgeService.terminate(systemUserInfoService.create(user));
		}
		return nodeExists;
	}

	private void stopEdge(String user) {
		if (UserInstanceStatus.RUNNING.toString().equals(keyDAO.getEdgeStatus(user))) {
			edgeService.stop(systemUserInfoService.create(user));
		}
	}

	private void stopNotebook(UserInstanceDTO instance) {
		final UserInfo userInfo = systemUserInfoService.create(instance.getUser());
		exploratoryService.stop(userInfo, instance.getExploratoryName());
	}

	private void terminateNotebook(UserInstanceDTO instance) {
		final UserInfo userInfo = systemUserInfoService.create(instance.getUser());
		exploratoryService.terminate(userInfo, instance.getExploratoryName());
	}
}
