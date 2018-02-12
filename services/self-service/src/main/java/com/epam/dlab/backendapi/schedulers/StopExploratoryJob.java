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

package com.epam.dlab.backendapi.schedulers;
import com.epam.dlab.auth.SystemUserInfoServiceImpl;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ExploratoryServiceImpl;
import com.epam.dlab.backendapi.service.SchedulerJobsService;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.fiestacabin.dropwizard.quartz.Scheduled;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Scheduled(interval = 10)
public class StopExploratoryJob implements Job{

    @Inject
    private SchedulerJobsService schedulerJobsService;

    @Inject
    private ExploratoryServiceImpl exploratoryService;

    @Inject
    private SystemUserInfoServiceImpl systemUserService;


    @Override
    public void execute(JobExecutionContext jobExecutionContext){
        LocalTime currentTime = LocalTime.now();
        LocalTime currentTimeRounded = LocalTime.of(currentTime.getHour(), currentTime.getMinute());
		List<SchedulerJobData> todayJobsToStop = schedulerJobsService
				.getSchedulerJobsForExploratoryAction(LocalDate.now(), currentTimeRounded, "stop")
				.stream()
				.filter(jobData -> jobData.getJobDTO().getEndTime().isAfter(jobData.getJobDTO().getStartTime()))
				.collect(Collectors.toList());
		List<SchedulerJobData> yesterdayJobsToStop = schedulerJobsService
				.getSchedulerJobsForExploratoryAction(LocalDate.now().minusDays(1), currentTimeRounded, "stop")
				.stream()
				.filter(jobData -> jobData.getJobDTO().getEndTime().isBefore(jobData.getJobDTO().getStartTime()))
				.collect(Collectors.toList());
		List<SchedulerJobData> jobsToStop = new ArrayList<>(todayJobsToStop);
		jobsToStop.addAll(yesterdayJobsToStop);
        if(!jobsToStop.isEmpty()){
            log.info("Scheduler stop job is executing...");
            log.info("Current time rounded: {} , current date: {}, current day of week: {}", currentTimeRounded, LocalDate.now(),
                    LocalDate.now().getDayOfWeek());
            log.info("Scheduler jobs for stopping: {}", jobsToStop.size());
            for(SchedulerJobData jobData : jobsToStop){
                log.info("Exploratory with name {} for user {} is stopping...", jobData.getExploratoryName(), jobData.getUser());
                UserInfo userInfo = systemUserService.create(jobData.getUser());
                String uuid = exploratoryService.stop(userInfo, jobData.getExploratoryName());
                RequestId.put(userInfo.getName(), uuid);
            }

        }

    }
}

