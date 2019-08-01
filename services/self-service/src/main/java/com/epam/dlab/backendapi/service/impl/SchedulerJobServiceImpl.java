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
import com.epam.dlab.backendapi.dao.SchedulerJobDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.SchedulerJobService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.exceptions.ResourceInappropriateStateException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.dlab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;
import static com.epam.dlab.dto.UserInstanceStatus.*;
import static com.epam.dlab.dto.base.DataEngineType.getDockerImageName;
import static java.time.ZoneId.systemDefault;
import static java.util.Collections.singletonList;
import static java.util.Date.from;

@Slf4j
@Singleton
public class SchedulerJobServiceImpl implements SchedulerJobService {

	private static final String SCHEDULER_NOT_FOUND_MSG =
			"Scheduler job data not found for user %s with exploratory %s";
	private static final long ALLOWED_INACTIVITY_MINUTES = 1L;

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
	private SecurityService securityService;

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

		if (!dto.inactivityScheduler() && dto.isSyncStartRequired()) {
			shareSchedulerJobDataToSparkClusters(user, exploratoryName, dto);
		} else if (!dto.inactivityScheduler()) {
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
		getComputationalSchedulersForStopping(OffsetDateTime.now(), true)
				.forEach(this::stopComputational);
	}

	@Override
	public void stopExploratoryByScheduler() {
		getExploratorySchedulersForStopping(OffsetDateTime.now(), true)
				.forEach(this::stopExploratory);
	}

	@Override
	public void startExploratoryByScheduler() {
		getExploratorySchedulersForStarting(OffsetDateTime.now())
				.forEach(this::startExploratory);
	}

	@Override
	public void startComputationalByScheduler() {
		getComputationalSchedulersForStarting(OffsetDateTime.now())
				.forEach(job -> startSpark(job.getUser(), job.getExploratoryName(), job.getComputationalName()));
	}

	@Override
	public void terminateExploratoryByScheduler() {
		getExploratorySchedulersForTerminating(OffsetDateTime.now())
				.forEach(this::terminateExploratory);

	}

	@Override
	public void terminateComputationalByScheduler() {
		getComputationalSchedulersForTerminating(OffsetDateTime.now()).forEach(this::terminateComputational);

	}

	@Override
	public void removeScheduler(String user, String exploratoryName) {
		schedulerJobDAO.removeScheduler(user, exploratoryName);
	}

	@Override
	public void removeScheduler(String user, String exploratoryName, String computationalName) {
		schedulerJobDAO.removeScheduler(user, exploratoryName, computationalName);
	}

	@Override
	public List<SchedulerJobData> getActiveSchedulers(String user, long minutesOffset) {
		final OffsetDateTime desiredDateTime = OffsetDateTime.now().plusMinutes(minutesOffset);
		final Predicate<SchedulerJobData> userPredicate = s -> user.equals(s.getUser());
		final Stream<SchedulerJobData> computationalSchedulersStream =
				getComputationalSchedulersForStopping(desiredDateTime, false)
						.stream()
						.filter(userPredicate);
		final Stream<SchedulerJobData> exploratorySchedulersStream =
				getExploratorySchedulersForStopping(desiredDateTime, false)
						.stream()
						.filter(userPredicate);
		return Stream.concat(computationalSchedulersStream, exploratorySchedulersStream)
				.collect(Collectors.toList());
	}

	private void stopComputational(SchedulerJobData job) {
		final String expName = job.getExploratoryName();
		final String compName = job.getComputationalName();
		final String user = job.getUser();
		log.debug("Stopping exploratory {} computational {} for user {} by scheduler", expName, compName, user);
		computationalService.stopSparkCluster(securityService.getUserInfoOffline(user), expName, compName);
	}

	private void terminateComputational(SchedulerJobData job) {
		final String user = job.getUser();
		final String expName = job.getExploratoryName();
		final String compName = job.getComputationalName();
		final UserInfo userInfo = securityService.getUserInfoOffline(user);
		log.debug("Terminating exploratory {} computational {} for user {} by scheduler", expName, compName, user);
		computationalService.terminateComputational(userInfo, expName, compName);
	}

	private void stopExploratory(SchedulerJobData job) {
		final String expName = job.getExploratoryName();
		final String user = job.getUser();
		log.debug("Stopping exploratory {} for user {} by scheduler", expName, user);
		exploratoryService.stop(securityService.getUserInfoOffline(user), expName);
	}

