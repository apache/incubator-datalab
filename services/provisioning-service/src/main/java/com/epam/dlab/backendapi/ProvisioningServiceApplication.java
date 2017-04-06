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
import com.epam.dlab.backendapi.core.DirectoriesCreator;
import com.epam.dlab.backendapi.core.DockerWarmuper;
import com.epam.dlab.backendapi.modules.ModuleFactory;
import com.epam.dlab.backendapi.resources.ComputationalResource;
import com.epam.dlab.backendapi.resources.DockerResource;
import com.epam.dlab.backendapi.resources.EdgeResource;
import com.epam.dlab.backendapi.resources.ExploratoryResource;
import com.epam.dlab.backendapi.resources.InfrastructureResource;
import com.epam.dlab.process.DlabProcess;
import com.epam.dlab.rest.mappers.JsonProcessingExceptionMapper;
import com.epam.dlab.rest.mappers.RuntimeExceptionMapper;
import com.epam.dlab.utils.ServiceUtils;
import com.google.inject.Guice;
import com.google.inject.Injector;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ProvisioningServiceApplication extends Application<ProvisioningServiceApplicationConfiguration> {
	private static Injector injector;
	
	public static Injector getInjector() {
		return injector;
	}
	
    public static void main(String[] args) throws Exception {
        new ProvisioningServiceApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<ProvisioningServiceApplicationConfiguration> bootstrap) {
        bootstrap.addBundle(new TemplateConfigBundle(
        		new TemplateConfigBundleConfiguration().fileIncludePath(ServiceUtils.getConfPath())
        ));
    }

    @Override
    public void run(ProvisioningServiceApplicationConfiguration configuration, Environment environment) throws Exception {
        DlabProcess.getInstance().setProcessTimeout(configuration.getProcessTimeout());
        DlabProcess.getInstance().setMaxProcessesPerBox(configuration.getProcessMaxThreadsPerJvm());
        DlabProcess.getInstance().setMaxProcessesPerUser(configuration.getProcessMaxThreadsPerUser());
        
        injector = Guice.createInjector(ModuleFactory.getModule(configuration, environment));
        injector.getInstance(SecurityFactory.class).configure(injector, environment);
        
        environment.lifecycle().manage(injector.getInstance(DirectoriesCreator.class));
        environment.lifecycle().manage(injector.getInstance(DockerWarmuper.class));
        
        JerseyEnvironment jersey = environment.jersey();
        jersey.register(new RuntimeExceptionMapper());
        jersey.register(new JsonProcessingExceptionMapper());
        jersey.register(injector.getInstance(DockerResource.class));
        jersey.register(injector.getInstance(EdgeResource.class));
        jersey.register(injector.getInstance(ExploratoryResource.class));
        jersey.register(injector.getInstance(ComputationalResource.class));
        jersey.register(injector.getInstance(InfrastructureResource.class));
    }
}
