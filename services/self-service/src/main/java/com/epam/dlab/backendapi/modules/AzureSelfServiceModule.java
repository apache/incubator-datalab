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
import com.epam.dlab.auth.rest.UserSessionDurationAuthorizer;
import com.epam.dlab.backendapi.SelfServiceApplication;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthenticator;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.azure.AzureKeyDao;
import com.epam.dlab.backendapi.resources.SecurityResource;
import com.epam.dlab.backendapi.resources.azure.AzureOauthResource;
import com.epam.dlab.backendapi.resources.azure.BillingResourceAzure;
import com.epam.dlab.backendapi.resources.azure.ComputationalResourceAzure;
import com.epam.dlab.backendapi.resources.callback.azure.EdgeCallbackAzure;
import com.epam.dlab.backendapi.resources.callback.azure.KeyUploaderCallbackAzure;
import com.epam.dlab.backendapi.service.BillingService;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.backendapi.service.InfrastructureTemplateService;
import com.epam.dlab.backendapi.service.azure.AzureBillingService;
import com.epam.dlab.backendapi.service.azure.AzureInfrastructureInfoService;
import com.epam.dlab.backendapi.service.azure.AzureInfrastructureTemplateService;
import com.epam.dlab.cloud.CloudModule;
import com.fiestacabin.dropwizard.quartz.SchedulerConfiguration;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

@Slf4j
public class AzureSelfServiceModule extends CloudModule {

	private boolean useLdap;
	private long maxSessionDurabilityMilliseconds;

	public AzureSelfServiceModule(boolean useLdap, long maxSessionDurabilityMilliseconds) {
		this.useLdap = useLdap;
		this.maxSessionDurabilityMilliseconds = maxSessionDurabilityMilliseconds;
	}

	@Override
	protected void configure() {
		bind(BillingService.class).to(AzureBillingService.class);
		bind((KeyDAO.class)).to(AzureKeyDao.class);
		bind(InfrastructureInfoService.class).to(AzureInfrastructureInfoService.class);
		bind(SchedulerConfiguration.class).toInstance(
				new SchedulerConfiguration(SelfServiceApplication.class.getPackage().getName()));
		bind(InfrastructureTemplateService.class).to(AzureInfrastructureTemplateService.class);
	}

	@Override
	public void init(Environment environment, Injector injector) {
		environment.jersey().register(injector.getInstance(EdgeCallbackAzure.class));
		environment.jersey().register(injector.getInstance(KeyUploaderCallbackAzure.class));
		environment.jersey().register(injector.getInstance(ComputationalResourceAzure.class));
		environment.jersey().register(injector.getInstance(BillingResourceAzure.class));

		if (!useLdap) {
			environment.jersey().register(injector.getInstance(AzureOauthResource.class));
			injector.getInstance(SecurityFactory.class).configure(injector, environment,
					SelfServiceSecurityAuthenticator.class,
					new UserSessionDurationAuthorizer(ui ->
							injector.getInstance(SecurityResource.class).userLogout(ui),
							maxSessionDurabilityMilliseconds));
		}

		injector.getInstance(SecurityFactory.class).configure(injector, environment,
				SelfServiceSecurityAuthenticator.class, injector.getInstance(Authorizer.class));
	}

	@Provides
	@Singleton
	Scheduler provideScheduler() throws SchedulerException {
		return StdSchedulerFactory.getDefaultScheduler();
	}
}
