package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.AccessKeyService;
import com.epam.dlab.backendapi.service.ExploratoryService;
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
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.epam.dlab.UserInstanceStatus.FAILED;
import static com.epam.dlab.UserInstanceStatus.TERMINATED;
import static com.epam.dlab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;
import static com.epam.dlab.rest.contracts.EdgeAPI.EDGE_CREATE;
import static com.epam.dlab.rest.contracts.KeyAPI.REUPLOAD_KEY;

@Singleton
@Slf4j
public class AccessKeyServiceImpl implements AccessKeyService {

	@Inject
	private KeyDAO keyDAO;
	@Inject
	@Named(PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;
	@Inject
	private RequestBuilder requestBuilder;
	@Inject
	private RequestId requestId;
	@Inject
	private ExploratoryService exploratoryService;
	@Inject
	private SelfServiceApplicationConfiguration configuration;

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
	public String loadKey(UserInfo user, String keyContent, boolean isPrimaryUploading) {
		return isPrimaryUploading ? uploadKey(user, keyContent) : reUploadKey(user, keyContent);
	}

	@Override
	public String recoverEdge(UserInfo userInfo) {
		log.debug("Recreating edge node for user {}", userInfo.getName());
		try {
			String userName = userInfo.getName();
			EdgeInfo edgeInfo = getEdgeInfo(userName);
			UserKeyDTO key = keyDAO.fetchKey(userName, KeyLoadStatus.SUCCESS);
			updateEdgeStatusToCreating(userName, edgeInfo);
			return createEdge(userInfo, key.getContent());
		} catch (Exception e) {
			log.error("Could not create the EDGE node for user {}", userInfo.getName(), e);
			keyDAO.updateEdgeStatus(userInfo.getName(), UserInstanceStatus.FAILED.toString());
			throw new DlabException("Could not upload the key and create EDGE node: " + e.getLocalizedMessage(), e);
		}
	}

	@Override
	public String generateKey(UserInfo userInfo) {
		log.debug("Generating new key pair for user {}", userInfo.getName());
		try (ByteArrayOutputStream publicKeyOut = new ByteArrayOutputStream();
			 ByteArrayOutputStream privateKeyOut = new ByteArrayOutputStream()) {
			KeyPair pair = KeyPair.genKeyPair(new JSch(), com.jcraft.jsch.KeyPair.RSA);
			pair.writePublicKey(publicKeyOut, userInfo.getName());
			pair.writePrivateKey(privateKeyOut);
			uploadKey(userInfo, new String(publicKeyOut.toByteArray()));
			return new String(privateKeyOut.toByteArray());
		} catch (JSchException | IOException e) {
			log.error("Can not generate private/public key pair due to: {}", e.getMessage());
			throw new DlabException("Can not generate private/public key pair due to: " + e.getMessage(), e);
		}
	}

	private String uploadKey(UserInfo user, String keyContent) {
		log.debug("The key uploading and EDGE node creating for user {} is starting...", user);
		keyDAO.insertKey(user.getName(), keyContent);
		exploratoryService.setReuploadKeyRequiredForCorrespondingExploratoriesAndComputationals(user.getName());
		try {
			return createEdge(user, keyContent);
		} catch (Exception e) {
			log.error("The key uploading and EDGE node creating for user {} fails", user.getName(), e);
			keyDAO.deleteKey(user.getName());
			throw new DlabException("Could not upload the key and create EDGE node: " + e.getLocalizedMessage(), e);
		}
	}

	private String reUploadKey(UserInfo user, String keyContent) {
		log.debug("The key reuploading for user {} is starting...", user);
		keyDAO.deleteKey(user.getName());
		keyDAO.insertKey(user.getName(), keyContent);
		keyDAO.updateKey(user.getName(), KeyLoadStatus.SUCCESS.getStatus());
		exploratoryService.setReuploadKeyRequiredForCorrespondingExploratoriesAndComputationals(user.getName());
		try {
			UploadFile uploadFile = requestBuilder.newKeyReupload(user, keyContent);
			String uuid = provisioningService.post(REUPLOAD_KEY, user.getAccessToken(), uploadFile, String.class);
			requestId.put(user.getName(), uuid);
			return uuid;
		} catch (Exception e) {
			log.error("The key reuploading for user {} fails", user.getName(), e);
			keyDAO.deleteKey(user.getName());
			exploratoryService.cancelReuploadKeyRequirementForAllUserInstances(user.getName());
			throw new DlabException("Could not reupload the key. Previous key has been deleted: " +
					e.getLocalizedMessage(), e);
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
		UploadFile uploadFile = requestBuilder.newEdgeKeyUpload(user, keyContent);
		String uuid = provisioningService.post(EDGE_CREATE, user.getAccessToken(), uploadFile, String.class);
		requestId.put(user.getName(), uuid);
		return uuid;
	}
}
