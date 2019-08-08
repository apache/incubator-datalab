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

package com.epam.dlab.backendapi.schedulers.internal;

import com.epam.dlab.backendapi.SelfServiceApplication;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.domain.SchedulerConfigurationData;
import com.epam.dlab.exceptions.DlabException;
import com.fiestacabin.dropwizard.quartz.GuiceJobFactory;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.util.Optional;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Slf4j
public class ManagedScheduler implements Managed {
	private final Scheduler scheduler;
	private final GuiceJobFactory jobFactory;
	private final SelfServiceApplicationConfiguration config;

	@Inject
	public ManagedScheduler(Scheduler scheduler, GuiceJobFactory jobFactory,
							SelfServiceApplicationConfiguration config) {
		this.scheduler = scheduler;
		this.jobFactory = jobFactory;
		this.config = config;
	}

	@Override
	public void start() throws Exception {
		scheduler.setJobFactory(jobFactory);
		scheduler.start();

		new Reflections(SelfServiceApplication.class.getPackage().getName(), new SubTypesScanner())
				.getSubTypesOf(Job.class)
				.forEach(scheduledClass ->
						Optional.ofNullable(scheduledClass.getAnnotation(Scheduled.class))
								.filter(this::triggerNotExist)
								.ifPresent(scheduleAnn -> schedule(scheduledClass, scheduleAnn)));

	}

	@Override
	public void stop() throws Exception {
		scheduler.shutdown();
	}

	private Trigger getTrigger(SchedulerConfigurationData schedulerConfig, String schedulerName) {
		return newTrigger()
				.withIdentity(schedulerName)
				.withSchedule(CronScheduleBuilder.cronSchedule(schedulerConfig.getCron()))
				.startNow()
				.build();
	}

	private void schedule(Class<? extends Job> scheduledClass, Scheduled scheduleAnn) {
		final String schedulerName = scheduleAnn.value();
		final SchedulerConfigurationData schedulerConfig =
				Optional.ofNullable(config.getSchedulers().get(schedulerName)).orElseThrow(() -> new IllegalStateException(
						"There is no configuration provided for scheduler with name " + schedulerName));
		if (schedulerConfig.isEnabled()) {
			scheduleJob(newJob(scheduledClass).build(), schedulerConfig, scheduleAnn.value());
		}
	}

	private void scheduleJob(JobDetail job, SchedulerConfigurationData schedulerConfig, String schedulerName) {
		try {
			final Trigger trigger = getTrigger(schedulerConfig, schedulerName);
			scheduler.scheduleJob(job, trigger);
			log.info("Scheduled job {} with trigger {}", job, trigger);
		} catch (SchedulerException e) {
			log.error("Can't schedule job due to: {}", e.getMessage());
			throw new DlabException("Can't schedule job due to: " + e.getMessage(), e);
		}
	}

	private boolean triggerNotExist(Scheduled scheduled) {
		try {
			return !scheduler.checkExists(new TriggerKey(scheduled.value()));
		} catch (SchedulerException e) {
			log.error("Can not check if trigger exist due to: {}", e.getMessage());
			throw new DlabException("Can not check if trigger exist due to: {}" + e.getMessage(), e);
		}
	}
}
