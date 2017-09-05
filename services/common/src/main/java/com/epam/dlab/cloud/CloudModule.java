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

package com.epam.dlab.cloud;

import com.google.inject.Injector;
import com.google.inject.PrivateModule;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;

public abstract class CloudModule extends PrivateModule {
    protected Environment environment;
    protected JerseyEnvironment jerseyEnvironment;
    protected Injector injector;

    public CloudModule(Environment environment, Injector injector) {
        this.environment = environment;
        this.jerseyEnvironment = environment.jersey();
        this.injector = injector;
    }

    @Override
    protected void configure() {
    }
}
