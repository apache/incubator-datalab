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

package com.epam.dlab.backendapi.core.health;

import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.contracts.HealthChecker;
import com.epam.dlab.mongo.MongoHealthChecker;
import com.epam.dlab.mongo.MongoService;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/** Health checkers for services.
 */
public class HealthModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    /** Creates and returns the health checker for Mongo database.
     * @param mongoService Mongo database service.
     */
    @Provides
    @Singleton
    @Named(ServiceConsts.MONGO_HEALTH_CHECKER)
    public HealthChecker mongoHealthChecker(MongoService mongoService) {
        return new MongoHealthChecker(mongoService);
    }

    /** Creates and returns the health checker for provisioning service.
     * @param provisioningService provisioning service.
     */
    @Provides
    @Singleton
    @Named(ServiceConsts.PROVISIONING_HEALTH_CHECKER)
    public HealthChecker provisioningHealthChecker(@Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService) {
        return new ProvisioningHealthChecker(provisioningService);
    }
}