	private List<SchedulerJobData> getExploratorySchedulersForTerminating(OffsetDateTime now) {
		return schedulerJobDAO.getExploratorySchedulerDataWithOneOfStatus(RUNNING, STOPPED)
				.stream()
				.filter(canSchedulerForTerminatingBeApplied(now))
				.collect(Collectors.toList());
	}

	private List<SchedulerJobData> getComputationalSchedulersForTerminating(OffsetDateTime now) {
		return schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(RUNNING, STOPPED, RUNNING)
				.stream()
				.filter(canSchedulerForTerminatingBeApplied(now))
				.collect(Collectors.toList());
	}

	private void startExploratory(SchedulerJobData schedulerJobData) {
		final String user = schedulerJobData.getUser();
		final String exploratoryName = schedulerJobData.getExploratoryName();
		log.debug("Starting exploratory {} for user {} by scheduler", exploratoryName, user);
		exploratoryService.start(securityService.getUserInfoOffline(user), exploratoryName, ""); //TODO chagne empty
		if (schedulerJobData.getJobDTO().isSyncStartRequired()) {
			log.trace("Starting computational for exploratory {} for user {} by scheduler", exploratoryName, user);
			final DataEngineType sparkCluster = DataEngineType.SPARK_STANDALONE;
			final List<UserComputationalResource> compToBeStarted =
					computationalDAO.findComputationalResourcesWithStatus(user, exploratoryName, STOPPED);

			compToBeStarted
					.stream()
					.filter(compResource -> shouldClusterBeStarted(sparkCluster, compResource))
					.forEach(comp -> startSpark(user, exploratoryName, comp.getComputationalName()));
		}
	}

	private void terminateExploratory(SchedulerJobData job) {
		final String user = job.getUser();
		final String expName = job.getExploratoryName();
		log.debug("Terminating exploratory {} for user {} by scheduler", expName, user);
		exploratoryService.terminate(securityService.getUserInfoOffline(user), expName);
	}

	private void startSpark(String user, String expName, String compName) {
		log.debug("Starting exploratory {} computational {} for user {} by scheduler", expName, compName, user);
		computationalService.startSparkCluster(securityService.getUserInfoOffline(user), expName, compName, "");
		//TODO change empty string
	}

	private boolean shouldClusterBeStarted(DataEngineType sparkCluster, UserComputationalResource compResource) {
		return Objects.nonNull(compResource.getSchedulerData()) && compResource.getSchedulerData().isSyncStartRequired()
				&& compResource.getImageName().equals(getDockerImageName(sparkCluster));
	}

