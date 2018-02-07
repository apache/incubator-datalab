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
import com.epam.dlab.cloud.CloudModule;
import com.google.inject.AbstractModule;
import io.dropwizard.setup.Environment;

public class ModuleFactory {

    private ModuleFactory() {
    }

    /**
     * Instantiates an application configuration of SelfService for production or tests if
     * the mock property of configuration is set to <b>true</b> and method {@link SelfServiceApplicationConfiguration#isMocked()}
     * returns <b>true</b> value.
     *
     * @param configuration application configuration of SelfService.
     * @param environment   environment of SelfService.
     */
    public static AbstractModule getModule(SelfServiceApplicationConfiguration configuration, Environment environment) {
        return configuration.isDevMode()
                ? new DevModule(configuration, environment)
                : new ProductionModule(configuration, environment);
    }

    public static CloudModule getCloudProviderModule(SelfServiceApplicationConfiguration configuration) {
        switch (configuration.getCloudProvider()) {
            case AWS:
                return new AwsSelfServiceModule();
            case AZURE:
                return new AzureSelfServiceModule(configuration.getAzureLoginConfiguration());
            case GCP:
                return new GcpSelfServiceModule();
            default:
                throw new UnsupportedOperationException("Unsupported cloud provider " + configuration.getCloudProvider());
        }
    }
}
