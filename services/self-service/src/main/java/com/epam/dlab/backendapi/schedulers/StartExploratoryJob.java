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
import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import org.bson.Document;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Every("30s")
public class StartExploratoryJob extends Job{

    private static final Logger LOGGER = LoggerFactory.getLogger(StartExploratoryJob.class);

    private final SchedulerJobsDAO schedulerJobsDAO;

    public StartExploratoryJob(SchedulerJobsDAO schedulerJobsDAO) {
        this.schedulerJobsDAO = schedulerJobsDAO;
    }

    @Override
    public void doJob(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LOGGER.info("Start exploratory job in progress...");
        LOGGER.info("Equals null: {}", schedulerJobsDAO == null);
        SchedulerJobDTO doc = schedulerJobsDAO.fetchSingleSchedulerJobByUserAndExploratory("test", "deep_1");
        LOGGER.info("All scheduler jobs: {}", doc);

    }
}
