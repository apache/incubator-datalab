package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.AccessKeyService;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.dto.base.keyload.UploadFile;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UserKeyDTO;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyDTO;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyStatus;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.ResourceData;
import com.epam.dlab.model.ResourceType;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.epam.dlab.UserInstanceStatus.*;
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
	private ComputationalService computationalService;
	@Inject
	private EdgeService edgeService;
	@Inject
	private SelfServiceApplicationConfiguration configuration;
	@Inject
	private SettingsDAO settingsDAO;
	@Inject
	private ComputationalDAO computationalDAO;
	@Inject
	private ExploratoryDAO exploratoryDAO;

	private static final String REUPLOAD_KEY_UPDATE_MSG = "Updating 'reupload_key_required' flag to 'false'";

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
	public String uploadKey(UserInfo user, String keyContent, boolean isPrimaryUploading) {
		log.debug(isPrimaryUploading ? "The key uploading and EDGE node creating for user {} is starting..." :
				"The key reuploading for user {} is starting...", user);
		keyDAO.upsertKey(user.getName(), keyContent, isPrimaryUploading);
		try {
			return isPrimaryUploading ? createEdge(user, keyContent) : reuploadKey(user, keyContent);
		} catch (Exception e) {
			log.error(isPrimaryUploading ? "The key uploading and EDGE node creating for user {} fails" :
					"The key reuploading for user {} fails", user.getName(), e);
			keyDAO.deleteKey(user.getName());
			throw new DlabException(isPrimaryUploading ? "Could not upload the key and create EDGE node: " :
					"Could not reupload the key. Previous key has been deleted: " + e.getLocalizedMessage(), e);
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
			KeyPair pair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, configuration.getPrivateKeySize());
			pair.writePublicKey(publicKeyOut, userInfo.getName());
			pair.writePrivateKey(privateKeyOut);
			uploadKey(userInfo, new String(publicKeyOut.toByteArray()), true);
			return new String(privateKeyOut.toByteArray());
		} catch (JSchException | IOException e) {
			log.error("Can not generate private/public key pair due to: {}", e.getMessage());
			throw new DlabException("Can not generate private/public key pair due to: " + e.getMessage(), e);
		}
	}

	public void processReuploadKeyResponse(ReuploadKeyStatusDTO dto) {
		ResourceData resource = dto.getReuploadKeyDTO().getResources().get(0);
		String user = dto.getUser();
		if (resource.getResourceType() == ResourceType.EDGE) {
			keyDAO.updateEdgeStatus(user, UserInstanceStatus.RUNNING.toString());
			if (dto.getReuploadKeyStatus() == ReuploadKeyStatus.COMPLETED) {
				log.debug(REUPLOAD_KEY_UPDATE_MSG + " for edge with id {}...", resource.getResourceId());
				keyDAO.updateEdgeReuploadKey(user, false, UserInstanceStatus.values());
			}
		} else if (resource.getResourceType() == ResourceType.EXPLORATORY) {
			exploratoryDAO.updateStatusForExploratory(user, resource.getExploratoryName(), RUNNING);
			if (dto.getReuploadKeyStatus() == ReuploadKeyStatus.COMPLETED) {
				log.debug(REUPLOAD_KEY_UPDATE_MSG + " for notebook {}...", resource.getExploratoryName());
				exploratoryDAO.updateReuploadKeyForExploratory(user, resource.getExploratoryName(), false);
			}
		} else if (resource.getResourceType() == ResourceType.COMPUTATIONAL) {
			computationalDAO.updateStatusForComputationalResource(user, resource.getExploratoryName(),
					resource.getComputationalName(), RUNNING);
			if (dto.getReuploadKeyStatus() == ReuploadKeyStatus.COMPLETED) {
				log.debug(REUPLOAD_KEY_UPDATE_MSG + " for cluster {} of notebook {}...",
						resource.getComputationalName(), resource.getExploratoryName());
				computationalDAO.updateReuploadKeyFlagForComputationalResource(user, resource.getExploratoryName(),
						resource.getComputationalName(), false);
			}
		}
	}

	private String reuploadKey(UserInfo user, String keyContent) {
		updateReuploadKeyForUserResources(user.getName(), true);
		List<ResourceData> resourcesForKeyReuploading = exploratoryService
				.getResourcesWithPredefinedStatuses(user.getName(), RUNNING, RUNNING);
		if (RUNNING == UserInstanceStatus.of(keyDAO.getEdgeStatus(user.getName()))) {
			resourcesForKeyReuploading.add(new ResourceData(ResourceType.EDGE, keyDAO.getEdgeInfo(user.getName())
					.getInstanceId(), null, null));
			keyDAO.updateEdgeStatus(user.getName(), UserInstanceStatus.REUPLOADING_KEY.toString());
		}
		updateStatusForUserResources(user.getName(), REUPLOADING_KEY);

		ReuploadKeyDTO reuploadKeyDTO = requestBuilder.newKeyReupload(user, UUID.randomUUID().toString(), keyContent,
				resourcesForKeyReuploading);
		String uuid = provisioningService.post(REUPLOAD_KEY, user.getAccessToken(), reuploadKeyDTO, String.class);
		requestId.put(user.getName(), uuid);
		return uuid;
	}

	private void updateReuploadKeyForUserResources(String user, boolean reuploadKeyRequired) {
		exploratoryService.updateExploratoriesReuploadKeyFlag(user, reuploadKeyRequired,
				CREATING, CONFIGURING, STARTING, RUNNING, STOPPING, STOPPED);
		computationalService.updateComputationalsReuploadKeyFlag(user,
				Arrays.asList(STARTING, RUNNING, STOPPING, STOPPED),
				Collections.singletonList(DataEngineType.SPARK_STANDALONE),
				reuploadKeyRequired,
				CREATING, CONFIGURING, STARTING, RUNNING, STOPPING, STOPPED);
		computationalService.updateComputationalsReuploadKeyFlag(user,
				Collections.singletonList(RUNNING),
				Collections.singletonList(DataEngineType.CLOUD_SERVICE),
				reuploadKeyRequired,
				CREATING, CONFIGURING, STARTING, RUNNING);
		edgeService.updateReuploadKeyFlag(user, reuploadKeyRequired, STARTING, RUNNING, STOPPING, STOPPED);
	}

	private void updateStatusForUserResources(String user, UserInstanceStatus newStatus) {
		exploratoryDAO.updateStatusForExploratories(newStatus, user, RUNNING);
		computationalDAO.updateStatusForComputationalResources(newStatus, user,
				Arrays.asList(RUNNING, REUPLOADING_KEY), Arrays.asList(DataEngineType.SPARK_STANDALONE,
						DataEngineType.CLOUD_SERVICE), RUNNING);
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
