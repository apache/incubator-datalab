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
package com.epam.dlab.backendapi.schedulers;

import com.epam.dlab.backendapi.schedulers.internal.Scheduled;
import com.epam.dlab.backendapi.service.InactivityService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * There realized integration with Quartz scheduler framework and defined check cluster inactivity scheduler job which
 * executes every time specified. If in 'self-service.yml' option 'clusterInactivityCheckerEnabled' equals 'true' then
 * this job we be executing every time predefined in option 'clusterInactivityCheckingTimeout'. Otherwise, it will
 * never execute.
 */
@Slf4j
@Scheduled("inactivity")
public class CheckInactivityScheduledJob implements Job {

	@Inject
	private InactivityService inactivityService;

	@Override
	public void execute(JobExecutionContext context) {
		log.trace("Starting check inactivity job");
		inactivityService.updateRunningResourcesLastActivity();
	}
}
