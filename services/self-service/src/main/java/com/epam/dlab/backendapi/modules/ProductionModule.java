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

package com.epam.dlab.backendapi.modules;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.mongo.MongoService;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.name.Names;
import io.dropwizard.setup.Environment;

import static com.epam.dlab.auth.SecurityRestAuthenticator.SECURITY_SERVICE;

public class ProductionModule extends BaseModule {
    public ProductionModule(SelfServiceApplicationConfiguration configuration, Environment environment) {
        super(configuration, environment);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(MongoService.class).toInstance(configuration.getMongoFactory().build(environment));
        bind(RESTService.class).annotatedWith(Names.named(SECURITY_SERVICE))
                .toInstance(configuration.getSecurityFactory().build(environment, SECURITY_SERVICE));
        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.PROVISIONING_SERVICE_NAME))
                .toInstance(configuration.getProvisioningFactory().build(environment, ServiceConsts.PROVISIONING_SERVICE_NAME));
    }
}
