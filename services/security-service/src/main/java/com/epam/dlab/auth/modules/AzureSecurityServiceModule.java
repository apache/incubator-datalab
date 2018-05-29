/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
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

package com.epam.dlab.auth.modules;

import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.UserVerificationService;
import com.epam.dlab.auth.azure.AzureAuthenticationService;
import com.epam.dlab.auth.azure.DlabExceptionMapper;
import com.epam.dlab.auth.resources.SynchronousLdapAuthenticationService;
import com.epam.dlab.cloud.CloudModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;

import java.io.IOException;

public class AzureSecurityServiceModule extends CloudModule {
	private final SecurityServiceConfiguration conf;

	AzureSecurityServiceModule(SecurityServiceConfiguration configuration) {
		this.conf = configuration;
	}

	@Override
	protected void configure() {
		bind(UserVerificationService.class).toInstance(SecurityServiceModule.defaultUserVerificationService());
	}

	@Override
	public void init(Environment environment, Injector injector) {

		environment.jersey().register(new DlabExceptionMapper());
		if (conf.getAzureLoginConfiguration().isUseLdap()) {
			environment.jersey().register(injector.getInstance(SynchronousLdapAuthenticationService.class));
		} else {
			environment.jersey().register(injector.getInstance(AzureAuthenticationService.class));
		}
	}

	@Provides
	@Singleton
	private AzureAuthenticationService azureAuthenticationService(UserInfoDAO userInfoDao) throws IOException {
		return new AzureAuthenticationService(conf, userInfoDao, conf.getAzureLoginConfiguration());
	}
}
