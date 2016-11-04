/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi;

import com.epam.dlab.auth.SecurityFactory;
import com.epam.dlab.backendapi.core.guice.ModuleFactory;
import com.epam.dlab.backendapi.dao.IndexCreator;
import com.epam.dlab.backendapi.resources.*;
import com.epam.dlab.providers.JsonProcessingExceptionMapper;
import com.epam.dlab.providers.RuntimeExceptionMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
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
        bootstrap.addBundle(new AssetsBundle("/webapp/dist/prod", "/", "index.html"));
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
