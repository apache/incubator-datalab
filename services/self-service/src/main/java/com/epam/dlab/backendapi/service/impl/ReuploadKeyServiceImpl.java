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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ReuploadKeyService;
import com.epam.dlab.backendapi.service.UserResourceService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.UserInstanceStatus;
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

import static com.epam.dlab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;
import static com.epam.dlab.dto.UserInstanceStatus.REUPLOADING_KEY;
import static com.epam.dlab.dto.UserInstanceStatus.RUNNING;
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
	private ComputationalDAO computationalDAO;
	@Inject
	private ExploratoryDAO exploratoryDAO;
	@Inject
	private UserResourceService userResourceService;

	private static final String REUPLOAD_KEY_UPDATE_MSG = "Reuploading key process is successfully finished. " +
			"Updating 'reupload_key_required' flag to 'false' for {}.";
	private static final String REUPLOAD_KEY_ERROR_MSG = "Reuploading key process is failed for {}. The next attempt" +
			"starts after resource restarting.";


	@Override
	public String reuploadKey(UserInfo user, String keyContent) {
		userResourceService.updateReuploadKeyFlagForUserResources(user.getName(), true);
		List<ResourceData> resourcesForKeyReuploading = userResourceService.convertToResourceData(
				exploratoryService.getInstancesWithStatuses(user.getName(), RUNNING, RUNNING));
		keyDAO.getEdgeInfoWhereStatusIn(user.getName(), RUNNING)
				.ifPresent(edgeInfo -> {
					resourcesForKeyReuploading.add(ResourceData.edgeResource(edgeInfo.getInstanceId()));
					keyDAO.updateEdgeStatus(user.getName(), REUPLOADING_KEY.toString());
				});
		updateStatusForUserInstances(user.getName(), REUPLOADING_KEY);

		ReuploadKeyDTO reuploadKeyDTO = requestBuilder.newKeyReupload(user, UUID.randomUUID().toString(), keyContent,
				resourcesForKeyReuploading);
		return provisioningService.post(REUPLOAD_KEY, user.getAccessToken(), reuploadKeyDTO, String.class);
	}

	@Override
	public void updateResourceData(ReuploadKeyStatusDTO dto) {
		String user = dto.getUser();
		ResourceData resource = dto.getReuploadKeyCallbackDTO().getResource();
		log.debug("Updating resource {} to status RUNNING...", resource.toString());
		updateResourceStatus(user, resource, RUNNING);
		if (dto.getReuploadKeyStatus() == ReuploadKeyStatus.COMPLETED) {
			log.debug(REUPLOAD_KEY_UPDATE_MSG, resource.toString());
			updateResourceReuploadKeyFlag(user, resource, false);
		} else {
			log.error(REUPLOAD_KEY_ERROR_MSG, resource.toString());
		}
	}

	@Override
	public void reuploadKeyAction(UserInfo userInfo, ResourceData resourceData) {
		try {
			updateResourceStatus(userInfo.getName(), resourceData, REUPLOADING_KEY);
			ReuploadKeyDTO reuploadKeyDTO = requestBuilder.newKeyReupload(userInfo, UUID.randomUUID().toString(),
					StringUtils.EMPTY, Collections.singletonList(resourceData));
			String uuid = provisioningService.post(REUPLOAD_KEY, userInfo.getAccessToken(), reuploadKeyDTO,
					String.class, Collections.singletonMap("is_primary_reuploading", false));
			requestId.put(userInfo.getName(), uuid);
		} catch (Exception t) {
			log.error("Couldn't reupload key to " + resourceData.toString() + " for user {}", userInfo.getName(), t);
			updateResourceStatus(userInfo.getName(), resourceData, RUNNING);
			throw new DlabException("Couldn't reupload key to " + resourceData.toString() + " for user " +
					userInfo.getName() + ":	" + t.getLocalizedMessage(), t);
		}
	}

	private void updateResourceStatus(String user, ResourceData resourceData, UserInstanceStatus newStatus) {
		if (resourceData.getResourceType() == ResourceType.EDGE) {
			keyDAO.updateEdgeStatus(user, newStatus.toString());
		} else if (resourceData.getResourceType() == ResourceType.EXPLORATORY) {
			exploratoryDAO.updateStatusForExploratory(user, resourceData.getExploratoryName(), newStatus);
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

	private void updateStatusForUserInstances(String user, UserInstanceStatus newStatus) {
		exploratoryDAO.updateStatusForExploratories(newStatus, user, RUNNING);
		computationalDAO.updateStatusForComputationalResources(newStatus, user,
				Arrays.asList(RUNNING, REUPLOADING_KEY), Arrays.asList(DataEngineType.SPARK_STANDALONE,
						DataEngineType.CLOUD_SERVICE), RUNNING);
	}

}
