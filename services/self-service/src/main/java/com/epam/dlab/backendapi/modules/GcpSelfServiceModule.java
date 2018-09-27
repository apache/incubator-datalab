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

package com.epam.dlab.backendapi.modules;

import com.epam.dlab.auth.SecurityFactory;
import com.epam.dlab.backendapi.SelfServiceApplication;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthenticator;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.gcp.GcpKeyDao;
import com.epam.dlab.backendapi.resources.DexOauthResource;
import com.epam.dlab.backendapi.resources.callback.gcp.EdgeCallbackGcp;
import com.epam.dlab.backendapi.resources.callback.gcp.KeyUploaderCallbackGcp;
import com.epam.dlab.backendapi.resources.gcp.ComputationalResourceGcp;
import com.epam.dlab.backendapi.resources.gcp.GcpOauthResource;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.backendapi.service.InfrastructureTemplateService;
import com.epam.dlab.backendapi.service.gcp.GcpInfrastructureInfoService;
import com.epam.dlab.backendapi.service.gcp.GcpInfrastructureTemplateService;
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

public class GcpSelfServiceModule extends CloudModule {

	private final boolean useDex;

	public GcpSelfServiceModule(boolean useDex) {
		this.useDex = useDex;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(Environment environment, Injector injector) {

		environment.jersey().register(injector.getInstance(EdgeCallbackGcp.class));
		environment.jersey().register(injector.getInstance(KeyUploaderCallbackGcp.class));
		environment.jersey().register(injector.getInstance(ComputationalResourceGcp.class));
		if (injector.getInstance(SelfServiceApplicationConfiguration.class).isGcpOuauth2AuthenticationEnabled()) {
			environment.jersey().register(injector.getInstance(GcpOauthResource.class));
		}
		injector.getInstance(SecurityFactory.class).configure(injector, environment,
				SelfServiceSecurityAuthenticator.class, injector.getInstance(Authorizer.class));
		if (useDex) {
			environment.jersey().register(injector.getInstance(DexOauthResource.class));
		}

	}

	@Override
	protected void configure() {
		bind((KeyDAO.class)).to(GcpKeyDao.class);
		bind(InfrastructureInfoService.class).to(GcpInfrastructureInfoService.class);
		bind(InfrastructureTemplateService.class).to(GcpInfrastructureTemplateService.class);
		bind(SchedulerConfiguration.class).toInstance(
				new SchedulerConfiguration(SelfServiceApplication.class.getPackage().getName()));
	}

	@Provides
	@Singleton
	Scheduler provideScheduler() throws SchedulerException {
		return StdSchedulerFactory.getDefaultScheduler();
	}
}
