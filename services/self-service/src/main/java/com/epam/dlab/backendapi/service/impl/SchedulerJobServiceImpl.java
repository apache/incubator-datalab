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

import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SchedulerJobDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.SchedulerJobService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.exceptions.ResourceInappropriateStateException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.InfrasctructureAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.epam.dlab.backendapi.dao.SchedulerJobDAO.TIMEZONE_PREFIX;
import static com.epam.dlab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;
import static com.epam.dlab.dto.UserInstanceStatus.*;
import static com.epam.dlab.dto.base.DataEngineType.getDockerImageName;
import static java.util.Collections.singletonList;

@Slf4j
@Singleton
public class SchedulerJobServiceImpl implements SchedulerJobService {

	private static final String SCHEDULER_NOT_FOUND_MSG =
			"Scheduler job data not found for user %s with exploratory %s";

	@Inject
	private SchedulerJobDAO schedulerJobDAO;

	@Inject
	private ExploratoryDAO exploratoryDAO;

	@Inject
	private ComputationalDAO computationalDAO;

	@Inject
	private ExploratoryService exploratoryService;

	@Inject
	private ComputationalService computationalService;

	@Inject
	private SystemUserInfoService systemUserService;

	@Inject
	private EnvDAO envDAO;

	@Inject
	private RequestId requestId;

