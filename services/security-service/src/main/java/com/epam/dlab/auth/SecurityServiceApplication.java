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

package com.epam.dlab.auth;

import com.epam.dlab.auth.modules.ModuleFactory;
import com.epam.dlab.auth.modules.SecurityServiceModule;
import com.epam.dlab.cloud.CloudModule;
import com.epam.dlab.rest.mappers.AuthenticationExceptionMapper;
import com.epam.dlab.util.ServiceUtils;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class SecurityServiceApplication extends Application<SecurityServiceConfiguration> {

	private static final Logger LOG = LoggerFactory.getLogger(SecurityServiceApplication.class);

	public static void main(String[] args) throws Exception {
		if (ServiceUtils.printAppVersion(SecurityServiceApplication.class, args)) {
			return;
		}
		LOG.debug("Starting Security Service Application with params: {}", String.join(",", args));
		new SecurityServiceApplication().run(args);
	}

	@Override
	public void initialize(Bootstrap<SecurityServiceConfiguration> bootstrap) {
		bootstrap.addBundle(new TemplateConfigBundle(
				new TemplateConfigBundleConfiguration().fileIncludePath(ServiceUtils.getConfPath())
		));
	}

	@Override
	public void run(SecurityServiceConfiguration conf, Environment env) {
		CloudModule cloudModule = ModuleFactory.getCloudProviderModule(conf);
		Injector injector = Guice.createInjector(new SecurityServiceModule(conf, env), cloudModule);
		env.jersey().register(new AuthenticationExceptionMapper());
		cloudModule.init(env, injector);
	}
}
