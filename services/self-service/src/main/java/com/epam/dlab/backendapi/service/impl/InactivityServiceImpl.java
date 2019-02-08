package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.InactivityService;
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
	private SystemUserInfoService systemUserInfoService;

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
			updateExploratoryLastActivity(systemUserInfoService.create(ui.getUser()), ui);
		}
		ui.getResources()
				.stream()
				.filter(comp -> UserInstanceStatus.RUNNING.toString().equals(comp.getStatus()))
				.forEach(cr -> updateComputationalLastActivity(systemUserInfoService.create(ui.getUser()), ui, cr));
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
