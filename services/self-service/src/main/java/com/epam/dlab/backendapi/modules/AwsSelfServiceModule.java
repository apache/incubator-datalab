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

import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.aws.AwsKeyDao;
import com.epam.dlab.backendapi.domain.aws.BillingSchedulerManagerAws;
import com.epam.dlab.backendapi.resources.aws.ComputationalResourceAws;
import com.epam.dlab.backendapi.resources.callback.aws.EdgeCallbackAws;
import com.epam.dlab.backendapi.resources.callback.aws.KeyUploaderCallbackAws;
import com.epam.dlab.backendapi.service.AwsBillingService;
import com.epam.dlab.backendapi.service.AwsInfrastructureInfoService;
import com.epam.dlab.backendapi.service.BillingService;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.cloud.CloudModule;
import com.google.inject.Injector;
import io.dropwizard.setup.Environment;

public class AwsSelfServiceModule extends CloudModule {

    @Override
    protected void configure() {
        bind(BillingService.class).to(AwsBillingService.class);
        bind((KeyDAO.class)).to(AwsKeyDao.class);
        bind(InfrastructureInfoService.class).to(AwsInfrastructureInfoService.class);
    }

    @Override
    public void init(Environment environment, Injector injector) {
        environment.jersey().register(injector.getInstance(EdgeCallbackAws.class));
        environment.jersey().register(injector.getInstance(KeyUploaderCallbackAws.class));
        environment.jersey().register(injector.getInstance(ComputationalResourceAws.class));
        environment.lifecycle().manage(injector.getInstance(BillingSchedulerManagerAws.class));
    }
}
