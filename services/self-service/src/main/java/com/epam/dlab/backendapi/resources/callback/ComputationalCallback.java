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
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.ReuploadKeyService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
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

import static com.epam.dlab.dto.UserInstanceStatus.RUNNING;

@Path("/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ComputationalCallback {

	@Inject
	private ComputationalDAO computationalDAO;
	@Inject
	private RequestId requestId;
	@Inject
	private SecurityService securityService;
	@Inject
	private ReuploadKeyService reuploadKeyService;
	@Inject
	private ComputationalService computationalService;

	/**
	 * Updates the status of the computational resource for user.
	 *
	 * @param dto DTO info about the status of the computational resource.
	 * @return 200 OK - if request success otherwise throws exception.
	 */
	@POST
	@Path(ApiCallbacks.STATUS_URI)
	public Response status(ComputationalStatusDTO dto) {
		log.debug("Updating status for computational resource {} for user {}: {}",
				dto.getComputationalName(), dto.getUser(), dto);
		String uuid = dto.getRequestId();
		requestId.checkAndRemove(uuid);

		UserComputationalResource compResource = computationalService.getComputationalResource(dto.getUser(),
				dto.getExploratoryName(), dto.getComputationalName()).orElseThrow(() ->
				new DlabException("Computational resource " + dto.getComputationalName() +
						" of exploratory environment " + dto.getExploratoryName() + " for user " + dto.getUser() +
						" doesn't exist"));
		log.debug("Current status for computational resource {} of exploratory environment {} for user {} is {}",
				dto.getComputationalName(), dto.getExploratoryName(), dto.getUser(),
				compResource.getStatus());
		try {
			computationalDAO.updateComputationalFields(dto.withLastActivity(new Date()));
		} catch (DlabException e) {
			log.error("Could not update status for computational resource {} for user {} to {}: {}", dto, e);
			throw e;
		}
		if (UserInstanceStatus.CONFIGURING == UserInstanceStatus.of(dto.getStatus())) {
			log.debug("Waiting for configuration of the computational resource {} for user {}",
					dto.getComputationalName(), dto.getUser());
			requestId.put(dto.getUser(), uuid);
		} else if (UserInstanceStatus.of(dto.getStatus()) == RUNNING && compResource.isReuploadKeyRequired()) {
			ResourceData resourceData = ResourceData.computationalResource(compResource.getComputationalId(),
					dto.getExploratoryName(), dto.getComputationalName());
			UserInfo userInfo = securityService.getUserInfoOffline(dto.getUser());
			reuploadKeyService.reuploadKeyAction(userInfo, resourceData);
		}
		return Response.ok().build();
	}
}
