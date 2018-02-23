package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.AccessKeyService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.dto.base.keyload.UploadFile;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UserKeyDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import static com.epam.dlab.UserInstanceStatus.FAILED;
import static com.epam.dlab.UserInstanceStatus.TERMINATED;
import static com.epam.dlab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;
import static com.epam.dlab.rest.contracts.EdgeAPI.EDGE_CREATE;

@Singleton
@Slf4j
public class AccessKeyServiceImpl implements AccessKeyService {

	@Inject
	private KeyDAO keyDAO;

	@Inject
	@Named(PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;

	@Override
	public KeyLoadStatus getUserKeyStatus(String user) {
		log.debug("Check the status of the user key for {}", user);
		try {
			return keyDAO.findKeyStatus(user);
		} catch (DlabException e) {
			log.error("Check the status of the user key for {} fails", user, e);
			return KeyLoadStatus.ERROR;
		}
	}

	@Override
	public String uploadKey(UserInfo user, String keyContent) {
		log.debug("The upload of the user key and creation EDGE node will be started for user {}", user);
		keyDAO.insertKey(user.getName(), keyContent);
		try {
			return createEdge(user, keyContent);
		} catch (Exception e) {
			log.error("The upload of the user key and create EDGE node for user {} fails", user.getName(), e);
			keyDAO.deleteKey(user.getName());
			throw new DlabException("Could not upload the key and create EDGE node: " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public String recoverEdge(UserInfo userInfo) {
		log.debug("Recreating edge node for user {}", userInfo.getName());
		try {
			String userName = userInfo.getName();
			EdgeInfo edgeInfo = getEdgeInfo(userName);
			UserKeyDTO key = keyDAO.fetchKey(userName, KeyLoadStatus.SUCCESS);
			updateEdgeStatusToCreating(userName, edgeInfo);
			return uploadKey(userInfo, key.getContent());
		} catch (Exception e) {
			log.error("Could not create the EDGE node for user {}", userInfo.getName(), e);
			keyDAO.updateEdgeStatus(userInfo.getName(), UserInstanceStatus.FAILED.toString());
			throw new DlabException("Could not upload the key and create EDGE node: " + e.getLocalizedMessage(), e);
		}
	}

	private EdgeInfo getEdgeInfo(String userName) {
		EdgeInfo edgeInfo = keyDAO.getEdgeInfo(userName);
		UserInstanceStatus status = UserInstanceStatus.of(edgeInfo.getEdgeStatus());
		if (status == null || !status.in(FAILED, TERMINATED)) {
			log.error("Could not create EDGE node for user {} because the status of instance is {}", userName,
					status);
			throw new DlabException("Could not create EDGE node because the status of instance is " + status);
		}
		return edgeInfo;
	}

	private void updateEdgeStatusToCreating(String userName, EdgeInfo edgeInfo) {
		edgeInfo.setInstanceId(null);
		edgeInfo.setEdgeStatus(UserInstanceStatus.CREATING.toString());
		try {
			keyDAO.updateEdgeInfo(userName, edgeInfo);
		} catch (DlabException e) {
			log.error("Could not update the status of EDGE node for user {}", userName, e);
			throw new DlabException("Could not create EDGE node: " + e.getLocalizedMessage(), e);
		}
	}

	private String createEdge(UserInfo user, String keyContent) {

		UploadFile uploadFile = RequestBuilder.newEdgeKeyUpload(user, keyContent);
		String uuid = provisioningService.post(EDGE_CREATE, user.getAccessToken(), uploadFile, String.class);
		RequestId.put(user.getName(), uuid);
		return uuid;
	}
}
