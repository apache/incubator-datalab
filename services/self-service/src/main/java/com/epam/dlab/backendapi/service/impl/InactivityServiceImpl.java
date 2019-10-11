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
package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.InactivityService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.computational.ComputationalCheckInactivityDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.ExploratoryCheckInactivityAction;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.InfrasctructureAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public class InactivityServiceImpl implements InactivityService {
	@Inject
	private ExploratoryDAO exploratoryDAO;
	@Inject
	private ComputationalDAO computationalDAO;
	@Inject
	private EnvDAO envDAO;
	@Inject
	private RequestBuilder requestBuilder;
	@Inject
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;
	@Inject
	private RequestId requestId;
	@Inject
	private ComputationalService computationalService;
	@Inject
	private ExploratoryService exploratoryService;
	@Inject
	private SecurityService securityService;

	@Override
	public void updateRunningResourcesLastActivity() {
		envDAO.findRunningResourcesForCheckInactivity()
				.forEach(this::updateLastActivity);
	}

	@Override
	public void updateLastActivityForExploratory(UserInfo userInfo, String exploratoryName,
												 LocalDateTime lastActivity) {
		exploratoryDAO.updateLastActivity(userInfo.getName(), exploratoryName, lastActivity);
	}

	@Override
	public void updateLastActivityForComputational(UserInfo userInfo, String exploratoryName,
												   String computationalName, LocalDateTime lastActivity) {
		computationalDAO.updateLastActivity(userInfo.getName(), exploratoryName, computationalName, lastActivity);
	}

	private void updateLastActivity(UserInstanceDTO ui) {
		if (UserInstanceStatus.RUNNING.toString().equals(ui.getStatus())) {
			updateExploratoryLastActivity(securityService.getUserInfoOffline(ui.getUser()), ui);
		}
		ui.getResources()
				.stream()
				.filter(comp -> UserInstanceStatus.RUNNING.toString().equals(comp.getStatus()))
				.forEach(cr -> updateComputationalLastActivity(securityService.getUserInfoOffline(ui.getUser()), ui, cr));
	}

	private void updateComputationalLastActivity(UserInfo userInfo, UserInstanceDTO ui, UserComputationalResource cr) {
		final ComputationalCheckInactivityDTO dto =
				requestBuilder.newComputationalCheckInactivity(userInfo, ui, cr);
		final String uuid =
				provisioningService.post(InfrasctructureAPI.COMPUTATIONAL_CHECK_INACTIVITY,
						userInfo.getAccessToken(), dto, String.class);
		requestId.put(userInfo.getName(), uuid);
	}

	private void updateExploratoryLastActivity(UserInfo userInfo, UserInstanceDTO ui) {
		final ExploratoryCheckInactivityAction dto =
				requestBuilder.newExploratoryCheckInactivityAction(userInfo, ui);
		final String uuid =
				provisioningService.post(InfrasctructureAPI.EXPLORATORY_CHECK_INACTIVITY,
						userInfo.getAccessToken(), dto, String.class);
		requestId.put(userInfo.getName(), uuid);
	}
}
