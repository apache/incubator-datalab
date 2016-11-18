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

package com.epam.dlab.backendapi;

import com.epam.dlab.backendapi.core.response.DirectoriesCreator;
import com.epam.dlab.backendapi.core.response.warmup.DockerWarmuper;
import com.epam.dlab.backendapi.core.response.warmup.MetadataHolder;
import com.epam.dlab.backendapi.resources.*;
import com.epam.dlab.client.restclient.RESTService;
import com.epam.dlab.providers.JsonProcessingExceptionMapper;
import com.epam.dlab.providers.RuntimeExceptionMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;

import static com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration.SELF_SERVICE;

public class ProvisioningServiceApplication extends Application<ProvisioningServiceApplicationConfiguration> {
    public static void main(String[] args) throws Exception {
        new ProvisioningServiceApplication().run(args);
    }

    @Override
    public void run(ProvisioningServiceApplicationConfiguration configuration, Environment environment) throws Exception {
        Injector injector = createInjector(configuration, environment);
        environment.lifecycle().manage(injector.getInstance(DirectoriesCreator.class));
        environment.lifecycle().manage(injector.getInstance(DockerWarmuper.class));
        JerseyEnvironment jersey = environment.jersey();
        jersey.register(new RuntimeExceptionMapper());
        jersey.register(new JsonProcessingExceptionMapper());
        jersey.register(injector.getInstance(DockerResource.class));
        jersey.register(injector.getInstance(KeyLoaderResource.class));
        jersey.register(injector.getInstance(KeyLoaderResource.class));
        jersey.register(injector.getInstance(ExploratoryResource.class));
        jersey.register(injector.getInstance(ComputationalResource.class));
        jersey.register(injector.getInstance(InfrastructureResource.class));
    }

    private Injector createInjector(ProvisioningServiceApplicationConfiguration configuration, Environment environment) {
        return Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ProvisioningServiceApplicationConfiguration.class).toInstance(configuration);
                bind(MetadataHolder.class).to(DockerWarmuper.class);
                bind(RESTService.class).toInstance(configuration.getSelfFactory().build(environment, SELF_SERVICE));
            }
        });
    }
}
