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

package com.epam.dlab.backendapi.modules;

import com.epam.dlab.backendapi.SelfServiceApplication;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.annotation.BudgetLimited;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.azure.AzureBillingDAO;
import com.epam.dlab.backendapi.dao.azure.AzureKeyDao;
import com.epam.dlab.backendapi.interceptor.BudgetLimitInterceptor;
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
import com.epam.dlab.mongo.MongoServiceFactory;
import com.fiestacabin.dropwizard.quartz.SchedulerConfiguration;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

@Slf4j
public class AzureSelfServiceModule extends CloudModule {

	private static final String MONGO_URI_FORMAT = "mongodb://%s:%s@%s:%d/%s";
	private static final String QUARTZ_MONGO_URI_PROPERTY = "org.quartz.jobStore.mongoUri";
	private static final String QUARTZ_DB_NAME = "org.quartz.jobStore.dbName";
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
		bind(BillingDAO.class).to(AzureBillingDAO.class);
		final BudgetLimitInterceptor budgetLimitInterceptor = new BudgetLimitInterceptor();
		requestInjection(budgetLimitInterceptor);
		bindInterceptor(any(), annotatedWith(BudgetLimited.class), budgetLimitInterceptor);
	}

	@Override
	public void init(Environment environment, Injector injector) {
		environment.jersey().register(injector.getInstance(EdgeCallbackAzure.class));
		environment.jersey().register(injector.getInstance(KeyUploaderCallbackAzure.class));
		environment.jersey().register(injector.getInstance(ComputationalResourceAzure.class));
		environment.jersey().register(injector.getInstance(BillingResourceAzure.class));

	}

	@Provides
	@Singleton
	Scheduler provideScheduler(SelfServiceApplicationConfiguration configuration) throws SchedulerException {
		final MongoServiceFactory mongoFactory = configuration.getMongoFactory();
		final String database = mongoFactory.getDatabase();
		final String mongoUri = String.format(MONGO_URI_FORMAT, mongoFactory.getUsername(), mongoFactory.getPassword(),
				mongoFactory.getHost(), mongoFactory.getPort(), database);
		System.setProperty(QUARTZ_MONGO_URI_PROPERTY, mongoUri);
		System.setProperty(QUARTZ_DB_NAME, database);
		return StdSchedulerFactory.getDefaultScheduler();
	}
}
