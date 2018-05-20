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
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

	private static final String REUPLOAD_KEY_UPDATE_MSG = "Reuploading key process is successfully finished. " +
			"Updating" +
			" 'reupload_key_required' flag to 'false' {}.";
	private static final String REUPLOAD_KEY_ERROR_MSG = "Reuploading key process is failed {}. The next attempt " +
			"starts after resource restarting.";


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
	public void waitForRunningStatusAndReuploadKey(UserInfo userInfo, ResourceData resourceData, long seconds) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(() -> {
			waitForRunningStatus(userInfo, resourceData, seconds);
			reuploadKeyAction(userInfo, resourceData);
		});
		executor.shutdown();
	}

	@Override
	public void processReuploadKeyResponse(ReuploadKeyStatusDTO dto) {
		String user = dto.getUser();
		ResourceData resource = dto.getReuploadKeyDTO().getResources().get(0);
		updateResourceStatus(user, resource, RUNNING);
		if (dto.getReuploadKeyStatus() == ReuploadKeyStatus.COMPLETED) {
			log.debug(REUPLOAD_KEY_UPDATE_MSG, messageAppendix(user, resource));
			updateResourceReuploadKeyFlag(user, resource, false);
		} else {
			log.error(REUPLOAD_KEY_ERROR_MSG, messageAppendix(user, resource));
		}
	}

	private String messageAppendix(String user, ResourceData resourceData) {
		if (resourceData.getResourceType() == ResourceType.EDGE) {
			return String.format("for edge with id %s for user %s", resourceData.getResourceId(), user);
		} else if (resourceData.getResourceType() == ResourceType.EXPLORATORY) {
			return String.format("for notebook %s for user %s", resourceData.getExploratoryName(), user);
		} else if (resourceData.getResourceType() == ResourceType.COMPUTATIONAL) {
			return String.format("for cluster %s of notebook %s for user %s",
					resourceData.getComputationalName(), resourceData.getExploratoryName(), user);
		} else return StringUtils.EMPTY;
	}

	private void waitForRunningStatus(UserInfo userInfo, ResourceData resourceData, long seconds) {
		while (fetchResourceStatus(userInfo.getName(), resourceData) != RUNNING) {
			pause(seconds);
		}
	}

	private void reuploadKeyAction(UserInfo userInfo, ResourceData resourceData) {
		try {
			updateResourceStatus(userInfo.getName(), resourceData, REUPLOADING_KEY);
			ReuploadKeyDTO reuploadKeyDTO = requestBuilder.newKeyReupload(userInfo, UUID.randomUUID().toString(),
					StringUtils.EMPTY, Collections.singletonList(resourceData));
			String uuid = provisioningService.post(REUPLOAD_KEY, userInfo.getAccessToken(), reuploadKeyDTO,
					String.class, "is_primary_reuploading", false);
			requestId.put(userInfo.getName(), uuid);
		} catch (Exception t) {
			log.error("Couldn't reupload key to " + resourceData.toString() + " for user {}", userInfo.getName(), t);
			updateResourceStatus(userInfo.getName(), resourceData, RUNNING);
			throw new DlabException("Couldn't reupload key to " + resourceData.toString() + " for user " +
					userInfo.getName() + ":	" + t.getLocalizedMessage(), t);
		}
	}

	private void pause(long seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			log.error("Interrupted exception occured: {}", e.getLocalizedMessage());
			Thread.currentThread().interrupt();
		}
	}

	private UserInstanceStatus fetchResourceStatus(String user, ResourceData resourceData) {
		if (resourceData.getResourceType() == ResourceType.EDGE) {
			return UserInstanceStatus.of(keyDAO.getEdgeStatus(user));
		} else if (resourceData.getResourceType() == ResourceType.EXPLORATORY) {
			return exploratoryDAO.fetchExploratoryStatus(user, resourceData.getExploratoryName());
		} else if (resourceData.getResourceType() == ResourceType.COMPUTATIONAL) {
			return UserInstanceStatus.of(computationalDAO.fetchComputationalFields(user,
					resourceData.getExploratoryName(), resourceData.getComputationalName()).getStatus());
		} else throw new DlabException("Unknown resource type: " + resourceData.getResourceType());
	}

	private void updateResourceStatus(String user, ResourceData resourceData, UserInstanceStatus newStatus) {
		if (resourceData.getResourceType() == ResourceType.EDGE) {
			keyDAO.updateEdgeStatus(user, newStatus.toString());
		} else if (resourceData.getResourceType() == ResourceType.EXPLORATORY) {
			exploratoryDAO.updateStatusForExploratory(user, resourceData.getExploratoryName(),
					newStatus);
		} else if (resourceData.getResourceType() == ResourceType.COMPUTATIONAL) {
			computationalDAO.updateStatusForComputationalResource(user, resourceData.getExploratoryName(),
					resourceData.getComputationalName(), newStatus);
		}
	}

	private void updateResourceReuploadKeyFlag(String user, ResourceData resourceData, boolean reuploadKeyRequired) {
		if (resourceData.getResourceType() == ResourceType.EDGE) {
			keyDAO.updateEdgeReuploadKey(user, reuploadKeyRequired, UserInstanceStatus.values());
		} else if (resourceData.getResourceType() == ResourceType.EXPLORATORY) {
			exploratoryDAO.updateReuploadKeyForExploratory(user, resourceData.getExploratoryName(),
					reuploadKeyRequired);
		} else if (resourceData.getResourceType() == ResourceType.COMPUTATIONAL) {
			computationalDAO.updateReuploadKeyFlagForComputationalResource(user, resourceData.getExploratoryName(),
					resourceData.getComputationalName(), reuploadKeyRequired);
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