	/**
	 * Performs bulk updating operation with scheduler data for corresponding to exploratory Spark clusters.
	 * All these resources will obtain data which is equal to exploratory's except 'stopping' operation (it will be
	 * performed automatically with notebook stopping since Spark resources have such feature).
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

	private List<SchedulerJobData> getExploratorySchedulersForStopping(OffsetDateTime currentDateTime,
																	   boolean checkInactivity) {

		final Date clusterMaxInactivityAllowedDate =
				from(LocalDateTime.now().minusMinutes(ALLOWED_INACTIVITY_MINUTES).atZone(systemDefault()).toInstant());
		return schedulerJobDAO.getExploratorySchedulerWithStatusAndClusterLastActivityLessThan(RUNNING,
				clusterMaxInactivityAllowedDate)
				.stream()
				.filter(canSchedulerForStoppingBeApplied(currentDateTime)
						.or(schedulerJobData -> checkInactivity && exploratoryInactivityCondition(schedulerJobData)))
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
		final ZoneOffset timeZoneOffset = jobDTO.getTimeZoneOffset();
		final LocalDateTime convertedCurrentTime = localDateTimeAtZone(currentDateTime, timeZoneOffset);
		final LocalDateTime terminateDateTime = jobDTO.getTerminateDateTime();
		return Objects.nonNull(terminateDateTime) && isSchedulerActive(jobDTO, convertedCurrentTime) &&
				convertedCurrentTime.equals(terminateDateTime.atOffset(timeZoneOffset).toLocalDateTime());
	}

	private List<SchedulerJobData> getComputationalSchedulersForStopping(OffsetDateTime currentDateTime,
																		 boolean checkInactivity) {
		return schedulerJobDAO
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, RUNNING)
				.stream()
				.filter(canSchedulerForStoppingBeApplied(currentDateTime)
						.or(schedulerJobData -> checkInactivity && computationalInactivityCondition(schedulerJobData)))
				.collect(Collectors.toList());
	}

	private boolean computationalInactivityCondition(SchedulerJobData jobData) {
		final SchedulerJobDTO schedulerData = jobData.getJobDTO();
		return schedulerData.isCheckInactivityRequired() && computationalInactivityExceed(jobData, schedulerData);
	}

	private boolean computationalInactivityExceed(SchedulerJobData schedulerJobData, SchedulerJobDTO schedulerData) {
		final String explName = schedulerJobData.getExploratoryName();
		final String compName = schedulerJobData.getComputationalName();
		final String user = schedulerJobData.getUser();
		final UserComputationalResource c = computationalDAO.fetchComputationalFields(user, explName, compName);
		final Long maxInactivity = schedulerData.getMaxInactivity();
		return inactivityCondition(maxInactivity, c.getStatus(), c.getLastActivity());
	}

	private boolean exploratoryInactivityCondition(SchedulerJobData jobData) {
		final SchedulerJobDTO schedulerData = jobData.getJobDTO();
		return schedulerData.isCheckInactivityRequired() && exploratoryInactivityExceed(jobData, schedulerData);
	}

	private boolean exploratoryInactivityExceed(SchedulerJobData schedulerJobData, SchedulerJobDTO schedulerData) {
		final String expName = schedulerJobData.getExploratoryName();
		final String user = schedulerJobData.getUser();
		final UserInstanceDTO userInstanceDTO = exploratoryDAO.fetchExploratoryFields(user, expName, true);
		final boolean canBeStopped = userInstanceDTO.getResources()
				.stream()
				.map(UserComputationalResource::getStatus)
				.map(UserInstanceStatus::of)
				.noneMatch(status -> status.in(TERMINATING, CONFIGURING, CREATING, CREATING));
		return canBeStopped && inactivityCondition(schedulerData.getMaxInactivity(), userInstanceDTO.getStatus(),
				userInstanceDTO.getLastActivity());
	}

	private boolean inactivityCondition(Long maxInactivity, String status, LocalDateTime lastActivity) {
		return UserInstanceStatus.RUNNING.toString().equals(status) &&
				Optional.ofNullable(lastActivity)
						.map(la -> la.plusMinutes(maxInactivity).isBefore(LocalDateTime.now()))
						.orElse(Boolean.FALSE);
	}

	private void populateDefaultSchedulerValues(SchedulerJobDTO dto) {
		if (Objects.isNull(dto.getBeginDate()) || StringUtils.isBlank(dto.getBeginDate().toString())) {
			dto.setBeginDate(LocalDate.now());
		}
		if (Objects.isNull(dto.getTimeZoneOffset()) || StringUtils.isBlank(dto.getTimeZoneOffset().toString())) {
			dto.setTimeZoneOffset(OffsetDateTime.now(systemDefault()).getOffset());
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
		if (Objects.isNull(status) || status.in(UserInstanceStatus.TERMINATED, TERMINATING,
				UserInstanceStatus.FAILED)) {
			throw new ResourceInappropriateStateException(String.format("Can not create/update scheduler for user " +
					"instance with status: %s", status));
		}
	}

	private boolean shouldSchedulerBeExecuted(SchedulerJobDTO dto, OffsetDateTime dateTime, List<DayOfWeek> daysRepeat,
											  LocalTime time) {
		LocalDateTime convertedDateTime = localDateTimeAtZone(dateTime, dto.getTimeZoneOffset());

		return isSchedulerActive(dto, convertedDateTime)
				&& daysRepeat.contains(convertedDateTime.toLocalDate().getDayOfWeek())
				&& convertedDateTime.toLocalTime().equals(time);
	}

	private boolean isSchedulerActive(SchedulerJobDTO dto, LocalDateTime convertedDateTime) {
		return !convertedDateTime.toLocalDate().isBefore(dto.getBeginDate())
				&& finishDateAfterCurrentDate(dto, convertedDateTime);
	}

	private LocalDateTime localDateTimeAtZone(OffsetDateTime dateTime, ZoneOffset timeZoneOffset) {
		return dateTime.atZoneSameInstant(ZoneOffset.UTC)
				.truncatedTo(ChronoUnit.MINUTES)
				.withZoneSameInstant(timeZoneOffset)
				.toLocalDateTime();
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

