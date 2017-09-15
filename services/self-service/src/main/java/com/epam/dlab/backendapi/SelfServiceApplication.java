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

package com.epam.dlab.backendapi;

import com.epam.dlab.auth.SecurityFactory;
import com.epam.dlab.backendapi.dao.IndexCreator;
import com.epam.dlab.backendapi.domain.EnvStatusListener;
import com.epam.dlab.backendapi.domain.ExploratoryLibCache;
import com.epam.dlab.backendapi.modules.ModuleFactory;
import com.epam.dlab.backendapi.resources.*;
import com.epam.dlab.backendapi.resources.callback.ExploratoryCallback;
import com.epam.dlab.backendapi.resources.callback.GitCredsCallback;
import com.epam.dlab.backendapi.resources.callback.LibExploratoryCallback;
import com.epam.dlab.cloud.CloudModule;
import com.epam.dlab.rest.mappers.JsonProcessingExceptionMapper;
import com.epam.dlab.rest.mappers.RuntimeExceptionMapper;
import com.epam.dlab.utils.ServiceUtils;
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

/**
 * Self Service based on Dropwizard application.
 */
public class SelfServiceApplication extends Application<SelfServiceApplicationConfiguration> {
    private static Injector injector;

    public static Injector getInjector() {
        return injector;
    }

    public static void main(String... args) throws Exception {
        if (ServiceUtils.printAppVersion(SelfServiceApplication.class, args)) {
            return;
        }
        new SelfServiceApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<SelfServiceApplicationConfiguration> bootstrap) {
        super.initialize(bootstrap);
        //bootstrap.addBundle(new AssetsBundle("/webapp/node_modules", "/node_modules", null, "node_modules"));
        //bootstrap.addBundle(new AssetsBundle("/webapp/dist/dev", "/", "index.html"));
        bootstrap.addBundle(new AssetsBundle("/webapp/dist", "/", "index.html"));
        bootstrap.addBundle(new TemplateConfigBundle(
                new TemplateConfigBundleConfiguration().fileIncludePath(ServiceUtils.getConfPath())
        ));
    }

    @Override
    public void run(SelfServiceApplicationConfiguration configuration, Environment environment) throws Exception {

        CloudModule cloudModule = ModuleFactory.getCloudProviderModule(configuration);
        injector = Guice.createInjector(ModuleFactory.getModule(configuration, environment), cloudModule);
        cloudModule.init(environment, injector);
        environment.lifecycle().manage(injector.getInstance(IndexCreator.class));
        environment.lifecycle().manage(injector.getInstance(EnvStatusListener.class));
        environment.lifecycle().manage(injector.getInstance(ExploratoryLibCache.class));

        injector.getInstance(SecurityFactory.class).configure(injector, environment);

        JerseyEnvironment jersey = environment.jersey();
        jersey.register(new RuntimeExceptionMapper());
        jersey.register(new JsonProcessingExceptionMapper());
        jersey.register(MultiPartFeature.class);
        jersey.register(injector.getInstance(SecurityResource.class));
        jersey.register(injector.getInstance(KeyUploaderResource.class));
        jersey.register(injector.getInstance(EdgeResource.class));

        jersey.register(injector.getInstance(InfrastructureTemplatesResource.class));
        jersey.register(injector.getInstance(InfrastructureInfoResource.class));

        jersey.register(injector.getInstance(UserSettingsResource.class));

        jersey.register(injector.getInstance(ExploratoryResource.class));
        jersey.register(injector.getInstance(ExploratoryCallback.class));

        jersey.register(injector.getInstance(LibExploratoryResource.class));
        jersey.register(injector.getInstance(LibExploratoryCallback.class));

        jersey.register(injector.getInstance(GitCredsResource.class));
        jersey.register(injector.getInstance(GitCredsCallback.class));

        jersey.register(injector.getInstance(BillingResource.class));
    }
}
