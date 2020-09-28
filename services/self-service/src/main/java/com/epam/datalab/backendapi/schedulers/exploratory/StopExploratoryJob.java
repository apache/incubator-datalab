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

package com.epam.datalab.backendapi.schedulers.exploratory;

import com.epam.datalab.backendapi.schedulers.internal.Scheduled;
import com.epam.datalab.backendapi.service.SchedulerJobService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * There realized integration with Quartz scheduler framework and defined stop exploratory scheduler job which
 * executes every time specified.
 */
@Slf4j
@Scheduled("stopExploratoryScheduler")
public class StopExploratoryJob implements Job {

    @Inject
    private SchedulerJobService schedulerJobService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        schedulerJobService.stopExploratoryByScheduler();
    }

}

