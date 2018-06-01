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
import com.epam.dlab.auth.UserVerificationService;
import com.epam.dlab.auth.gcp.resources.GcpOauth2SecurityResource;
import com.epam.dlab.auth.gcp.service.GcpAuthenticationService;
import com.epam.dlab.auth.oauth2.Oauth2AuthenticationService;
import com.epam.dlab.auth.resources.SynchronousLdapAuthenticationService;
import com.epam.dlab.cloud.CloudModule;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.epam.dlab.config.gcp.GcpLoginConfiguration;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GcpSecurityServiceModule extends CloudModule {
	private static final List<String> SCOPES = Arrays.asList("https://www.googleapis.com/auth/plus.login", "email",
			"profile", "https://www.googleapis.com/auth/plus.me");
	private final com.epam.dlab.config.gcp.GcpLoginConfiguration conf;

	GcpSecurityServiceModule(SecurityServiceConfiguration conf) {
		this.conf = conf.getGcpLoginConfiguration();
	}

	@Override
	protected void configure() {
		if (conf.isOauth2authenticationEnabled()) {
			bind(Oauth2AuthenticationService.class).to(GcpAuthenticationService.class);
		}
		bind(GcpLoginConfiguration.class).toInstance(conf);
		bind(UserVerificationService.class).toInstance(SecurityServiceModule.defaultUserVerificationService());
	}

	@Override
	public void init(Environment environment, Injector injector) {
		environment.jersey().register(injector.getInstance(SynchronousLdapAuthenticationService.class));
		if (conf.isOauth2authenticationEnabled()) {
			environment.jersey().register(injector.getInstance(GcpOauth2SecurityResource.class));
		}
	}

	@Provides
	@Singleton
	private Cache<String, Object> userStateCache() {
		return CacheBuilder.newBuilder().expireAfterWrite(conf.getUserStateCacheExpirationTime(), TimeUnit.HOURS)
				.maximumSize(conf.getUserStateCacheSize()).build();
	}


	@Provides
	@Singleton
	private HttpTransport httpTransport() {
		return new NetHttpTransport();
	}

	@Provides
	@Singleton
	private AuthorizationCodeFlow authorizationCodeFlow(HttpTransport httpTransport, JacksonFactory jacksonFactory) {
		return new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jacksonFactory, conf.getClientId(), conf.getClientSecret(), SCOPES)
				.build();
	}

}
