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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserVerificationService;
import com.epam.dlab.auth.aws.dao.AwsUserDAO;
import com.epam.dlab.auth.aws.dao.AwsUserDAOImpl;
import com.epam.dlab.auth.aws.service.AwsCredentialRefreshService;
import com.epam.dlab.auth.aws.service.AwsUserVerificationService;
import com.epam.dlab.auth.resources.SynchronousLdapAuthenticationService;
import com.epam.dlab.cloud.CloudModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;

public class AwsSecurityServiceModule extends CloudModule {
	private final SecurityServiceConfiguration conf;

	AwsSecurityServiceModule(SecurityServiceConfiguration conf) {
		this.conf = conf;
	}

	@Override
	protected void configure() {
		if (conf.isAwsUserIdentificationEnabled()) {
			bind(AwsUserDAO.class).to(AwsUserDAOImpl.class);
			bind(UserVerificationService.class).to(AwsUserVerificationService.class);
		} else {
			bind(UserVerificationService.class).toInstance(SecurityServiceModule.defaultUserVerificationService());
		}
	}

	@Override
	public void init(Environment environment, Injector injector) {
		environment.jersey().register(injector.getInstance(SynchronousLdapAuthenticationService.class));
		if (conf.isAwsUserIdentificationEnabled()) {
			environment.lifecycle().manage(injector.getInstance(AwsCredentialRefreshService.class));
		}
	}

	@Provides
	@Singleton
	private AWSCredentials awsCredentials(DefaultAWSCredentialsProviderChain providerChain) {
		if (conf.isDevMode()) {
			return devAwsCredentials();
		} else {
			return providerChain.getCredentials();
		}
	}

	private AWSCredentials devAwsCredentials() {
		return new AWSCredentials() {
			@Override
			public String getAWSAccessKeyId() {
				return "access_key";
			}

			@Override
			public String getAWSSecretKey() {
				return "secret_key";
			}
		};
	}

	@Provides
	@Singleton
	public AWSCredentialsProvider defaultAWSCredentialsProviderChain() {
		return new DefaultAWSCredentialsProviderChain();
	}
}
