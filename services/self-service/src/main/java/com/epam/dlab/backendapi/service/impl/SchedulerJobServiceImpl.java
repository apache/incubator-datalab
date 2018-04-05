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

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.SystemUserInfoServiceImpl;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SchedulerJobDAO;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.SchedulerJobService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.exceptions.ResourceInappropriateStateException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class SchedulerJobServiceImpl implements SchedulerJobService {

	private static final String SCHEDULER_NOT_FOUND_MSG =
			"Scheduler job data not found for user %s with exploratory %s";
	private static final String CURRENT_DATETIME_INFO =
			"Current time rounded: {} , current date: {}, current day of week: {}";

	@Inject
	private SchedulerJobDAO schedulerJobDAO;

	@Inject
	private ExploratoryDAO exploratoryDAO;

	@Inject
	private ExploratoryService exploratoryService;

	@Inject
	private SystemUserInfoServiceImpl systemUserService;

	@Override
	public SchedulerJobDTO fetchSchedulerJobForUserAndExploratory(String user, String exploratoryName) {
		if (!exploratoryDAO.isExploratoryExist(user, exploratoryName)) {
			throw new ResourceNotFoundException(String.format(ExploratoryDAO.EXPLORATORY_NOT_FOUND_MSG, user,
					exploratoryName));
		}
		return schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(user, exploratoryName)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(SCHEDULER_NOT_FOUND_MSG, user,
						exploratoryName)));
	}

	@Override
	public void updateSchedulerDataForUserAndExploratory(String user, String exploratoryName, SchedulerJobDTO dto) {
		final UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(user, exploratoryName);
		final UserInstanceStatus status = UserInstanceStatus.of(userInstance.getStatus());
		if (Objects.isNull(status) || status.in(UserInstanceStatus.TERMINATED, UserInstanceStatus.TERMINATING,
				UserInstanceStatus.FAILED)) {
			throw new ResourceInappropriateStateException(String.format("Can not create/update scheduler for user " +
					"instance with status: %s", status));
		}
		enrichSchedulerJobIfNecessary(dto);
		log.debug("Updating exploratory {} for user {} with new scheduler job data {}...",
				exploratoryName, user, dto);
		exploratoryDAO.updateSchedulerDataForUserAndExploratory(user, exploratoryName, dto);
	}

	@Override
	public void executeStartExploratoryJob() {
		OffsetDateTime currentDateTime = OffsetDateTime.now();
		List<SchedulerJobData> jobsToStart =
				getSchedulerJobsForExploratoryAction(UserInstanceStatus.RUNNING, currentDateTime);
		if (!jobsToStart.isEmpty()) {
			log.debug("Scheduler start job is executing...");
			log.info(CURRENT_DATETIME_INFO, LocalTime.of(currentDateTime.toLocalTime().getHour(),
					currentDateTime.toLocalTime().getMinute()), currentDateTime.toLocalDate(),
					currentDateTime.getDayOfWeek());
			log.info("Quantity of exploratories for starting: {}", jobsToStart.size());
			jobsToStart.forEach(job -> changeExploratoryStatusTo(UserInstanceStatus.RUNNING, job));
		}
	}

	@Override
	public void executeStopExploratoryJob() {
		OffsetDateTime currentDateTime = OffsetDateTime.now();
		List<SchedulerJobData> jobsToStop =
				getSchedulerJobsForExploratoryAction(UserInstanceStatus.STOPPED, currentDateTime);
		if (!jobsToStop.isEmpty()) {
			log.debug("Scheduler stop job is executing...");
			log.info(CURRENT_DATETIME_INFO, LocalTime.of(currentDateTime.toLocalTime().getHour(),
					currentDateTime.toLocalTime().getMinute()), currentDateTime.toLocalDate(),
					currentDateTime.getDayOfWeek());
			log.info("Quantity of exploratories for stopping: {}", jobsToStop.size());
			jobsToStop.forEach(job -> changeExploratoryStatusTo(UserInstanceStatus.STOPPED, job));
		}
	}

	@Override
	public void executeTerminateExploratoryJob() {
		OffsetDateTime currentDateTime = OffsetDateTime.now();
		List<SchedulerJobData> jobsToTerminate =
				getSchedulerJobsForExploratoryAction(UserInstanceStatus.TERMINATED, currentDateTime);
		if (!jobsToTerminate.isEmpty()) {
			log.debug("Scheduler terminate job is executing...");
			log.info(CURRENT_DATETIME_INFO, LocalTime.of(currentDateTime.toLocalTime().getHour(),
					currentDateTime.toLocalTime().getMinute()), currentDateTime.toLocalDate(),
					currentDateTime.getDayOfWeek());
			log.info("Quantity of exploratories for terminating: {}", jobsToTerminate.size());
			jobsToTerminate.forEach(job -> changeExploratoryStatusTo(UserInstanceStatus.TERMINATED, job));
		}
	}

	/**
	 * Pulls out scheduler jobs data to achieve target exploratory status (running/stopped)
	 *
	 * @param desiredStatus target exploratory status (running/stopped)
	 * @param dateTime      actual date with time
	 * @return list of scheduler jobs data
	 */
	private List<SchedulerJobData> getSchedulerJobsToAchieveStatus(UserInstanceStatus desiredStatus,
																   OffsetDateTime dateTime) {
		return schedulerJobDAO.getSchedulerJobsToAchieveStatus(desiredStatus, dateTime);
	}

	/**
	 * Pulls out scheduler jobs data for the following starting/terminating/stopping corresponding exploratories.
	 *
	 * @param desiredStatus actual date with time
	 * @param currentDateTime actual date with time
	 * @return list of scheduler jobs data
	 */
	private List<SchedulerJobData> getSchedulerJobsForExploratoryAction(UserInstanceStatus desiredStatus,
																		OffsetDateTime currentDateTime) {
		switch (desiredStatus) {
			case RUNNING:
				return getSchedulerJobsToAchieveStatus(desiredStatus, currentDateTime);
			case TERMINATED:
				return getSchedulerJobsToAchieveStatus(desiredStatus, currentDateTime);
			case STOPPED:
				return Stream.of(
						getSchedulerJobsToAchieveStatus(desiredStatus, currentDateTime)
								.stream()
								.filter(jobData -> Objects.nonNull(jobData.getJobDTO().getStartTime()) &&
										jobData.getJobDTO().getEndTime().isAfter(jobData.getJobDTO().getStartTime())),
						getSchedulerJobsToAchieveStatus(desiredStatus, currentDateTime.minusDays(1))
								.stream()
								.filter(jobData -> {
									LocalDateTime convertedDateTime = ZonedDateTime.ofInstant(currentDateTime
													.toInstant(),
											ZoneId.ofOffset(SchedulerJobDAO.TIMEZONE_PREFIX, jobData.getJobDTO()
													.getTimeZoneOffset())).toLocalDateTime();
									return Objects.nonNull(jobData.getJobDTO().getStartTime()) &&
											jobData.getJobDTO().getEndTime().isBefore(jobData.getJobDTO()
													.getStartTime())
											&& !convertedDateTime.toLocalDate().isAfter(jobData.getJobDTO()
											.getFinishDate());
								}),
						getSchedulerJobsToAchieveStatus(desiredStatus, currentDateTime).stream()
								.filter(jobData -> Objects.isNull(jobData.getJobDTO().getStartTime()))
				).flatMap(Function.identity()).collect(Collectors.toList());
			default:
				return Collections.emptyList();
		}
	}

	/**
	 * Starts/stops/terminates exploratory corresponding to target status and scheduler job data.
	 *
	 * @param desiredStatus target exploratory status (running/stopped/terminated)
	 * @param jobData       scheduler job data which includes exploratory details
	 */
	private void changeExploratoryStatusTo(UserInstanceStatus desiredStatus, SchedulerJobData jobData) {
		log.debug("Exploratory with name {} for user {} is {}...", jobData.getExploratoryName(), jobData.getUser(),
				getActionBasedOnDesiredStatus(desiredStatus));
		UserInfo userInfo = systemUserService.create(jobData.getUser());
		executeCorrespondingAction(userInfo, jobData.getExploratoryName(), desiredStatus);
	}

	private UserInstanceStatus getActionBasedOnDesiredStatus(UserInstanceStatus desiredStatus) {
		switch (desiredStatus) {
			case RUNNING:
				return UserInstanceStatus.STARTING;
			case STOPPED:
				return UserInstanceStatus.STOPPING;
			case TERMINATED:
				return UserInstanceStatus.TERMINATING;
			default:
				return null;
		}
	}

	private void executeCorrespondingAction(UserInfo userInfo, String exploratoryName,
											UserInstanceStatus desiredStatus) {
		switch (desiredStatus) {
			case RUNNING:
				exploratoryService.start(userInfo, exploratoryName);
				break;
			case STOPPED:
				exploratoryService.stop(userInfo, exploratoryName);
				break;
			case TERMINATED:
				exploratoryService.terminate(userInfo, exploratoryName);
				break;
			default:
				break;
		}
	}

	/**
	 * Enriches existing scheduler job with the following data:
	 * - sets current date as 'beginDate' if this parameter wasn't defined;
	 * - sets '9999-12-31' as 'finishDate' if this parameter wasn't defined;
	 * - sets repeating days of existing scheduler job to all days of week if this parameter wasn't defined;
	 * - sets '9999-12-31 00:00' as 'terminateDateTime' if this parameter wasn't defined.
	 *
	 * @param dto current scheduler job
	 */
	private void enrichSchedulerJobIfNecessary(SchedulerJobDTO dto) {
		if (Objects.isNull(dto.getBeginDate()) || StringUtils.isBlank(dto.getBeginDate().toString())) {
			dto.setBeginDate(LocalDate.now());
		}
		if (Objects.isNull(dto.getFinishDate()) || StringUtils.isBlank(dto.getFinishDate().toString())) {
			dto.setFinishDate(LocalDate.of(9999, 12, 31));
		}
		if (Objects.isNull(dto.getDaysRepeat()) || dto.getDaysRepeat().isEmpty()) {
			dto.setDaysRepeat(Arrays.asList(DayOfWeek.values()));
		}
		if (Objects.isNull(dto.getTerminateDateTime()) || StringUtils.isBlank(dto.getTerminateDateTime().toString())) {
			dto.setTerminateDateTime(LocalDateTime.of(9999, 12, 31, 0, 0));
		}
	}
}

