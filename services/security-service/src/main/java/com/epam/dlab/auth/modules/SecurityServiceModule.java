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

import com.epam.dlab.ModuleBase;
import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.UserVerificationService;
import com.epam.dlab.auth.dao.LdapUserDAO;
import com.epam.dlab.auth.dao.UserInfoDAODumbImpl;
import com.epam.dlab.auth.dao.UserInfoDAOMongoImpl;
import com.epam.dlab.auth.service.DexOauthService;
import com.epam.dlab.auth.service.DexOauthServiceImpl;
import com.epam.dlab.mongo.MongoService;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;

@Slf4j
public class SecurityServiceModule extends ModuleBase<SecurityServiceConfiguration> {

	private static final String REST_CLIENT = "restClient";

	public SecurityServiceModule(SecurityServiceConfiguration configuration, Environment environment) {
		super(configuration, environment);
	}

	@Override
	protected void configure() {
		bind(SecurityServiceConfiguration.class).toInstance(configuration);
		bind(HttpClient.class).toInstance(
				new HttpClientBuilder(environment)
						.using(new HttpClientConfiguration())
						.build(REST_CLIENT));
		bind(DexOauthService.class).to(DexOauthServiceImpl.class);
		if (configuration.isUserInfoPersistenceEnabled()) {
			bind(UserInfoDAO.class).to(UserInfoDAOMongoImpl.class);
		} else {
			bind(UserInfoDAO.class).to(UserInfoDAODumbImpl.class);
		}
	}

	@Provides
	@Singleton
	private MongoService mongoService() {
		return configuration.getMongoFactory().build(environment);
	}

	@Provides
	@Singleton
	private LdapUserDAO ldapUserDAOWithoutCache() {
		return new LdapUserDAO(configuration, false);
	}

	public static UserVerificationService defaultUserVerificationService() {
		return (username, userInfo) -> log.debug("No additional user verification configured");
	}
}
