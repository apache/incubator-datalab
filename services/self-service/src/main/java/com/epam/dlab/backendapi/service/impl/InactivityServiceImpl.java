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
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.InfrasctructureAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.epam.dlab.dto.UserInstanceStatus.RUNNING;
import static com.epam.dlab.dto.base.DataEngineType.CLOUD_SERVICE;
import static com.epam.dlab.dto.base.DataEngineType.SPARK_STANDALONE;

@Slf4j
public class InactivityServiceImpl implements InactivityService {
	@Inject
	private ExploratoryDAO exploratoryDAO;
	@Inject
	private ComputationalDAO computationalDAO;
	@Inject
	private EnvDAO envDAO;
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
	public void updateRunningResourcesLastActivity(UserInfo userInfo) {
		List<EnvResource> resources = envDAO.findRunningResourcesForCheckInactivity();
		if (!resources.isEmpty()) {
			String uuid = provisioningService.post(InfrasctructureAPI.INFRASTRUCTURE_CHECK_INACTIVITY,
					userInfo.getAccessToken(), resources, String.class);
			requestId.put(userInfo.getName(), uuid);
		}
	}

	@Override
	public void stopClustersByInactivity(List<String> computationalIds) {
		exploratoryDAO.getInstancesByComputationalIdsAndStatus(computationalIds, RUNNING)
				.forEach(this::stopClusters);
	}

	@Override
	public void updateLastActivityForClusters(List<EnvResource> clusters) {
		log.debug("Updating last activity date for clusters...");
		clusters.forEach(r -> computationalDAO.updateLastActivityDateForInstanceId(r.getId(), r.getLastActivity()));
	}

	@Override
	public void stopByInactivity(List<EnvResource> exploratories) {
		final List<String> expIds = exploratories.stream().map(EnvResource::getId).collect(Collectors.toList());
		exploratoryDAO.getInstancesByIdsAndStatus(expIds, RUNNING)
				.stream()
				.filter(this::shouldExploratoryBeInactivated)
				.forEach(this::stopNotebook);

	}

	@Override
	public void updateLastActivity(List<EnvResource> exploratories) {
		exploratories.forEach(r -> exploratoryDAO.updateLastActivityDateForInstanceId(r.getId(), r.getLastActivity()));
	}

	private void stopNotebook(UserInstanceDTO ui) {
		exploratoryService.stop(systemUserInfoService.create(ui.getUser()), ui.getExploratoryName());
	}

	private boolean shouldExploratoryBeInactivated(UserInstanceDTO ui) {
		final SchedulerJobDTO schedulerData = ui.getSchedulerData();

		return Objects.nonNull(schedulerData) && schedulerData.isCheckInactivityRequired() && Objects.nonNull(ui.getLastActivity()) &&
				ui.getLastActivity().plusMinutes(schedulerData.getMaxInactivity()).isBefore(LocalDateTime.now());
	}

	private void stopClusters(UserInstanceDTO ui) {
		ui.getResources().stream()
				.filter(this::shouldClusterBeInactivated)
				.forEach(c -> stopCluster(c, ui.getUser(), ui.getExploratoryName()));
	}

	private boolean shouldClusterBeInactivated(UserComputationalResource c) {
		final SchedulerJobDTO schedulerData = c.getSchedulerData();
		return Objects.nonNull(schedulerData) && schedulerData.isCheckInactivityRequired() &&
				c.getLastActivity().plusMinutes(schedulerData.getMaxInactivity()).isBefore(LocalDateTime.now());
	}

	private void stopCluster(UserComputationalResource c, String user, String exploratoryName) {
		final DataEngineType dataEngineType = c.getDataEngineType();
		final String compName = c.getComputationalName();
		if (dataEngineType == SPARK_STANDALONE) {
			computationalService.stopSparkCluster(systemUserInfoService.create(user), exploratoryName, compName);
		} else if (dataEngineType == CLOUD_SERVICE) {
			computationalService.terminateComputational(systemUserInfoService.create(user), exploratoryName, compName);
		}
	}
}
