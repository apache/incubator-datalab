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

import com.epam.dlab.auth.SecurityFactory;
import com.epam.dlab.backendapi.dao.IndexCreator;
import com.epam.dlab.backendapi.modules.ModuleFactory;
import com.epam.dlab.backendapi.resources.*;
import com.epam.dlab.utils.ServiceUtils;
import com.epam.dlab.rest.mappers.JsonProcessingExceptionMapper;
import com.epam.dlab.rest.mappers.RuntimeExceptionMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

public class SelfServiceApplication extends Application<SelfServiceApplicationConfiguration> {
    public static void main(String... args) throws Exception {
        new SelfServiceApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<SelfServiceApplicationConfiguration> bootstrap) {
        super.initialize(bootstrap);
        //bootstrap.addBundle(new AssetsBundle("/webapp/node_modules", "/node_modules", null, "node_modules"));
        //bootstrap.addBundle(new AssetsBundle("/webapp/dist/dev", "/", "index.html"));
        bootstrap.addBundle(new AssetsBundle("/webapp/dist/prod", "/", "index.html"));
        bootstrap.addBundle(new TemplateConfigBundle(
        		new TemplateConfigBundleConfiguration().fileIncludePath(ServiceUtils.getConfPath())
        ));
    }

    @Override
    public void run(SelfServiceApplicationConfiguration configuration, Environment environment) throws Exception {
        Injector injector = Guice.createInjector(ModuleFactory.getModule(configuration, environment));
        environment.lifecycle().manage(injector.getInstance(IndexCreator.class));
        injector.getInstance(SecurityFactory.class).configure(injector, environment);
        JerseyEnvironment jersey = environment.jersey();
        jersey.register(new RuntimeExceptionMapper());
        jersey.register(new JsonProcessingExceptionMapper());
        jersey.register(MultiPartFeature.class);
        jersey.register(injector.getInstance(SecurityResource.class));
        jersey.register(injector.getInstance(DockerResource.class));
        jersey.register(injector.getInstance(KeyUploaderResource.class));
        jersey.register(injector.getInstance(InfrastructureProvisionResource.class));
        jersey.register(injector.getInstance(ComputationalResource.class));
        jersey.register(injector.getInstance(ExploratoryResource.class));
        jersey.register(injector.getInstance(InfrasctructureResource.class));
    }
}
