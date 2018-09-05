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
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SchedulerJobDAO;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.SchedulerJobService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.exceptions.ResourceInappropriateStateException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.epam.dlab.backendapi.dao.SchedulerJobDAO.TIMEZONE_PREFIX;
import static com.epam.dlab.dto.UserInstanceStatus.*;

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
	private ComputationalDAO computationalDAO;

	@Inject
	private ExploratoryService exploratoryService;

	@Inject
	private ComputationalService computationalService;

	@Inject
	private SystemUserInfoService systemUserService;

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
	public SchedulerJobDTO fetchSchedulerJobForComputationalResource(String user, String exploratoryName,
																	 String computationalName) {
		if (!computationalDAO.isComputationalExist(user, exploratoryName, computationalName)) {
			throw new ResourceNotFoundException(String.format(ComputationalDAO.COMPUTATIONAL_NOT_FOUND_MSG,
					computationalName, exploratoryName, user));
		}
		return schedulerJobDAO.fetchSingleSchedulerJobForCluster(user, exploratoryName, computationalName)
				.orElseThrow(() -> new ResourceNotFoundException(String.format(SCHEDULER_NOT_FOUND_MSG, user,
						exploratoryName) + " with computational resource " + computationalName));
	}

	@Override
	public void updateExploratorySchedulerData(String user, String exploratoryName, SchedulerJobDTO dto) {
		checkExploratoryStatusOrElseThrowException(user, exploratoryName);
		enrichSchedulerJobIfNecessary(dto);
		log.debug("Updating exploratory {} for user {} with new scheduler job data: {}...",
				exploratoryName, user, nullableJobDto(dto));
		exploratoryDAO.updateSchedulerDataForUserAndExploratory(user, exploratoryName, nullableJobDto(dto));
		if (dto.isSyncStartRequired()) {
			shareSchedulerJobDataToSparkClusters(user, exploratoryName, dto);
		} else {
			computationalDAO.updateSchedulerSyncFlag(user, exploratoryName, dto.isSyncStartRequired());
		}
	}

	@Override
	public void updateComputationalSchedulerData(String user, String exploratoryName, String computationalName,
												 SchedulerJobDTO dto) {
		checkExploratoryStatusOrElseThrowException(user, exploratoryName);
		checkComputationalStatusOrElseThrowException(user, exploratoryName, computationalName);
		enrichSchedulerJobIfNecessary(dto);
		log.debug("Updating computational resource {} affiliated with exploratory {} for user {} with new scheduler " +
				"job data {}...", computationalName, exploratoryName, user, nullableJobDto(dto));
		computationalDAO.updateSchedulerDataForComputationalResource(user, exploratoryName, computationalName,
				nullableJobDto(dto));
	}

	@Override
	public void executeStartResourceJob(boolean isAppliedForClusters) {
		OffsetDateTime currentDateTime = OffsetDateTime.now();
		List<SchedulerJobData> jobsToStart =
				getSchedulerJobsForAction(UserInstanceStatus.RUNNING, currentDateTime, isAppliedForClusters);
		if (!jobsToStart.isEmpty()) {
			log.debug(isAppliedForClusters ? "Scheduler computational resource start job is executing..." :
					"Scheduler exploratory start job is executing...");
			log.info(CURRENT_DATETIME_INFO, LocalTime.of(currentDateTime.toLocalTime().getHour(),
					currentDateTime.toLocalTime().getMinute()), currentDateTime.toLocalDate(),
					currentDateTime.getDayOfWeek());
			log.info(isAppliedForClusters ? "Quantity of clusters for starting: {}" :
					"Quantity of exploratories for starting: {}", jobsToStart.size());
			jobsToStart.forEach(job -> changeResourceStatusTo(UserInstanceStatus.RUNNING, job, isAppliedForClusters));
		}

	}

	@Override
	public void executeStopResourceJob(boolean isAppliedForClusters) {
		OffsetDateTime currentDateTime = OffsetDateTime.now();
		List<SchedulerJobData> jobsToStop =
				getSchedulerJobsForAction(STOPPED, currentDateTime, isAppliedForClusters);
		if (!jobsToStop.isEmpty()) {
			log.debug(isAppliedForClusters ? "Scheduler computational resource stop job is executing..." :
					"Scheduler exploratory stop job is executing...");
			log.info(CURRENT_DATETIME_INFO, LocalTime.of(currentDateTime.toLocalTime().getHour(),
					currentDateTime.toLocalTime().getMinute()), currentDateTime.toLocalDate(),
					currentDateTime.getDayOfWeek());
			log.info(isAppliedForClusters ? "Quantity of clusters for stopping: {}" :
					"Quantity of exploratories for stopping: {}", jobsToStop.size());
			jobsToStop.forEach(job -> changeResourceStatusTo(STOPPED, job, isAppliedForClusters));
		}
	}

	@Override
	public void executeTerminateResourceJob(boolean isAppliedForClusters) {
		OffsetDateTime currentDateTime = OffsetDateTime.now();
		List<SchedulerJobData> jobsToTerminate =
				getSchedulerJobsForAction(UserInstanceStatus.TERMINATED, currentDateTime, isAppliedForClusters);
		if (!jobsToTerminate.isEmpty()) {
			log.debug(isAppliedForClusters ? "Scheduler computational resource terminate job is executing..." :
					"Scheduler exploratory terminate job is executing...");
			log.info(CURRENT_DATETIME_INFO, LocalTime.of(currentDateTime.toLocalTime().getHour(),
					currentDateTime.toLocalTime().getMinute()), currentDateTime.toLocalDate(),
					currentDateTime.getDayOfWeek());
			log.info(isAppliedForClusters ? "Quantity of clusters for terminating: {}" :
					"Quantity of exploratories for terminating: {}", jobsToTerminate.size());
			jobsToTerminate.forEach(job ->
					changeResourceStatusTo(UserInstanceStatus.TERMINATED, job, isAppliedForClusters));
		}
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
				Collections.singletonList(DataEngineType.SPARK_STANDALONE), exploratoryName,
				STARTING, RUNNING, STOPPING, STOPPED);
		SchedulerJobDTO dtoWithoutStopData = getSchedulerJobWithoutStopData(dto);
		for (String sparkName : correspondingSparkClusters) {
			log.debug("Updating computational resource {} affiliated with exploratory {} for user {} with new " +
					"scheduler job data {}...", sparkName, exploratoryName, user, nullableJobDto(dtoWithoutStopData));
			computationalDAO.updateSchedulerDataForComputationalResource(user, exploratoryName, sparkName,
					nullableJobDto(dtoWithoutStopData));
		}
	}


	/**
	 * Pulls out scheduler jobs data to achieve target exploratory ('isAppliedForClusters' equals 'false') or
	 * computational ('isAppliedForClusters' equals 'true') status running/stopped/terminated.
	 *
	 * @param desiredStatus        target exploratory/cluster status (running/stopped/terminated)
	 * @param dateTime             actual date with time
	 * @param isAppliedForClusters true/false
	 * @return list of scheduler jobs data
	 */
	private List<SchedulerJobData> getSchedulerJobsToAchieveStatus(UserInstanceStatus desiredStatus,
																   OffsetDateTime dateTime,
																   boolean isAppliedForClusters) {
		return schedulerJobDAO.getSchedulerJobsToAchieveStatus(desiredStatus, isAppliedForClusters).stream()
				.filter(jobData -> isSchedulerJobDtoSatisfyCondition(jobData.getJobDTO(), dateTime, desiredStatus))
				.collect(Collectors.toList());
	}


	/**
	 * Pulls out scheduler jobs data for the following starting/terminating/stopping corresponding exploratories
	 * ('isAppliedForClusters' equals 'false') or computational resources ('isAppliedForClusters' equals 'true').
	 *
	 * @param desiredStatus        target exploratory/cluster status (running/stopped/terminated)
	 * @param currentDateTime      actual date with time
	 * @param isAppliedForClusters true/false
	 * @return list of scheduler jobs data
	 */
	private List<SchedulerJobData> getSchedulerJobsForAction(UserInstanceStatus desiredStatus,
															 OffsetDateTime currentDateTime,
															 boolean isAppliedForClusters) {
		return desiredStatus.in(STOPPED, RUNNING, TERMINATED) ?
				getSchedulerJobsToAchieveStatus(desiredStatus, currentDateTime, isAppliedForClusters) :
				Collections.emptyList();
	}


	/**
	 * Starts/stops/terminates exploratory ('isAppliedForClusters' equals 'false') or computational resource
	 * ('isAppliedForClusters' equals 'true') corresponding to target status and scheduler job data.
	 *
	 * @param desiredStatus target exploratory/computational status (running/stopped/terminated)
	 * @param jobData       scheduler job data which includes exploratory details
	 */
	private void changeResourceStatusTo(UserInstanceStatus desiredStatus, SchedulerJobData jobData,
										boolean isAppliedForClusters) {
		log.debug(String.format(isAppliedForClusters ? "Computational resource " + jobData.getComputationalName() +
						" affiliated with exploratory %s and user %s is %s..." :
						"Exploratory with name %s for user %s is %s...",
				jobData.getExploratoryName(), jobData.getUser(), getActionBasedOnDesiredStatus(desiredStatus)));
		UserInfo userInfo = systemUserService.create(jobData.getUser());
		if (isAppliedForClusters) {
			executeComputationalAction(userInfo, jobData.getExploratoryName(), jobData.getComputationalName(),
					desiredStatus);
		} else {
			executeExploratoryAction(userInfo, jobData.getExploratoryName(), desiredStatus);
		}
	}

	private UserInstanceStatus getActionBasedOnDesiredStatus(UserInstanceStatus desiredStatus) {
		if (desiredStatus == RUNNING) {
			return UserInstanceStatus.STARTING;
		} else if (desiredStatus == STOPPED) {
			return UserInstanceStatus.STOPPING;
		} else if (desiredStatus == TERMINATED) {
			return UserInstanceStatus.TERMINATING;
		} else return null;
	}

	private void executeExploratoryAction(UserInfo userInfo, String exploratoryName,
										  UserInstanceStatus desiredStatus) {
		if (desiredStatus == RUNNING) {
			exploratoryService.start(userInfo, exploratoryName);
			List<String> computationalResourcesForStartingWithExploratory =
					getComputationalResourcesForStartingWithExploratory(userInfo.getName(), exploratoryName);
			computationalResourcesForStartingWithExploratory.forEach(compName -> {
				UserInfo user = systemUserService.create(userInfo.getName());
				computationalService.startSparkCluster(user, exploratoryName, compName);
			});
		} else if (desiredStatus == STOPPED) {
			exploratoryService.stop(userInfo, exploratoryName);
		} else if (desiredStatus == TERMINATED) {
			exploratoryService.terminate(userInfo, exploratoryName);
		}
	}

	private void executeComputationalAction(UserInfo userInfo, String exploratoryName, String computationalName,
											UserInstanceStatus desiredStatus) {
		if (desiredStatus == RUNNING) {
			computationalService.startSparkCluster(userInfo, exploratoryName, computationalName);
		} else if (desiredStatus == STOPPED) {
			computationalService.stopSparkCluster(userInfo, exploratoryName, computationalName);
		} else if (desiredStatus == TERMINATED) {
			computationalService.terminateComputationalEnvironment(userInfo, exploratoryName, computationalName);
		}
	}

	private List<String> getComputationalResourcesForStartingWithExploratory(String user, String exploratoryName) {
		Optional<SchedulerJobDTO> schedulerJobForExploratory = schedulerJobDAO
				.fetchSingleSchedulerJobByUserAndExploratory(user, exploratoryName);
		if (!schedulerJobForExploratory.isPresent() || !schedulerJobForExploratory.get().isSyncStartRequired()) {
			return Collections.emptyList();
		}
		return computationalDAO.getComputationalResourcesWhereStatusIn(user,
				Collections.singletonList(DataEngineType.SPARK_STANDALONE), exploratoryName, STOPPED).stream()
				.filter(clusterName -> isClusterSchedulerPresentAndEqualToAnotherForSyncStarting(user, exploratoryName,
						clusterName, schedulerJobForExploratory.get())).collect(Collectors.toList());
	}

	private boolean isClusterSchedulerPresentAndEqualToAnotherForSyncStarting(String user, String exploratoryName,
																			  String clusterName,
																			  SchedulerJobDTO dto) {
		Optional<SchedulerJobDTO> schedulerJobForCluster =
				schedulerJobDAO.fetchSingleSchedulerJobForCluster(user, exploratoryName, clusterName);
		return schedulerJobForCluster.isPresent() &&
				areSchedulersEqualForSyncStarting(dto, schedulerJobForCluster.get());
	}

	@SuppressWarnings("unchecked")
	private boolean areCollectionsEqual(Collection col1, Collection col2) {
		return col1.containsAll(col2) && col2.containsAll(col1);
	}

	private boolean areSchedulersEqualForSyncStarting(SchedulerJobDTO notebookScheduler,
													  SchedulerJobDTO clusterScheduler) {
		return !Objects.isNull(notebookScheduler) && !Objects.isNull(clusterScheduler) &&
				notebookScheduler.getBeginDate().equals(clusterScheduler.getBeginDate()) &&
				notebookScheduler.getStartTime().equals(clusterScheduler.getStartTime()) &&
				areCollectionsEqual(notebookScheduler.getStartDaysRepeat(), clusterScheduler.getStartDaysRepeat())
				&& notebookScheduler.getTimeZoneOffset().equals(clusterScheduler.getTimeZoneOffset()) &&
				notebookScheduler.isSyncStartRequired() && clusterScheduler.isSyncStartRequired();
	}

	/**
	 * Enriches existing scheduler job with the following data:
	 * - sets current date as 'beginDate' if this parameter wasn't defined;
	 * - sets current system time zone offset as 'timeZoneOffset' if this parameter wasn't defined.
	 *
	 * @param dto current scheduler job
	 */
	private void enrichSchedulerJobIfNecessary(SchedulerJobDTO dto) {
		if (Objects.isNull(dto.getBeginDate()) || StringUtils.isBlank(dto.getBeginDate().toString())) {
			dto.setBeginDate(LocalDate.now());
		}
		if (Objects.isNull(dto.getTimeZoneOffset()) || StringUtils.isBlank(dto.getTimeZoneOffset().toString())) {
			dto.setTimeZoneOffset(OffsetDateTime.now(ZoneId.systemDefault()).getOffset());
		}
	}

	private void checkExploratoryStatusOrElseThrowException(String user, String exploratoryName) {
		final UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(user, exploratoryName);
		checkResourceStatusOrElseThrowException(userInstance.getStatus());
	}

	private void checkComputationalStatusOrElseThrowException(String user, String exploratoryName,
															  String computationalName) {
		final UserComputationalResource computationalResource =
				computationalDAO.fetchComputationalFields(user, exploratoryName, computationalName);
		final String computationalStatus = computationalResource.getStatus();
		checkResourceStatusOrElseThrowException(computationalStatus);
	}

	private void checkResourceStatusOrElseThrowException(String resourceStatus) {
		final UserInstanceStatus status = UserInstanceStatus.of(resourceStatus);
		if (Objects.isNull(status) || status.in(UserInstanceStatus.TERMINATED, UserInstanceStatus.TERMINATING,
				UserInstanceStatus.FAILED)) {
			throw new ResourceInappropriateStateException(String.format("Can not create/update scheduler for user " +
					"instance with status: %s", status));
		}
	}

	/**
	 * Checks if scheduler's time data satisfies existing time parameters.
	 *
	 * @param dto           scheduler job data.
	 * @param dateTime      existing time data.
	 * @param desiredStatus target exploratory status which has influence for time/date checking ('running' status
	 *                      requires for checking start time, 'stopped' - for end time, 'terminated' - for
	 *                      'terminatedDateTime').
	 * @return true/false.
	 */
	private boolean isSchedulerJobDtoSatisfyCondition(SchedulerJobDTO dto, OffsetDateTime dateTime,
													  UserInstanceStatus desiredStatus) {
		ZoneOffset zOffset = dto.getTimeZoneOffset();
		OffsetDateTime roundedDateTime = OffsetDateTime.of(
				dateTime.toLocalDate(),
				LocalTime.of(dateTime.toLocalTime().getHour(), dateTime.toLocalTime().getMinute()),
				dateTime.getOffset());

		LocalDateTime convertedDateTime = ZonedDateTime.ofInstant(roundedDateTime.toInstant(),
				ZoneId.ofOffset(TIMEZONE_PREFIX, zOffset)).toLocalDateTime();

		return desiredStatus == TERMINATED ?
				Objects.nonNull(dto.getTerminateDateTime()) &&
						convertedDateTime.toLocalDate().equals(dto.getTerminateDateTime().toLocalDate())
						&& convertedDateTime.toLocalTime().equals(getDesiredTime(dto, desiredStatus)) :
				!convertedDateTime.toLocalDate().isBefore(dto.getBeginDate())
						&& isFinishDateMatchesCondition(dto, convertedDateTime)
						&& getDaysRepeat(dto, desiredStatus).contains(convertedDateTime.toLocalDate().getDayOfWeek())
						&& convertedDateTime.toLocalTime().equals(getDesiredTime(dto, desiredStatus));
	}

	private List<DayOfWeek> getDaysRepeat(SchedulerJobDTO dto, UserInstanceStatus desiredStatus) {
		if (desiredStatus == RUNNING) {
			return dto.getStartDaysRepeat();
		} else if (desiredStatus == STOPPED) {
			return dto.getStopDaysRepeat();
		} else return Collections.emptyList();
	}

	private boolean isFinishDateMatchesCondition(SchedulerJobDTO dto, LocalDateTime currentDateTime) {
		return dto.getFinishDate() == null || !currentDateTime.toLocalDate().isAfter(dto.getFinishDate());
	}

	private LocalTime getDesiredTime(SchedulerJobDTO dto, UserInstanceStatus desiredStatus) {
		if (desiredStatus == RUNNING) {
			return dto.getStartTime();
		} else if (desiredStatus == STOPPED) {
			return dto.getEndTime();
		} else if (desiredStatus == TERMINATED) {
			return dto.getTerminateDateTime().toLocalTime();
		} else return null;
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

	private SchedulerJobDTO nullableJobDto(SchedulerJobDTO dto) {
		return (Objects.isNull(dto.getStartDaysRepeat()) || dto.getStartDaysRepeat().isEmpty()) &&
				(Objects.isNull(dto.getStopDaysRepeat()) || dto.getStopDaysRepeat().isEmpty()) ? null : dto;
	}

}