	@Inject
	@Named(PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;

	@Override
	public SchedulerJobDTO fetchSchedulerJobForUserAndExploratory(String user, String exploratoryName) {
		return schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(user, exploratoryName)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(SCHEDULER_NOT_FOUND_MSG, user,
						exploratoryName)));
	}

	@Override
	public SchedulerJobDTO fetchSchedulerJobForComputationalResource(String user, String exploratoryName,
																	 String computationalName) {
		return schedulerJobDAO.fetchSingleSchedulerJobForCluster(user, exploratoryName, computationalName)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(SCHEDULER_NOT_FOUND_MSG, user,
						exploratoryName) + " with computational resource " + computationalName));
	}

	@Override
	public void updateExploratorySchedulerData(String user, String exploratoryName, SchedulerJobDTO dto) {
		validateExploratoryStatus(user, exploratoryName);
		populateDefaultSchedulerValues(dto);
		log.debug("Updating exploratory {} for user {} with new scheduler job data: {}...", exploratoryName, user,
				dto);
		exploratoryDAO.updateSchedulerDataForUserAndExploratory(user, exploratoryName, dto);
		if (dto.isSyncStartRequired()) {
			shareSchedulerJobDataToSparkClusters(user, exploratoryName, dto);
		} else {
			computationalDAO.updateSchedulerSyncFlag(user, exploratoryName, dto.isSyncStartRequired());
		}
	}

	@Override
	public void updateComputationalSchedulerData(String user, String exploratoryName, String computationalName,
												 SchedulerJobDTO dto) {
		validateExploratoryStatus(user, exploratoryName);
		validateComputationalStatus(user, exploratoryName, computationalName);
		populateDefaultSchedulerValues(dto);
		log.debug("Updating computational resource {} affiliated with exploratory {} for user {} with new scheduler " +
				"job data {}...", computationalName, exploratoryName, user, dto);
		computationalDAO.updateSchedulerDataForComputationalResource(user, exploratoryName, computationalName, dto);
	}

	@Override
	public void stopComputationalByScheduler() {
		getComputationalSchedulersForStopping(OffsetDateTime.now())
				.forEach(job -> computationalService.stopSparkCluster(systemUserService.create(job.getUser()),
						job.getExploratoryName(), job.getComputationalName()));
	}

	@Override
	public void stopExploratoryByScheduler() {
		getExploratorySchedulersForStopping(OffsetDateTime.now())
				.forEach(job -> exploratoryService.stop(systemUserService.create(job.getUser()),
						job.getExploratoryName()));
	}

	@Override
	public void startExploratoryByScheduler() {
		getExploratorySchedulersForStarting(OffsetDateTime.now())
				.forEach(this::startExploratory);
	}

	@Override
	public void startComputationalByScheduler() {
		getComputationalSchedulersForStarting(OffsetDateTime.now())
				.forEach(job -> computationalService.startSparkCluster(systemUserService.create(job.getUser()),
						job.getExploratoryName(), job.getComputationalName()));
	}

	@Override
	public void terminateExploratoryByScheduler() {
		getExploratorySchedulersForTerminating(OffsetDateTime.now())
				.forEach(job -> exploratoryService.terminate(systemUserService.create(job.getUser()),
						job.getExploratoryName()));

	}

	@Override
	public void terminateComputationalByScheduler() {
		getComputationalSchedulersForTerminating(OffsetDateTime.now())
				.forEach(job ->
						computationalService.terminateComputationalEnvironment(
								systemUserService.create(job.getUser()), job.getExploratoryName(),
								job.getComputationalName()));

	}

	@Override
	public String executeCheckClusterInactivityJob(UserInfo userInfo) {
		List<EnvResource> runningClusters = envDAO.findRunningClustersForCheckInactivity();
		if (!runningClusters.isEmpty()) {
			String uuid = provisioningService.post(InfrasctructureAPI.INFRASTRUCTURE_CHECK_INACTIVITY,
					userInfo.getAccessToken(), runningClusters, String.class);
			requestId.put(userInfo.getName(), uuid);
			return uuid;
		} else return StringUtils.EMPTY;
	}

	private List<SchedulerJobData> getExploratorySchedulersForTerminating(OffsetDateTime now) {
		return schedulerJobDAO.getExploratorySchedulerDataWithOneOfStatus(RUNNING, STOPPED)
				.stream()
				.filter(canSchedulerForTerminatingBeApplied(now))
				.collect(Collectors.toList());
	}

	private List<SchedulerJobData> getComputationalSchedulersForTerminating(OffsetDateTime now) {
		return schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(RUNNING,
				DataEngineType.SPARK_STANDALONE, STOPPED, RUNNING)
				.stream()
				.filter(canSchedulerForTerminatingBeApplied(now))
				.collect(Collectors.toList());
	}

	private void startExploratory(SchedulerJobData schedulerJobData) {
		final String user = schedulerJobData.getUser();
		final String exploratoryName = schedulerJobData.getExploratoryName();
		exploratoryService.start(systemUserService.create(user), exploratoryName);
		if (schedulerJobData.getJobDTO().isSyncStartRequired()) {
			final DataEngineType sparkCluster = DataEngineType.SPARK_STANDALONE;
			computationalDAO.findComputationalResourcesWithStatus(user, exploratoryName, STOPPED)
					.stream()
					.filter(compResource -> shouldClusterBeStarted(sparkCluster, compResource))
					.forEach(comp -> computationalService.startSparkCluster(systemUserService.create(user),
							exploratoryName, comp.getComputationalName()));
		}
	}

	private boolean shouldClusterBeStarted(DataEngineType sparkCluster, UserComputationalResource compResource) {
		return Objects.nonNull(compResource.getSchedulerData()) && compResource.getSchedulerData().isSyncStartRequired()
				&& compResource.getImageName().equals(getDockerImageName(sparkCluster));
	}

	/**
	 * Performs bulk updating operation with scheduler data for corresponding to exploratory Spark clusters.
	 * All these clusters will obtain data which is equal to exploratory's except 'stopping' operation (it will be
	 * performed automatically with notebook stopping since Spark clusters have such feature).
	 *
	 * @param user            user's name
	 * @param exploratoryName name of exploratory resource
	 * @param dto             scheduler job data.
	 */
	private void shareSchedulerJobDataToSparkClusters(String user, String exploratoryName, SchedulerJobDTO dto) {
		List<String> correspondingSparkClusters = computationalDAO.getComputationalResourcesWhereStatusIn(user,
				singletonList(DataEngineType.SPARK_STANDALONE), exploratoryName,
				STARTING, RUNNING, STOPPING, STOPPED);
		SchedulerJobDTO dtoWithoutStopData = getSchedulerJobWithoutStopData(dto);
		for (String sparkName : correspondingSparkClusters) {
			log.debug("Updating computational resource {} affiliated with exploratory {} for user {} with new " +
					"scheduler job data {}...", sparkName, exploratoryName, user, dtoWithoutStopData);
			computationalDAO.updateSchedulerDataForComputationalResource(user, exploratoryName, sparkName,
					dtoWithoutStopData);
		}
	}

	private List<SchedulerJobData> getExploratorySchedulersForStopping(OffsetDateTime currentDateTime) {
		return schedulerJobDAO.getExploratorySchedulerDataWithStatus(RUNNING)
				.stream()
				.filter(canSchedulerForStoppingBeApplied(currentDateTime))
				.collect(Collectors.toList());
	}

	private List<SchedulerJobData> getExploratorySchedulersForStarting(OffsetDateTime currentDateTime) {
		return schedulerJobDAO.getExploratorySchedulerDataWithStatus(STOPPED)
				.stream()
				.filter(canSchedulerForStartingBeApplied(currentDateTime))
				.collect(Collectors.toList());
	}

	private List<SchedulerJobData> getComputationalSchedulersForStarting(OffsetDateTime currentDateTime) {
		return schedulerJobDAO
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, STOPPED)
				.stream()
				.filter(canSchedulerForStartingBeApplied(currentDateTime))
				.collect(Collectors.toList());
	}

	private Predicate<SchedulerJobData> canSchedulerForStoppingBeApplied(OffsetDateTime currentDateTime) {
		return schedulerJobData -> shouldSchedulerBeExecuted(schedulerJobData.getJobDTO(),
				currentDateTime, schedulerJobData.getJobDTO().getStopDaysRepeat(),
				schedulerJobData.getJobDTO().getEndTime());
	}

	private Predicate<SchedulerJobData> canSchedulerForStartingBeApplied(OffsetDateTime currentDateTime) {
		return schedulerJobData -> shouldSchedulerBeExecuted(schedulerJobData.getJobDTO(),
				currentDateTime, schedulerJobData.getJobDTO().getStartDaysRepeat(),
				schedulerJobData.getJobDTO().getStartTime());
	}

	private Predicate<SchedulerJobData> canSchedulerForTerminatingBeApplied(OffsetDateTime currentDateTime) {
		return schedulerJobData -> shouldBeTerminated(currentDateTime, schedulerJobData);
	}

	private boolean shouldBeTerminated(OffsetDateTime currentDateTime, SchedulerJobData schedulerJobData) {
		final SchedulerJobDTO jobDTO = schedulerJobData.getJobDTO();
		final LocalDateTime convertedCurrentTime = schedulerExecutionDate(jobDTO, currentDateTime);
		return Objects.nonNull(jobDTO.getTerminateDateTime()) &&
				convertedCurrentTime.equals(jobDTO.getTerminateDateTime());
	}

	private List<SchedulerJobData> getComputationalSchedulersForStopping(OffsetDateTime currentDateTime) {
		return schedulerJobDAO
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, RUNNING)
				.stream()
				.filter(canSchedulerForStoppingBeApplied(currentDateTime))
				.collect(Collectors.toList());
	}

	private void populateDefaultSchedulerValues(SchedulerJobDTO dto) {
		if (Objects.isNull(dto.getBeginDate()) || StringUtils.isBlank(dto.getBeginDate().toString())) {
			dto.setBeginDate(LocalDate.now());
		}
		if (Objects.isNull(dto.getTimeZoneOffset()) || StringUtils.isBlank(dto.getTimeZoneOffset().toString())) {
			dto.setTimeZoneOffset(OffsetDateTime.now(ZoneId.systemDefault()).getOffset());
		}
	}

	private void validateExploratoryStatus(String user, String exploratoryName) {
		final UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(user, exploratoryName);
		validateResourceStatus(userInstance.getStatus());
	}

	private void validateComputationalStatus(String user, String exploratoryName, String computationalName) {
		final UserComputationalResource computationalResource =
				computationalDAO.fetchComputationalFields(user, exploratoryName, computationalName);
		final String computationalStatus = computationalResource.getStatus();
		validateResourceStatus(computationalStatus);
	}

	private void validateResourceStatus(String resourceStatus) {
		final UserInstanceStatus status = UserInstanceStatus.of(resourceStatus);
		if (Objects.isNull(status) || status.in(UserInstanceStatus.TERMINATED, UserInstanceStatus.TERMINATING,
				UserInstanceStatus.FAILED)) {
			throw new ResourceInappropriateStateException(String.format("Can not create/update scheduler for user " +
					"instance with status: %s", status));
		}
	}

	private boolean shouldSchedulerBeExecuted(SchedulerJobDTO dto, OffsetDateTime dateTime, List<DayOfWeek> daysRepeat,
											  LocalTime time) {
		LocalDateTime convertedDateTime = schedulerExecutionDate(dto, dateTime);

		return !convertedDateTime.toLocalDate().isBefore(dto.getBeginDate())
				&& finishDateAfterCurrentDate(dto, convertedDateTime)
				&& daysRepeat.contains(convertedDateTime.toLocalDate().getDayOfWeek())
				&& convertedDateTime.toLocalTime().equals(time);
	}

	private LocalDateTime schedulerExecutionDate(SchedulerJobDTO dto, OffsetDateTime dateTime) {
		ZoneOffset zOffset = dto.getTimeZoneOffset();
		OffsetDateTime roundedDateTime = OffsetDateTime.of(
				dateTime.toLocalDate(),
				LocalTime.of(dateTime.toLocalTime().getHour(), dateTime.toLocalTime().getMinute()),
				dateTime.getOffset());

		return ZonedDateTime.ofInstant(roundedDateTime.toInstant(),
				ZoneId.ofOffset(TIMEZONE_PREFIX, zOffset)).toLocalDateTime();
	}

	private boolean finishDateAfterCurrentDate(SchedulerJobDTO dto, LocalDateTime currentDateTime) {
		return Objects.isNull(dto.getFinishDate()) || !currentDateTime.toLocalDate().isAfter(dto.getFinishDate());
	}

	private SchedulerJobDTO getSchedulerJobWithoutStopData(SchedulerJobDTO dto) {
		SchedulerJobDTO convertedDto = new SchedulerJobDTO();
		convertedDto.setBeginDate(dto.getBeginDate());
		convertedDto.setFinishDate(dto.getFinishDate());
		convertedDto.setStartTime(dto.getStartTime());
		convertedDto.setStartDaysRepeat(dto.getStartDaysRepeat());
		convertedDto.setTerminateDateTime(dto.getTerminateDateTime());
		convertedDto.setTimeZoneOffset(dto.getTimeZoneOffset());
		convertedDto.setSyncStartRequired(dto.isSyncStartRequired());
		return convertedDto;
	}

}

