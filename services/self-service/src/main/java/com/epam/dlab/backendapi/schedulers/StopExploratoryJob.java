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

import com.epam.dlab.backendapi.service.SchedulerJobService;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.fiestacabin.dropwizard.quartz.Scheduled;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Scheduled(interval = 10)
public class StopExploratoryJob implements Job{

    @Inject
	private SchedulerJobService schedulerJobService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext){
		OffsetDateTime currentDateTime = OffsetDateTime.now();
		List<SchedulerJobData> jobsToStop = schedulerJobService.getSchedulerJobsForStoppingExploratories
				(currentDateTime);
		if (!jobsToStop.isEmpty()) {
			log.debug("Scheduler stop job is executing...");
			log.info("Current time rounded: {} , current date: {}, current day of week: {}",
					LocalTime.of(currentDateTime.toLocalTime().getHour(), currentDateTime.toLocalTime().getMinute()),
					currentDateTime.toLocalDate(),
					currentDateTime.getDayOfWeek());
			log.info("Scheduler jobs for stopping: {}", jobsToStop.size());
			jobsToStop.forEach(job -> schedulerJobService.changeExploratoryStateOn("stopped", job));
		}
	}

}

