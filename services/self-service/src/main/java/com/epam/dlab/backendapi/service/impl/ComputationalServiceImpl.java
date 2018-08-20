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

package com.epam.dlab.backendapi.service.impl;


import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.computational.*;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.dlab.dto.UserInstanceStatus.*;
import static com.epam.dlab.rest.contracts.ComputationalAPI.COMPUTATIONAL_CREATE_CLOUD_SPECIFIC;

@Singleton
@Slf4j
public class ComputationalServiceImpl implements ComputationalService {

	private static final String COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT = "Could not update the status of " +
			"computational resource {} for user {}";
	private static final String OP_NOT_SUPPORTED_DES = "Operation for data engine service is not supported";

	@Inject
	private ExploratoryDAO exploratoryDAO;

	@Inject
	private ComputationalDAO computationalDAO;

	@Inject
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;

	@Inject
	private SelfServiceApplicationConfiguration configuration;

	@Inject
	private RequestBuilder requestBuilder;

	@Inject
	private RequestId requestId;

	@Inject
	private SystemUserInfoService systemUserInfoService;

	@Inject
	private ExploratoryService exploratoryService;


	@Override
	public boolean createSparkCluster(UserInfo userInfo, SparkStandaloneClusterCreateForm form) {

		validateForm(form);

		if (computationalDAO.addComputational(userInfo.getName(), form.getNotebookName(),
				createInitialComputationalResource(form))) {

			try {
				UserInstanceDTO instance =
						exploratoryDAO.fetchExploratoryFields(userInfo.getName(), form.getNotebookName());

				ComputationalBase<?> dto = requestBuilder.newComputationalCreate(userInfo, instance, form);

				String uuid = provisioningService.post(ComputationalAPI.COMPUTATIONAL_CREATE_SPARK,
						userInfo.getAccessToken(), dto, String.class);
				requestId.put(userInfo.getName(), uuid);
				return true;
			} catch (RuntimeException e) {
				try {
					updateComputationalStatus(userInfo.getName(), form.getNotebookName(), form.getName(), FAILED);
				} catch (DlabException d) {
					log.error(COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT, form.getName(), userInfo.getName(), d);
				}
				throw e;
			}
		} else {
			log.debug("Computational with name {} is already existing for user {}", form.getName(),
					userInfo.getName());
			return false;
		}
	}

	@Override
	public void terminateComputationalEnvironment(UserInfo userInfo, String exploratoryName, String
			computationalName) {
		try {

			updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, TERMINATING);

			String exploratoryId = exploratoryDAO.fetchExploratoryId(userInfo.getName(), exploratoryName);
			UserComputationalResource computationalResource = computationalDAO.fetchComputationalFields(userInfo
					.getName(), exploratoryName, computationalName);

			ComputationalTerminateDTO dto = requestBuilder.newComputationalTerminate(userInfo, exploratoryName,
					exploratoryId, computationalName, computationalResource.getComputationalId(),
					DataEngineType.fromDockerImageName(computationalResource.getImageName()));

			String uuid = provisioningService.post(getTerminateUrl(computationalResource), userInfo.getAccessToken(),
					dto, String.class);
			requestId.put(userInfo.getName(), uuid);
		} catch (RuntimeException re) {

			try {
				updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, FAILED);
			} catch (DlabException e) {
				log.error(COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT, computationalName, userInfo.getName(), e);
			}

