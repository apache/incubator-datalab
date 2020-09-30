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

package com.epam.datalab.backendapi.modules;

import com.epam.datalab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.google.inject.AbstractModule;
import io.dropwizard.setup.Environment;

public class ModuleFactory {

    private ModuleFactory() {
    }

    /**
     * Instantiates an application configuration of Provisioning Service for production or tests if
     * the mock property of configuration is set to <b>true</b> and method {@link ProvisioningServiceApplicationConfiguration#isDevMode()}}
     * returns <b>true</b> value.
     *
     * @param configuration application configuration of Provisioning Service.
     * @param environment   environment of Provisioning Service.
     */
    public static AbstractModule getModule(ProvisioningServiceApplicationConfiguration configuration, Environment environment) {
        return configuration.isDevMode()
                ? new ProvisioningDevModule(configuration, environment)
                : new ProductionModule(configuration, environment);
    }
}
