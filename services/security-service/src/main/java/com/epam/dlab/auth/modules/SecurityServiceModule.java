/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.auth.modules;

import com.epam.dlab.ModuleBase;
import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.UserVerificationService;
import com.epam.dlab.auth.dao.LdapUserDAO;
import com.epam.dlab.auth.dao.LdapUserDAOImpl;
import com.epam.dlab.auth.dao.UserInfoDAODumbImpl;
import com.epam.dlab.auth.dao.UserInfoDAOMongoImpl;
import com.epam.dlab.auth.service.AuthenticationService;
import com.epam.dlab.auth.service.impl.LdapAuthenticationService;
import com.epam.dlab.mongo.MongoService;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityServiceModule extends ModuleBase<SecurityServiceConfiguration> {

	public SecurityServiceModule(SecurityServiceConfiguration configuration, Environment environment) {
		super(configuration, environment);
	}

	@Override
	protected void configure() {
		bind(SecurityServiceConfiguration.class).toInstance(configuration);
		bind(LdapUserDAO.class).to(LdapUserDAOImpl.class);
		bind(AuthenticationService.class).to(LdapAuthenticationService.class);
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

	public static UserVerificationService defaultUserVerificationService() {
		return userInfo -> log.debug("No additional user verification configured");
	}
}
