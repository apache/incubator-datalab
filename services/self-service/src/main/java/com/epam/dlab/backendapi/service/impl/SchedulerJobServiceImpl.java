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
		List<SchedulerJobData> jobsToStart = getSchedulerJobsForStartingExploratories(currentDateTime);
		if (!jobsToStart.isEmpty()) {
			log.debug("Scheduler start job is executing...");
			log.info("Current time rounded: {} , current date: {}, current day of week: {}",
					LocalTime.of(currentDateTime.toLocalTime().getHour(), currentDateTime.toLocalTime().getMinute()),
					currentDateTime.toLocalDate(),
					currentDateTime.getDayOfWeek());
			log.info("Quantity of exploratories for starting: {}", jobsToStart.size());
			jobsToStart.forEach(job -> changeExploratoryStatusTo(UserInstanceStatus.RUNNING, job));
		}
	}

	@Override
	public void executeStopExploratoryJob() {
		OffsetDateTime currentDateTime = OffsetDateTime.now();
		List<SchedulerJobData> jobsToStop = getSchedulerJobsForStoppingExploratories(currentDateTime);
		if (!jobsToStop.isEmpty()) {
			log.debug("Scheduler stop job is executing...");
			log.info("Current time rounded: {} , current date: {}, current day of week: {}",
					LocalTime.of(currentDateTime.toLocalTime().getHour(), currentDateTime.toLocalTime().getMinute()),
					currentDateTime.toLocalDate(),
					currentDateTime.getDayOfWeek());
			log.info("Quantity of exploratories for stopping: {}", jobsToStop.size());
			jobsToStop.forEach(job -> changeExploratoryStatusTo(UserInstanceStatus.STOPPED, job));
		}
	}

	@Override
	public void executeTerminateExploratoryJob() {
		OffsetDateTime currentDateTime = OffsetDateTime.now();
		List<SchedulerJobData> jobsToTerminate = getSchedulerJobsForTerminatingExploratories(currentDateTime);
		if (!jobsToTerminate.isEmpty()) {
			log.debug("Scheduler terminate job is executing...");
			log.info("Current time rounded: {} , current date: {}, current day of week: {}",
					LocalTime.of(currentDateTime.toLocalTime().getHour(), currentDateTime.toLocalTime().getMinute()),
					currentDateTime.toLocalDate(),
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
	 * Pulls out scheduler jobs data for following stopping corresponding exploratories
	 *
	 * @param currentDateTime actual date with time
	 * @return list of scheduler jobs data
	 */
	private List<SchedulerJobData> getSchedulerJobsForStoppingExploratories(OffsetDateTime currentDateTime) {
		return Stream.of(
				getSchedulerJobsToAchieveStatus(UserInstanceStatus.STOPPED, currentDateTime)
						.stream()
						.filter(jobData -> Objects.nonNull(jobData.getJobDTO().getStartTime()) &&
								jobData.getJobDTO().getEndTime().isAfter(jobData.getJobDTO().getStartTime())),
				getSchedulerJobsToAchieveStatus(UserInstanceStatus.STOPPED, currentDateTime.minusDays(1))
						.stream()
						.filter(jobData -> {
							LocalDateTime convertedDateTime = ZonedDateTime.ofInstant(currentDateTime.toInstant(),
									ZoneId.ofOffset(SchedulerJobDAO.TIMEZONE_PREFIX, jobData.getJobDTO()
											.getTimeZoneOffset())).toLocalDateTime();
							return Objects.nonNull(jobData.getJobDTO().getStartTime()) &&
									jobData.getJobDTO().getEndTime().isBefore(jobData.getJobDTO().getStartTime())
									&& !convertedDateTime.toLocalDate().isAfter(jobData.getJobDTO().getFinishDate());
						}),
				getSchedulerJobsToAchieveStatus(UserInstanceStatus.STOPPED, currentDateTime).stream()
						.filter(jobData -> Objects.isNull(jobData.getJobDTO().getStartTime()))
		).flatMap(Function.identity()).collect(Collectors.toList());
	}

	/**
	 * Pulls out scheduler jobs data for following starting corresponding exploratories
	 *
	 * @param currentDateTime actual date with time
	 * @return list of scheduler jobs data
	 */
	private List<SchedulerJobData> getSchedulerJobsForStartingExploratories(OffsetDateTime currentDateTime) {
		return getSchedulerJobsToAchieveStatus(UserInstanceStatus.RUNNING, currentDateTime);
	}

	/**
	 * Pulls out scheduler jobs data for following terminating corresponding exploratories
	 *
	 * @param currentDateTime actual date with time
	 * @return list of scheduler jobs data
	 */
	private List<SchedulerJobData> getSchedulerJobsForTerminatingExploratories(OffsetDateTime currentDateTime) {
		return Stream.of(
				getSchedulerJobsToAchieveStatus(UserInstanceStatus.STOPPED, currentDateTime)
						.stream()
						.filter(jobData -> Objects.nonNull(jobData.getJobDTO().getStartTime()) &&
								jobData.getJobDTO().getEndTime().isAfter(jobData.getJobDTO().getStartTime())),
				getSchedulerJobsToAchieveStatus(UserInstanceStatus.STOPPED, currentDateTime.minusDays(1))
						.stream()
						.filter(jobData -> {
							LocalDateTime convertedDateTime = ZonedDateTime.ofInstant(currentDateTime.toInstant(),
									ZoneId.ofOffset(SchedulerJobDAO.TIMEZONE_PREFIX, jobData.getJobDTO()
											.getTimeZoneOffset())).toLocalDateTime();
							return Objects.nonNull(jobData.getJobDTO().getStartTime()) &&
									jobData.getJobDTO().getEndTime().isBefore(jobData.getJobDTO().getStartTime())
									&& !convertedDateTime.toLocalDate().isAfter(jobData.getJobDTO().getFinishDate());
						}),
				getSchedulerJobsToAchieveStatus(UserInstanceStatus.STOPPED, currentDateTime).stream()
						.filter(jobData -> Objects.isNull(jobData.getJobDTO().getStartTime()))
		).flatMap(Function.identity()).collect(Collectors.toList());
	}

	/**
	 * Starts/stops exploratory corresponding to target status and scheduler job data
	 *
	 * @param desiredStatus target exploratory status (running/stopped)
	 * @param jobData       scheduler job data which includes exploratory details
	 */
	private void changeExploratoryStatusTo(UserInstanceStatus desiredStatus, SchedulerJobData jobData) {
		log.debug("Exploratory with name {} for user {} is {}...",
				jobData.getExploratoryName(), jobData.getUser(),
				(desiredStatus.equals(UserInstanceStatus.RUNNING) ? UserInstanceStatus.STARTING :
						UserInstanceStatus.STOPPING));
		UserInfo userInfo = systemUserService.create(jobData.getUser());
		if (desiredStatus.equals(UserInstanceStatus.RUNNING)) {
			exploratoryService.start(userInfo, jobData.getExploratoryName());
		} else {
			exploratoryService.stop(userInfo, jobData.getExploratoryName());
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

