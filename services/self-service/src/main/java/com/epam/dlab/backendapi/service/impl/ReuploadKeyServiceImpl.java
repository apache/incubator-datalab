package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ReuploadKeyService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.base.DataEngineType;
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
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.epam.dlab.UserInstanceStatus.*;
import static com.epam.dlab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;
import static com.epam.dlab.rest.contracts.KeyAPI.REUPLOAD_KEY;

@Singleton
@Slf4j
public class ReuploadKeyServiceImpl implements ReuploadKeyService {

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
	private ComputationalDAO computationalDAO;
	@Inject
	private ExploratoryDAO exploratoryDAO;

	private static final String REUPLOAD_KEY_UPDATE_MSG = "Updating 'reupload_key_required' flag to 'false'";


	@Override
	public String reuploadKey(UserInfo user, String keyContent) {
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

	@Override
	public void reuploadKeyAction(UserInfo userInfo, ResourceData resourceData) {
		try {
			updateResourceStatus(userInfo, resourceData, REUPLOADING_KEY);
			ReuploadKeyDTO reuploadKeyDTO = requestBuilder.newKeyReupload(userInfo, UUID.randomUUID().toString(), null,
					Collections.singletonList(resourceData));
			String uuid = provisioningService.post(REUPLOAD_KEY + "?is_primary_reuploading=false",
					userInfo.getAccessToken(), reuploadKeyDTO, String.class);
			requestId.put(userInfo.getName(), uuid);
		} catch (Exception t) {
			log.error("Couldn't reupload key to " + resourceData.toString() + " for user {}", userInfo.getName(), t);
			updateResourceStatus(userInfo, resourceData, RUNNING);
			throw new DlabException("Couldn't reupload key to " + resourceData.toString() + " for user " +
					userInfo.getName() + ":	" + t.getLocalizedMessage(), t);
		}
	}

	@Override
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

	private void updateResourceStatus(UserInfo userInfo, ResourceData resourceData, UserInstanceStatus newStatus) {
		if (resourceData.getResourceType() == ResourceType.EDGE) {
			keyDAO.updateEdgeStatus(userInfo.getName(), newStatus.toString());
		} else if (resourceData.getResourceType() == ResourceType.EXPLORATORY) {
			exploratoryDAO.updateStatusForExploratory(userInfo.getName(), resourceData.getExploratoryName(),
					newStatus);
		} else if (resourceData.getResourceType() == ResourceType.COMPUTATIONAL) {
			computationalDAO.updateStatusForComputationalResource(userInfo.getName(),
					resourceData.getExploratoryName(), resourceData.getComputationalName(), newStatus);
		}
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

}
