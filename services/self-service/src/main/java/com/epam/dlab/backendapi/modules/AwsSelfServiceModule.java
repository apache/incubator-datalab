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
import com.epam.dlab.backendapi.dao.aws.AwsBillingDAO;
import com.epam.dlab.backendapi.dao.aws.AwsKeyDao;
import com.epam.dlab.backendapi.interceptor.BudgetLimitInterceptor;
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
import com.epam.dlab.mongo.MongoServiceFactory;
import com.fiestacabin.dropwizard.quartz.SchedulerConfiguration;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

public class AwsSelfServiceModule extends CloudModule {

	private static final String MONGO_URI_FORMAT = "mongodb://%s:%s@%s:%d/%s";
	private static final String QUARTZ_MONGO_URI_PROPERTY = "org.quartz.jobStore.mongoUri";
	private static final String QUARTZ_DB_NAME = "org.quartz.jobStore.dbName";

	@Override
	protected void configure() {
		bind(BillingService.class).to(AwsBillingService.class);
		bind((KeyDAO.class)).to(AwsKeyDao.class);
		bind(InfrastructureInfoService.class).to(AwsInfrastructureInfoService.class);
		bind(SchedulerConfiguration.class).toInstance(
				new SchedulerConfiguration(SelfServiceApplication.class.getPackage().getName()));
		bind(InfrastructureTemplateService.class).to(AwsInfrastructureTemplateService.class);
		bind(BillingDAO.class).to(AwsBillingDAO.class);
		final BudgetLimitInterceptor budgetLimitInterceptor = new BudgetLimitInterceptor();
		requestInjection(budgetLimitInterceptor);
		bindInterceptor(any(), annotatedWith(BudgetLimited.class), budgetLimitInterceptor);
	}

	@Override
	public void init(Environment environment, Injector injector) {
		environment.jersey().register(injector.getInstance(EdgeCallbackAws.class));
		environment.jersey().register(injector.getInstance(KeyUploaderCallbackAws.class));
		environment.jersey().register(injector.getInstance(ComputationalResourceAws.class));
		environment.jersey().register(injector.getInstance(BillingResourceAws.class));

		/*injector.getInstance(SecurityFactory.class).configure(injector, environment,
				SelfServiceSecurityAuthenticator.class, injector.getInstance(Authorizer.class));*/
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
