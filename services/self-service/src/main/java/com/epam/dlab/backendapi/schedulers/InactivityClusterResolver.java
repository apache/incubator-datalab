/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.epam.dlab.backendapi.schedulers;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.schedulers.computational.CheckInactivityComputationalJob;
import com.epam.dlab.util.AnnotationUtils;
import com.fiestacabin.dropwizard.quartz.Scheduled;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class InactivityClusterResolver implements Managed {

	@Inject
	private SelfServiceApplicationConfiguration configuration;

	@Override
	public void start() {
		setCheckClusterInactivityScheduler(configuration.isClusterInactivityCheckerEnabled());
	}

	@Override
	public void stop() {
		log.debug("InactivityClusterResolver is stopped");
	}

	private void setCheckClusterInactivityScheduler(boolean value) {
		DynamicScheduledAnnotation targetValue = new DynamicScheduledAnnotation();
		if (value) {
			targetValue.setInterval((int) configuration.getClusterInactivityCheckingTimeout().toMinutes());
			targetValue.setTimeUnit(TimeUnit.MINUTES);
		} else {
			targetValue.setInterval((CronExpression.MAX_YEAR - Calendar.getInstance().get(Calendar.YEAR)) * 365);
			targetValue.setTimeUnit(TimeUnit.DAYS);
		}
		AnnotationUtils.updateAnnotation(CheckInactivityComputationalJob.class, Scheduled.class, targetValue);
	}
}
