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

package com.epam.dlab.backendapi.service;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.SystemUserInfoServiceImpl;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SchedulerJobDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class SchedulerJobService {

	@Inject
	private SchedulerJobDAO schedulerJobDAO;

	@Inject
	private ExploratoryDAO exploratoryDAO;

	@Inject
	private ExploratoryService exploratoryService;

	@Inject
	private SystemUserInfoServiceImpl systemUserService;


	/**
	 * Pulls out scheduler job data for user <code>user<code/> and his exploratory <code>exploratoryName<code/>
	 *
	 * @param user            user's name
	 * @param exploratoryName name of exploratory resource
	 * @return dto object
	 */
	@SuppressWarnings("unchecked")
	public SchedulerJobDTO fetchSchedulerJobForUserAndExploratory(String user, String exploratoryName) {
		return schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(user, exploratoryName);
	}

	/**
	 * Pulls out scheduler jobs data to achieve target exploratory status (running/stopped)
	 *
	 * @param desiredStatus       target exploratory status (running/stopped)
	 * @param dateTime            actual date with time
	 * @return list of scheduler jobs data
	 */
	@SuppressWarnings("unchecked")
	private List<SchedulerJobData> getSchedulerJobsToAchieveStatus(UserInstanceStatus desiredStatus,
																   OffsetDateTime dateTime) {
		return schedulerJobDAO.getSchedulerJobsToAchieveStatus(desiredStatus, dateTime);
	}

	/**
	 * Updates scheduler job data for user <code>user<code/> and his exploratory <code>exploratoryName<code/>
	 *
	 * @param user            user's name
	 * @param exploratoryName name of exploratory resource
	 * @param dto             scheduler job data
	 */
	@SuppressWarnings("unchecked")
	public void updateSchedulerDataForUserAndExploratory(String user, String exploratoryName, SchedulerJobDTO dto) {
		exploratoryDAO.updateSchedulerDataForUserAndExploratory(user, exploratoryName, dto);
	}

	/**
	 * Pulls out scheduler jobs data for following stopping corresponding exploratories
	 *
	 * @param currentDateTime actual date with time
	 * @return list of scheduler jobs data
	 */
	private List<SchedulerJobData> getSchedulerJobsForStoppingExploratories(OffsetDateTime currentDateTime) {
		return Stream.concat(
				getSchedulerJobsToAchieveStatus(UserInstanceStatus.STOPPED, currentDateTime)
						.stream()
						.filter(jobData ->
								jobData.getJobDTO().getEndTime().isAfter(jobData.getJobDTO().getStartTime())),
				getSchedulerJobsToAchieveStatus(UserInstanceStatus.STOPPED, currentDateTime.minusDays(1))
						.stream()
						.filter(jobData -> {
							LocalDateTime convertedDateTime = ZonedDateTime.ofInstant(currentDateTime.toInstant(),
									ZoneId.ofOffset(SchedulerJobDAO.TIMEZONE_PREFIX, jobData.getJobDTO()
											.getTimeZoneOffset()))
									.toLocalDateTime();
							return jobData.getJobDTO().getEndTime().isBefore(jobData.getJobDTO().getStartTime())
									&& !convertedDateTime.toLocalDate().isAfter(jobData.getJobDTO().getFinishDate());
						})
		).collect(Collectors.toList());
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
	 * Starts/stops exploratory corresponding to target status and scheduler job data
	 *
	 * @param desiredStatus target exploratory status (running/stopped)
	 * @param jobData       scheduler job data which includes exploratory details
	 */
	private void changeExploratoryStatusTo(UserInstanceStatus desiredStatus, SchedulerJobData jobData) {
		log.debug("Exploratory with name {} for user {} is {}...",
				jobData.getExploratoryName(), jobData.getUser(),
				(desiredStatus.equals(UserInstanceStatus.RUNNING) ? UserInstanceStatus.STARTING : UserInstanceStatus.STOPPING));
		UserInfo userInfo = systemUserService.create(jobData.getUser());
		String uuid = desiredStatus.equals(UserInstanceStatus.RUNNING) ?
				exploratoryService.start(userInfo, jobData.getExploratoryName()) :
				exploratoryService.stop(userInfo, jobData.getExploratoryName());
		RequestId.put(userInfo.getName(), uuid);
	}

	/**
	 * Executes start scheduler job for corresponding exploratories
	 */
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

	/**
	 * Executes stop scheduler job for corresponding exploratories
	 */
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

}

