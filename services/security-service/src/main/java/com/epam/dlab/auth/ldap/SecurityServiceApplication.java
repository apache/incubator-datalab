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

package com.epam.dlab.auth.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.ldap.api.LdapAuthenticationService;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;


public class SecurityServiceApplication extends Application<SecurityServiceConfiguration> {

	private final static Logger LOG = LoggerFactory.getLogger(SecurityServiceApplication.class);
	
	public static void main(String[] args) throws Exception {
		
		String[] params = null;
		
		if(args.length != 0 ) {
			params = args;
		} else {
			params = new String[] { "server", "application.yml" };
		}
		LOG.debug("Starting Security Service Application with params: {}",String.join(",", params));
		new SecurityServiceApplication().run(params);
	}

	@Override
	public void initialize(Bootstrap<SecurityServiceConfiguration> bootstrap) {
	}

	@Override
	public void run(SecurityServiceConfiguration conf, Environment env) throws Exception {
		env.jersey().register( new LdapAuthenticationService(conf,env) );
	}

}
