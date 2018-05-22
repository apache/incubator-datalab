/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
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

package com.epam.dlab.backendapi.resources.callback.base;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ReuploadKeyService;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.ResourceData;
import com.epam.dlab.model.ResourceType;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import static com.epam.dlab.UserInstanceStatus.RUNNING;
import static com.epam.dlab.UserInstanceStatus.STARTING;

@Slf4j
public class EdgeCallback {
	@Inject
	private KeyDAO keyDAO;
	@Inject
	private ExploratoryService exploratoryService;
	@Inject
	private SystemUserInfoService systemUserService;
	@Inject
	private ReuploadKeyService reuploadKeyService;

	protected EdgeCallback() {
		log.info("{} is initialized", getClass().getSimpleName());
	}

	protected void handleEdgeCallback(String user, String status) {
		EdgeInfo edgeInfo = keyDAO.getEdgeInfo(user);
		UserInstanceStatus currentStatus = UserInstanceStatus.of(edgeInfo.getEdgeStatus());
		log.debug("Current status of edge node for user {} is {}", user, currentStatus);

		try {
			if (UserInstanceStatus.of(status) == UserInstanceStatus.TERMINATED) {
				log.debug("Removing key for user {}", user);
				keyDAO.deleteKey(user);
			}
			log.debug("Updating the status of EDGE node for user {} to {}", user, status);
			keyDAO.updateEdgeStatus(user, status);

		} catch (DlabException e) {
			log.error("Could not update status of EDGE node for user {} to {}", user, status, e);
			throw new DlabException(String.format("Could not update status of EDGE node to %s: %s",
					status, e.getLocalizedMessage()), e);
		}
		if (currentStatus == STARTING && UserInstanceStatus.of(status) == RUNNING && edgeInfo.isReuploadKeyRequired
				()) {
			ResourceData resourceData = new ResourceData(ResourceType.EDGE,
					edgeInfo.getInstanceId(), null, null);
			UserInfo userInfo = systemUserService.create(user);
			reuploadKeyService.reuploadKeyAction(userInfo, resourceData);
		}
	}


}
