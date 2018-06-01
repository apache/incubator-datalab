/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
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

package com.epam.dlab.backendapi.modules;

import com.epam.dlab.auth.SecurityFactory;
import com.epam.dlab.backendapi.SelfServiceApplication;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthenticator;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.aws.AwsKeyDao;
import com.epam.dlab.backendapi.resources.aws.BillingResourceAws;
import com.epam.dlab.backendapi.resources.aws.ComputationalResourceAws;
import com.epam.dlab.backendapi.resources.callback.aws.EdgeCallbackAws;
import com.epam.dlab.backendapi.resources.callback.aws.KeyUploaderCallbackAws;
import com.epam.dlab.backendapi.service.BillingService;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.backendapi.service.InfrastructureTemplateService;
import com.epam.dlab.backendapi.service.aws.AwsBillingService;
import com.epam.dlab.backendapi.service.aws.AwsInfrastructureInfoService;
import com.epam.dlab.backendapi.service.aws.AwsInfrastructureTemplateService;
import com.epam.dlab.cloud.CloudModule;
import com.fiestacabin.dropwizard.quartz.SchedulerConfiguration;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.setup.Environment;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class AwsSelfServiceModule extends CloudModule {

	@Override
	protected void configure() {
		bind(BillingService.class).to(AwsBillingService.class);
		bind((KeyDAO.class)).to(AwsKeyDao.class);
		bind(InfrastructureInfoService.class).to(AwsInfrastructureInfoService.class);
		bind(SchedulerConfiguration.class).toInstance(
				new SchedulerConfiguration(SelfServiceApplication.class.getPackage().getName()));
		bind(InfrastructureTemplateService.class).to(AwsInfrastructureTemplateService.class);
	}

	@Override
	public void init(Environment environment, Injector injector) {
		environment.jersey().register(injector.getInstance(EdgeCallbackAws.class));
		environment.jersey().register(injector.getInstance(KeyUploaderCallbackAws.class));
		environment.jersey().register(injector.getInstance(ComputationalResourceAws.class));
		environment.jersey().register(injector.getInstance(BillingResourceAws.class));

		injector.getInstance(SecurityFactory.class).configure(injector, environment,
				SelfServiceSecurityAuthenticator.class, injector.getInstance(Authorizer.class));
	}


	@Provides
	@Singleton
	Scheduler provideScheduler() throws SchedulerException {
		return StdSchedulerFactory.getDefaultScheduler();
	}
}
