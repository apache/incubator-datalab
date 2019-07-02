/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ReuploadKeyService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.ResourceData;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;

import static com.epam.dlab.dto.UserInstanceStatus.*;


@Path("/infrastructure_provision/exploratory_environment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ExploratoryCallback {

	private static final String USER_INSTANCE_NOT_EXIST_MSG = "User instance with exploratory name %s for user %s " +
			"doesn't exist";
	@Inject
	private ExploratoryDAO exploratoryDAO;
	@Inject
	private ComputationalDAO computationalDAO;
	@Inject
	private RequestId requestId;
	@Inject
	private SecurityService securityService;
	@Inject
	private ReuploadKeyService reuploadKeyService;
	@Inject
	private ExploratoryService exploratoryService;

	/**
	 * Changes the status of exploratory environment.
	 *
	 * @param dto description of status.
	 * @return 200 OK - if request success.
	 */
	@POST
	@Path(ApiCallbacks.STATUS_URI)
	public Response status(ExploratoryStatusDTO dto) {
		log.debug("Updating status for exploratory environment {} for user {} to {}",
				dto.getExploratoryName(), dto.getUser(), dto.getStatus());
		requestId.checkAndRemove(dto.getRequestId());

		UserInstanceDTO instance = exploratoryService.getUserInstance(dto.getUser(), dto.getExploratoryName())
				.orElseThrow(() -> new DlabException(String.format(USER_INSTANCE_NOT_EXIST_MSG,
						dto.getExploratoryName(), dto.getUser())));

		UserInstanceStatus currentStatus = UserInstanceStatus.of(instance.getStatus());
		log.debug("Current status for exploratory environment {} for user {} is {}",
				dto.getExploratoryName(), dto.getUser(), currentStatus);

		try {
			exploratoryDAO.updateExploratoryFields(dto.withLastActivity(new Date()));
			if (currentStatus == TERMINATING) {
				updateComputationalStatuses(dto.getUser(), dto.getExploratoryName(),
						UserInstanceStatus.of(dto.getStatus()));
			} else if (currentStatus == STOPPING) {
				updateComputationalStatuses(dto.getUser(), dto.getExploratoryName(),
						UserInstanceStatus.of(dto.getStatus()), TERMINATED, FAILED, TERMINATED, STOPPED);
			}
		} catch (DlabException e) {
			log.error("Could not update status for exploratory environment {} for user {} to {}",
					dto.getExploratoryName(), dto.getUser(), dto.getStatus(), e);
			throw new DlabException("Could not update status for exploratory environment " + dto.getExploratoryName() +
					" for user " + dto.getUser() + " to " + dto.getStatus() + ": " + e.getLocalizedMessage(), e);
		}
		if (UserInstanceStatus.of(dto.getStatus()) == RUNNING && instance.isReuploadKeyRequired()) {
			ResourceData resourceData =
					ResourceData.exploratoryResource(dto.getExploratoryId(), dto.getExploratoryName());
			UserInfo userInfo = securityService.getUserInfoOffline(dto.getUser());
			reuploadKeyService.reuploadKeyAction(userInfo, resourceData);
		}
		return Response.ok().build();
	}

	/**
	 * Updates the computational status of exploratory environment.
	 *
	 * @param user            user name
	 * @param exploratoryName name of exploratory environment.
	 * @param status          status for exploratory environment.
	 */
	private void updateComputationalStatuses(String user, String exploratoryName, UserInstanceStatus status) {
		log.debug("updating status for all computational resources of {} for user {}: {}", exploratoryName, user,
				status);
		computationalDAO.updateComputationalStatusesForExploratory(new ExploratoryStatusDTO()
				.withUser(user)
				.withExploratoryName(exploratoryName)
				.withStatus(status));
	}

	private void updateComputationalStatuses(String user, String exploratoryName, UserInstanceStatus
			dataEngineStatus, UserInstanceStatus dataEngineServiceStatus, UserInstanceStatus... excludedStatuses) {
		log.debug("updating status for all computational resources of {} for user {}: DataEngine {}, " +
				"dataengine-service {}", exploratoryName, user, dataEngineStatus, dataEngineServiceStatus);
		computationalDAO.updateComputationalStatusesForExploratory(user, exploratoryName, dataEngineStatus,
				dataEngineServiceStatus, excludedStatuses);
	}
}
