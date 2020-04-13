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
import com.epam.dlab.backendapi.annotation.BudgetLimited;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.dao.aws.AwsBillingDAO;
import com.epam.dlab.backendapi.dao.azure.AzureBillingDAO;
import com.epam.dlab.backendapi.dao.gcp.GcpBillingDao;
import com.epam.dlab.backendapi.interceptor.BudgetLimitInterceptor;
import com.epam.dlab.backendapi.resources.BillingResource;
import com.epam.dlab.backendapi.resources.BucketResource;
import com.epam.dlab.backendapi.resources.aws.ComputationalResourceAws;
import com.epam.dlab.backendapi.resources.azure.ComputationalResourceAzure;
import com.epam.dlab.backendapi.resources.gcp.ComputationalResourceGcp;
import com.epam.dlab.backendapi.resources.gcp.GcpOauthResource;
import com.epam.dlab.backendapi.service.BillingService;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.backendapi.service.InfrastructureTemplateService;
import com.epam.dlab.backendapi.service.aws.AwsBillingService;
import com.epam.dlab.backendapi.service.azure.AzureBillingService;
import com.epam.dlab.backendapi.service.gcp.GcpBillingService;
import com.epam.dlab.backendapi.service.impl.InfrastructureInfoServiceImpl;
import com.epam.dlab.backendapi.service.impl.InfrastructureTemplateServiceImpl;
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

public class CloudProviderModule extends CloudModule {

    private static final String MONGO_URI_FORMAT = "mongodb://%s:%s@%s:%d/%s";
    private static final String QUARTZ_MONGO_URI_PROPERTY = "org.quartz.jobStore.mongoUri";
    private static final String QUARTZ_DB_NAME = "org.quartz.jobStore.dbName";

    private SelfServiceApplicationConfiguration configuration;

    public CloudProviderModule(SelfServiceApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bindBilling();
        bind(InfrastructureInfoService.class).to(InfrastructureInfoServiceImpl.class);
        bind(InfrastructureTemplateService.class).to(InfrastructureTemplateServiceImpl.class);
        bind(SchedulerConfiguration.class).toInstance(
                new SchedulerConfiguration(SelfServiceApplication.class.getPackage().getName()));

        final BudgetLimitInterceptor budgetLimitInterceptor = new BudgetLimitInterceptor();
        requestInjection(budgetLimitInterceptor);
        bindInterceptor(any(), annotatedWith(BudgetLimited.class), budgetLimitInterceptor);
    }

    @Override
    public void init(Environment environment, Injector injector) {
        environment.jersey().register(injector.getInstance(BillingResource.class));
        environment.jersey().register(injector.getInstance(ComputationalResourceAws.class));
        environment.jersey().register(injector.getInstance(ComputationalResourceAzure.class));
        environment.jersey().register(injector.getInstance(ComputationalResourceGcp.class));
        environment.jersey().register(injector.getInstance(BucketResource.class));
        if (injector.getInstance(SelfServiceApplicationConfiguration.class).isGcpOuauth2AuthenticationEnabled()) {
            environment.jersey().register(injector.getInstance(GcpOauthResource.class));
        }
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

    private void bindBilling() {
        switch (configuration.getCloudProvider()) {
            case AWS:
                bind(BillingService.class).to(AwsBillingService.class);
                bind(BillingDAO.class).to(AwsBillingDAO.class);
                break;
            case AZURE:
                bind(BillingService.class).to(AzureBillingService.class);
                bind(BillingDAO.class).to(AzureBillingDAO.class);
                break;
            case GCP:
                bind(BillingService.class).to(GcpBillingService.class);
                bind(BillingDAO.class).to(GcpBillingDao.class);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported cloud provider " + configuration.getCloudProvider());
        }
    }
}
