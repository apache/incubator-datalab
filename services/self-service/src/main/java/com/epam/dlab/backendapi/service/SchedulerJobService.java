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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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


	@SuppressWarnings("unchecked")
    public SchedulerJobDTO fetchSchedulerJobForUserAndExploratory(String user, String exploratoryName) {
		return schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(user, exploratoryName);
    }

    @SuppressWarnings("unchecked")
	private List<SchedulerJobData> getSchedulerJobsForExploratoryAction(OffsetDateTime dateTime, String actionType) {
		return schedulerJobDAO.getSchedulerJobsForAction(dateTime, actionType);
	}

	@SuppressWarnings("unchecked")
	public void updateSchedulerDataForUserAndExploratory(String user, String exploratoryName, SchedulerJobDTO dto) {
		exploratoryDAO.updateSchedulerDataForUserAndExploratory(user, exploratoryName, dto);
	}

	public List<SchedulerJobData> getSchedulerJobsForStoppingExploratories(OffsetDateTime currentDateTime) {
		return Stream.concat(
				getSchedulerJobsForExploratoryAction(currentDateTime, "stop")
						.stream()
						.filter(jobData ->
								jobData.getJobDTO().getEndTime().isAfter(jobData.getJobDTO().getStartTime())),
				getSchedulerJobsForExploratoryAction(currentDateTime.minusDays(1), "stop")
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

	public List<SchedulerJobData> getSchedulerJobsForStartingExploratories(OffsetDateTime currentDateTime) {
		return getSchedulerJobsForExploratoryAction(currentDateTime, "start");
	}

	public void changeExploratoryStateOn(String state, SchedulerJobData jobData) {
		log.debug("Exploratory with name {} for user {} is {}...",
				jobData.getExploratoryName(), jobData.getUser(),
				(state.equalsIgnoreCase("running") ? "starting" : "stopping"));
		UserInfo userInfo = systemUserService.create(jobData.getUser());
		String uuid = state.equalsIgnoreCase("running") ?
				exploratoryService.start(userInfo, jobData.getExploratoryName()) :
				exploratoryService.stop(userInfo, jobData.getExploratoryName());
		RequestId.put(userInfo.getName(), uuid);
	}

}

