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
import com.epam.dlab.backendapi.core.DirectoriesCreator;
import com.epam.dlab.backendapi.core.DockerWarmuper;
import com.epam.dlab.backendapi.modules.CloudModuleConfigurator;
import com.epam.dlab.backendapi.modules.ModuleFactory;
import com.epam.dlab.backendapi.resources.*;
import com.epam.dlab.backendapi.resources.base.KeyResource;
import com.epam.dlab.cloud.CloudModule;
import com.epam.dlab.process.model.DlabProcess;
import com.epam.dlab.rest.mappers.JsonProcessingExceptionMapper;
import com.epam.dlab.rest.mappers.RuntimeExceptionMapper;
import com.epam.dlab.util.ServiceUtils;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ProvisioningServiceApplication extends Application<ProvisioningServiceApplicationConfiguration> {

	public static void main(String[] args) throws Exception {
		if (ServiceUtils.printAppVersion(ProvisioningServiceApplication.class, args)) {
			return;
		}
		new ProvisioningServiceApplication().run(args);
	}

	@Override
	public void initialize(Bootstrap<ProvisioningServiceApplicationConfiguration> bootstrap) {
		bootstrap.addBundle(new TemplateConfigBundle(
				new TemplateConfigBundleConfiguration().fileIncludePath(ServiceUtils.getConfPath())
		));
	}

	@Override
	public void run(ProvisioningServiceApplicationConfiguration configuration, Environment environment) {
		DlabProcess.getInstance().setProcessTimeout(configuration.getProcessTimeout());
		DlabProcess.getInstance().setMaxProcessesPerBox(configuration.getProcessMaxThreadsPerJvm());
		DlabProcess.getInstance().setMaxProcessesPerUser(configuration.getProcessMaxThreadsPerUser());

		CloudModule cloudModule = CloudModuleConfigurator.getCloudModule(configuration);
		Injector injector = Guice.createInjector(ModuleFactory.getModule(configuration, environment), cloudModule);
		cloudModule.init(environment, injector);

		injector.getInstance(SecurityFactory.class).configure(injector, environment);

		environment.lifecycle().manage(injector.getInstance(DirectoriesCreator.class));
		environment.lifecycle().manage(injector.getInstance(DockerWarmuper.class));

		JerseyEnvironment jersey = environment.jersey();
		jersey.register(configuration.getCloudProvider());
		jersey.register(new RuntimeExceptionMapper());
		jersey.register(new JsonProcessingExceptionMapper());

		jersey.register(injector.getInstance(DockerResource.class));
		jersey.register(injector.getInstance(GitExploratoryResource.class));
		jersey.register(injector.getInstance(LibraryResource.class));
		jersey.register(injector.getInstance(InfrastructureResource.class));
		jersey.register(injector.getInstance(ImageResource.class));
		jersey.register(injector.getInstance(BackupResource.class));
		jersey.register(injector.getInstance(KeyResource.class));

	}
}
