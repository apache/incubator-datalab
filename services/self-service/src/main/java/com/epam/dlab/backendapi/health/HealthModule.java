/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.backendapi.health;

import com.epam.dlab.client.mongo.MongoService;
import com.epam.dlab.client.restclient.RESTService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import static com.epam.dlab.backendapi.SelfServiceApplicationConfiguration.PROVISIONING_SERVICE;
import static com.epam.dlab.backendapi.health.HealthChecks.MONGO_HEALTH_CHECKER;
import static com.epam.dlab.backendapi.health.HealthChecks.PROVISIONING_HEALTH_CHECKER;

public class HealthModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    @Named(MONGO_HEALTH_CHECKER)
    public HealthChecker mongoHealthChecker(MongoService mongoService) {
        return new MongoHealthChecker(mongoService);
    }

    @Provides
    @Singleton
    @Named(PROVISIONING_HEALTH_CHECKER)
    public HealthChecker provisioningHealthChecker(@Named(PROVISIONING_SERVICE) RESTService provisioningService) {
        return new ProvisioningHealthChecker(provisioningService);
    }
}
