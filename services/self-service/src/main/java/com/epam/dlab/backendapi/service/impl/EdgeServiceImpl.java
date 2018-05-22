package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import static com.epam.dlab.UserInstanceStatus.*;
import static com.epam.dlab.rest.contracts.EdgeAPI.*;

@Singleton
@Slf4j
public class EdgeServiceImpl implements EdgeService {

	@Inject
	private KeyDAO keyDAO;

	@Inject
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;

	@Inject
	private RequestBuilder requestBuilder;

	@Inject
	private RequestId requestId;


	@Override
	public String start(UserInfo userInfo) {
		log.debug("Starting EDGE node for user {}", userInfo.getName());
		UserInstanceStatus status = UserInstanceStatus.of(keyDAO.getEdgeStatus(userInfo.getName()));
		if (status == null || !status.in(STOPPED)) {
			log.error("Could not start EDGE node for user {} because the status of instance is {}",
					userInfo.getName(), status);
			throw new DlabException("Could not start EDGE node because the status of instance is " + status);
		}
		try {
			return action(userInfo, EDGE_START, STARTING);
		} catch (DlabException e) {
			log.error("Could not start EDGE node for user {}", userInfo.getName(), e);
			throw new DlabException("Could not start EDGE node: " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public String stop(UserInfo userInfo) {
		log.debug("Stopping EDGE node for user {}", userInfo.getName());
		UserInstanceStatus status = UserInstanceStatus.of(keyDAO.getEdgeStatus(userInfo.getName()));
		if (status == null || !status.in(RUNNING)) {
			log.error("Could not stop EDGE node for user {} because the status of instance is {}",
					userInfo.getName(), status);
			throw new DlabException("Could not stop EDGE node because the status of instance is " + status);
		}

		try {
			return action(userInfo, EDGE_STOP, STOPPING);
		} catch (DlabException e) {
			log.error("Could not stop EDGE node for user {}", userInfo.getName(), e);
			throw new DlabException("Could not stop EDGE node: " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public String terminate(UserInfo userInfo) {
		log.debug("Terminating EDGE node for user {}", userInfo.getName());
		UserInstanceStatus status = UserInstanceStatus.of(keyDAO.getEdgeStatus(userInfo.getName()));
		if (status == null) {
			log.error("Could not terminate EDGE node for user {} because the status of instance is null",
					userInfo.getName());
			throw new DlabException("Could not terminate EDGE node because the status of instance is null");
		}

		try {
			return action(userInfo, EDGE_TERMINATE, TERMINATING);
		} catch (DlabException e) {
			log.error("Could not terminate EDGE node for user {}", userInfo.getName(), e);
			throw new DlabException("Could not terminate EDGE node: " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Updates parameter 'reuploadKeyRequired' for user's edge node with allowable statuses.
	 *
	 * @param user                user.
	 * @param reuploadKeyRequired true/false.
	 * @param edgeStatuses        allowable statuses of edge node.
	 */
	@Override
	public void updateReuploadKeyFlag(String user, boolean reuploadKeyRequired, UserInstanceStatus... edgeStatuses) {
		keyDAO.updateEdgeReuploadKey(user, reuploadKeyRequired, edgeStatuses);
	}

	/**
	 * Sends the post request to the provisioning service and update the status of EDGE node.
	 *
	 * @param userInfo user info.
	 * @param action   action for EDGE node.
	 * @param status   status of EDGE node.
	 * @return Request Id.
	 */
	private String action(UserInfo userInfo, String action, UserInstanceStatus status) {
		try {
			keyDAO.updateEdgeStatus(userInfo.getName(), status.toString());
			ResourceSysBaseDTO<?> dto = requestBuilder.newEdgeAction(userInfo);
			String uuid = provisioningService.post(action, userInfo.getAccessToken(), dto, String.class);
			requestId.put(userInfo.getName(), uuid);
			return uuid;
		} catch (Exception t) {
			keyDAO.updateEdgeStatus(userInfo.getName(), FAILED.toString());
			throw new DlabException("Could not " + action + " EDGE node " + ": " + t.getLocalizedMessage(), t);
		}
	}
}
