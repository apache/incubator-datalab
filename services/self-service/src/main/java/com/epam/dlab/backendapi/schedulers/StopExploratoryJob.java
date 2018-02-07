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
import com.epam.dlab.backendapi.dao.SchedulerJobsDAO;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.fiestacabin.dropwizard.quartz.Scheduled;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.TimeUnit;

@Slf4j
@Scheduled(interval = 30, unit = TimeUnit.SECONDS)
public class StopExploratoryJob implements Job{

    @Inject
    private SchedulerJobsDAO schedulerJobsDAO;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
//        log.info("===============JOB EXECUTION==============");
//        SchedulerJobDTO doc = schedulerJobsDAO.fetchSingleSchedulerJobByUserAndExploratory("test", "deep_1");
//        log.info("All scheduler jobs: {}", doc);
    }
}
