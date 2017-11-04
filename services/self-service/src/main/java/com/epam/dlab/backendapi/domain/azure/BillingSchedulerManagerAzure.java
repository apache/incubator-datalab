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

package com.epam.dlab.backendapi.domain.azure;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.billing.azure.BillingSchedulerAzure;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;

import java.nio.file.Paths;

/**
 * Managed bean that schedules azure billing job
 */
public class BillingSchedulerManagerAzure implements Managed {

    @Inject
    private SelfServiceApplicationConfiguration configuration;
    private BillingSchedulerAzure billingSchedulerAzure;

    @Override
    public void start() throws Exception {
        if (configuration.isBillingSchedulerEnabled()) {
            String confFile = Paths.get(configuration.getBillingConfFile()).toAbsolutePath().toString();
            billingSchedulerAzure = new BillingSchedulerAzure(confFile);
            billingSchedulerAzure.start();
        }
    }

    @Override
    public void stop() throws Exception {
        billingSchedulerAzure.stop();
    }
}
