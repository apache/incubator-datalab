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

import com.epam.dlab.auth.resources.LdapAuthenticationService;
import com.epam.dlab.utils.ServiceUtils;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SecurityServiceApplication extends Application<SecurityServiceConfiguration> {

	private final static Logger LOG = LoggerFactory.getLogger(SecurityServiceApplication.class);
	
	public static void main(String[] args) throws Exception {
		
		String[] params = null;
		
		if(args.length != 0 ) {
			params = args;
		} else {
			params = new String[] { "server", "security.yml" };
		}
		LOG.debug("Starting Security Service Application with params: {}",String.join(",", params));
		new SecurityServiceApplication().run(params);
	}

	@Override
	public void initialize(Bootstrap<SecurityServiceConfiguration> bootstrap) {
		//bootstrap.addBundle(new TemplateConfigBundle());
        bootstrap.addBundle(new TemplateConfigBundle(
        		new TemplateConfigBundleConfiguration().fileIncludePath(ServiceUtils.getConfPath())
        ));
	}

	@Override
	public void run(SecurityServiceConfiguration conf, Environment env) throws Exception {
		env.jersey().register( new LdapAuthenticationService(conf,env) );
	}

}
