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
import com.epam.dlab.exceptions.ResourceConflictException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

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
		checkState(user, "stop");
		exploratoryDAO.fetchRunningExploratoryFields(user)
				.forEach(this::stopNotebook);
		stopEdge(user);
	}

	@Override
	public void terminateEnvironment(String user) {
		log.debug("Terminating environment for user {}", user);
		checkState(user, "terminate");
		if (!terminateEdge(user)) {
			exploratoryDAO.fetchUserExploratoriesWhereStatusIncludedOrExcluded(false, user,
					UserInstanceStatus.TERMINATED, UserInstanceStatus.FAILED, UserInstanceStatus.TERMINATING)
					.forEach(this::terminateNotebook);
		}
	}

	private void checkState(String user, String action) {
		final List<UserInstanceDTO> userInstances = exploratoryDAO
				.fetchUserExploratoriesWhereStatusIncludedOrExcluded(true, user, UserInstanceStatus.CREATING,
						UserInstanceStatus.STARTING);
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