			throw re;
		}
	}

	@Override
	public boolean createDataEngineService(UserInfo userInfo, ComputationalCreateFormDTO formDTO,
										   UserComputationalResource computationalResource) {

		boolean isAdded = computationalDAO.addComputational(userInfo.getName(), formDTO.getNotebookName(),
				computationalResource);

		if (isAdded) {
			try {
				UserInstanceDTO instance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), formDTO
						.getNotebookName());
				String uuid = provisioningService.post(COMPUTATIONAL_CREATE_CLOUD_SPECIFIC, userInfo.getAccessToken(),
						requestBuilder.newComputationalCreate(userInfo, instance, formDTO), String.class);
				requestId.put(userInfo.getName(), uuid);
				return true;
			} catch (Exception t) {
				try {
					updateComputationalStatus(userInfo.getName(), formDTO.getNotebookName(), formDTO.getName(),
							FAILED);
				} catch (DlabException e) {
					log.error(COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT, formDTO.getName(), userInfo.getName(), e);
				}
				throw new DlabException("Could not send request for creation the computational resource " + formDTO
						.getName() + ": " + t.getLocalizedMessage(), t);
			}
		} else {
			log.debug("Used existing computational resource {} for user {}", formDTO.getName(), userInfo.getName());
			return false;
		}
	}

	@Override
	public void stopSparkCluster(UserInfo userInfo, String exploratoryName, String computationalName) {
		sparkAction(userInfo, exploratoryName, computationalName, STOPPING, ComputationalAPI.COMPUTATIONAL_STOP_SPARK);
	}

	@Override
	public void startSparkCluster(UserInfo userInfo, String exploratoryName, String computationalName) {
		sparkAction(userInfo, exploratoryName, computationalName, STARTING,
				ComputationalAPI.COMPUTATIONAL_START_SPARK);
	}

	/**
	 * Updates parameter 'reuploadKeyRequired' for corresponding user's computational resources with allowable statuses
	 * which are affiliated with exploratories with theirs allowable statuses.
	 *
	 * @param user                  user.
	 * @param exploratoryStatuses   allowable exploratories' statuses.
	 * @param computationalTypes    type list of computational resource.
	 * @param reuploadKeyRequired   true/false.
	 * @param computationalStatuses allowable statuses for computational resources.
	 */
	@Override
	public void updateComputationalsReuploadKeyFlag(String user, List<UserInstanceStatus> exploratoryStatuses,
													List<DataEngineType> computationalTypes,
													boolean reuploadKeyRequired,
													UserInstanceStatus... computationalStatuses) {
		computationalDAO.updateReuploadKeyFlagForComputationalResources(user, exploratoryStatuses, computationalTypes,
				reuploadKeyRequired, computationalStatuses);
	}

	/**
	 * Returns computational resource's data by name for user's exploratory.
	 *
	 * @param user              user.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @return corresponding computational resource's data or empty data if resource doesn't exist.
	 */
	@Override
	public Optional<UserComputationalResource> getComputationalResource(String user, String exploratoryName,
																		String computationalName) {
		try {
			return Optional.of(computationalDAO.fetchComputationalFields(user, exploratoryName, computationalName));
		} catch (DlabException e) {
			log.warn("Computational resource {} affiliated with exploratory {} for user {} not found.",
					computationalName, exploratoryName, user);
		}
		return Optional.empty();
	}

	@Override
	public void stopClustersByCondition(CheckInactivityClusterStatusDTO dto) {
		if (dto.getCheckInactivityClusterStatus() == CheckInactivityClusterStatus.COMPLETED &&
				!dto.getClusters().isEmpty()) {
			List<String> ids = getIds(dto.getClusters());
			List<UserInstanceDTO> instances = exploratoryService.getInstancesByComputationalIds(ids);
			instances.forEach(this::turnOffClustersAffiliatedWithExploratory);
		}
	}

	@Override
	public void updateLastActivityForClusters(CheckInactivityClusterStatusDTO dto) {
		log.debug("Updating last activity date for clusters...");
		List<String> ids = getIds(dto.getClusters());
		List<UserInstanceDTO> instances = exploratoryService.getInstancesByComputationalIds(ids);
		instances.forEach(this::updateLastActivityForClustersAffiliatedWithInstance);
	}

	@Override
	public void updateCheckInactivityFlag(UserInfo userInfo, String exploratoryName, String computationalName,
										  boolean checkInactivityRequired) {
		computationalDAO.updateCheckInactivityFlagForComputationalResource(userInfo.getName(), exploratoryName,
				computationalName, checkInactivityRequired);
	}

	private void updateLastActivityForClustersAffiliatedWithInstance(UserInstanceDTO ui) {
		ui.getResources().forEach(cr -> updateLastActivityForCluster(ui.getUser(), ui.getExploratoryName(),
				cr.getComputationalName(), cr.getLastActivity()));
	}

	private void updateLastActivityForCluster(String user, String exploratoryName, String computationalName,
											  Date lastActivity) {
		computationalDAO.updateLastActivityForCluster(user, exploratoryName, computationalName, lastActivity);
	}

	private void turnOffClustersAffiliatedWithExploratory(UserInstanceDTO ui) {
		ui.getResources().forEach(cr ->
				turnOffClustersByCondition(convertedToLocalDate(cr.getLastActivity()),
						DataEngineType.fromDockerImageName(cr.getImageName()), ui.getUser(), ui.getExploratoryName(),
						cr.getComputationalName()));
	}

	private LocalDateTime convertedToLocalDate(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	private List<String> getIds(List<EnvResource> resources) {
		return resources.stream().map(EnvResource::getId).collect(Collectors.toList());
	}

	private void turnOffClustersByCondition(LocalDateTime lastActivityDate,
											DataEngineType dataEngineType, String user,
											String exploratoryName, String computationalName) {
		LocalDateTime now = LocalDateTime.now();
		if (lastActivityDate.plus(configuration.getClusterInactivityCheckingTimeout().toMinutes(),
				ChronoUnit.MINUTES).isBefore(now)) {
			UserInfo userInfo = systemUserInfoService.create(user);
			if (dataEngineType == DataEngineType.CLOUD_SERVICE) {
				terminateComputationalEnvironment(userInfo, exploratoryName, computationalName);
			} else if (dataEngineType == DataEngineType.SPARK_STANDALONE) {
				stopSparkCluster(userInfo, exploratoryName, computationalName);
			}
		}
	}

	private void sparkAction(UserInfo userInfo, String exploratoryName, String computationalName, UserInstanceStatus
			compStatus, String provisioningEndpoint) {
		final UserComputationalResource computationalResource = computationalDAO.fetchComputationalFields(userInfo
				.getName(), exploratoryName, computationalName);
		final DataEngineType dataEngineType = DataEngineType.fromDockerImageName(computationalResource.getImageName());
		if (DataEngineType.SPARK_STANDALONE == dataEngineType) {
			log.debug("{} spark cluster {} for exploratory {}", compStatus.toString(), computationalName,
					exploratoryName);
			updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, compStatus);
			final UserInstanceDTO exploratory = exploratoryDAO.fetchExploratoryFields(userInfo.getName(),
					exploratoryName);
			final String uuid = provisioningService.post(provisioningEndpoint,
					userInfo.getAccessToken(),
					toProvisioningDto(userInfo, computationalName, compStatus, exploratory),
					String.class);
			requestId.put(userInfo.getName(), uuid);
		} else {
			log.error(OP_NOT_SUPPORTED_DES);
			throw new UnsupportedOperationException(OP_NOT_SUPPORTED_DES);
		}
	}

	private ComputationalBase<? extends ComputationalBase<?>> toProvisioningDto(UserInfo userInfo, String
			computationalName, UserInstanceStatus compStatus, UserInstanceDTO exploratory) {
		if (UserInstanceStatus.STARTING == compStatus) {
			return requestBuilder
					.newComputationalStart(userInfo, exploratory, computationalName);
		} else if (UserInstanceStatus.STOPPING == compStatus) {
			return requestBuilder
					.newComputationalStop(userInfo, exploratory, computationalName);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	private String getTerminateUrl(UserComputationalResource computationalResource) {

		if (DataEngineType.fromDockerImageName(computationalResource.getImageName())
				== DataEngineType.SPARK_STANDALONE) {

			return ComputationalAPI.COMPUTATIONAL_TERMINATE_SPARK;
		} else if (DataEngineType.fromDockerImageName(computationalResource.getImageName())
				== DataEngineType.CLOUD_SERVICE) {

			return ComputationalAPI.COMPUTATIONAL_TERMINATE_CLOUD_SPECIFIC;
		} else {
			throw new IllegalArgumentException("Unknown docker image for " + computationalResource);
		}
	}

	/**
	 * Validates if input form is correct
	 *
	 * @param form user input form
	 * @throws IllegalArgumentException if user typed wrong arguments
	 */

	private void validateForm(SparkStandaloneClusterCreateForm form) {

		int instanceCount = Integer.parseInt(form.getDataEngineInstanceCount());

		if (instanceCount < configuration.getMinSparkInstanceCount()
				|| instanceCount > configuration.getMaxSparkInstanceCount()) {
			throw new IllegalArgumentException(String.format("Instance count should be in range [%d..%d]",
					configuration.getMinSparkInstanceCount(), configuration.getMaxSparkInstanceCount()));
		}

		if (DataEngineType.fromDockerImageName(form.getImage()) != DataEngineType.SPARK_STANDALONE) {
			throw new IllegalArgumentException(String.format("Unknown data engine %s", form.getImage()));
		}
	}

	/**
	 * Updates the status of computational resource in database.
	 *
	 * @param user              user name.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @param status            status
	 */
	private void updateComputationalStatus(String user, String exploratoryName, String computationalName,
										   UserInstanceStatus status) {
		ComputationalStatusDTO computationalStatus = new ComputationalStatusDTO()
				.withUser(user)
				.withExploratoryName(exploratoryName)
				.withComputationalName(computationalName)
				.withStatus(status);

		computationalDAO.updateComputationalStatus(computationalStatus);
	}

	private SparkStandaloneClusterResource createInitialComputationalResource(SparkStandaloneClusterCreateForm form) {

		return SparkStandaloneClusterResource.builder()
				.computationalName(form.getName())
				.imageName(form.getImage())
				.templateName(form.getTemplateName())
				.status(CREATING.toString())
				.dataEngineInstanceCount(form.getDataEngineInstanceCount())
				.dataEngineInstanceShape(form.getDataEngineInstanceShape())
				.config(form.getConfig())
				.checkInactivityRequired(form.isCheckInactivityRequired())
				.build();
	}

}
