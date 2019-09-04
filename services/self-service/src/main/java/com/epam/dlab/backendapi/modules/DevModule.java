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

package com.epam.dlab.backendapi.modules;

import com.epam.dlab.ModuleBase;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.contract.SecurityAPI;
import com.epam.dlab.auth.dto.UserCredentialDTO;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthorizer;
import com.epam.dlab.backendapi.dao.*;
import com.epam.dlab.backendapi.service.*;
import com.epam.dlab.backendapi.service.impl.*;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.mongo.MongoService;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.epam.dlab.rest.dto.ErrorDTO;
import com.google.inject.name.Names;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.logging.LoggingFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Mock class for an application configuration of SelfService for developer mode.
 */
public class DevModule extends ModuleBase<SelfServiceApplicationConfiguration> implements SecurityAPI, DockerAPI {

	public static final String TOKEN = "token123";
	private static final String LOGIN_NAME = "test";
	private static final String OPERATION_IS_NOT_SUPPORTED = "Operation is not supported";
	private static final String LOGOUT = "logout";

	/**
	 * Instantiates an application configuration of SelfService for developer mode.
	 *
	 * @param configuration application configuration of SelfService.
	 * @param environment   environment of SelfService.
	 */
	DevModule(SelfServiceApplicationConfiguration configuration, Environment environment) {
		super(configuration, environment);
	}

	@Override
	protected void configure() {
		final Client httpClient =
				new JerseyClientBuilder(environment)
						.using(configuration.getJerseyClientConfiguration())
						.build("httpClient")
						.register(new LoggingFeature());
		bind(SecurityService.class).to(SecurityServiceImpl.class);
		bind(KeycloakService.class).to(KeycloakServiceImpl.class);
		bind(Client.class).toInstance(httpClient);
		bind(SelfServiceApplicationConfiguration.class).toInstance(configuration);
		bind(MongoService.class).toInstance(configuration.getMongoFactory().build(environment));
		bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.PROVISIONING_SERVICE_NAME))
				.toInstance(configuration.getProvisioningFactory()
						.build(environment, ServiceConsts.PROVISIONING_SERVICE_NAME));
		bind(ImageExploratoryService.class).to(ImageExploratoryServiceImpl.class);
		bind(ImageExploratoryDao.class).to(ImageExploratoryDaoImpl.class);
		bind(BackupService.class).to(BackupServiceImpl.class);
		bind(BackupDao.class).to(BackupDaoImpl.class);
		bind(ExploratoryService.class).to(ExploratoryServiceImpl.class);
		bind(TagService.class).to(TagServiceImpl.class);
		bind(InactivityService.class).to(InactivityServiceImpl.class);
		bind(Authorizer.class).to(SelfServiceSecurityAuthorizer.class);
		bind(AccessKeyService.class).to(AccessKeyServiceImpl.class);
		bind(GitCredentialService.class).to(GitCredentialServiceImpl.class);
		bind(ComputationalService.class).to(ComputationalServiceImpl.class);
		bind(LibraryService.class).to(LibraryServiceImpl.class);
		bind(SchedulerJobService.class).to(SchedulerJobServiceImpl.class);
		bind(EnvironmentService.class).to(EnvironmentServiceImpl.class);
		bind(EdgeService.class).to(EdgeServiceImpl.class);
		bind(ReuploadKeyService.class).to(ReuploadKeyServiceImpl.class);
		bind(UserResourceService.class).to(UserResourceServiceImpl.class);
		bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.MAVEN_SEARCH_API))
				.toInstance(configuration.getMavenApiFactory().build(environment, ServiceConsts.MAVEN_SEARCH_API));

		bind(ExternalLibraryService.class).to(MavenCentralLibraryService.class);
		bind(SystemInfoService.class).to(SystemInfoServiceImpl.class);
		bind(UserGroupService.class).to(UserGroupServiceImpl.class);
		bind(UserRoleService.class).to(UserRoleServiceImpl.class);
		bind(UserRoleDao.class).to(UserRoleDaoImpl.class);
		bind(UserGroupDao.class).to(UserGroupDaoImpl.class);
		bind(ApplicationSettingService.class).to(ApplicationSettingServiceImpl.class);
		bind(UserSettingService.class).to(UserSettingServiceImpl.class);
		bind(GuacamoleService.class).to(GuacamoleServiceImpl.class);
		bind(EndpointService.class).to(EndpointServiceImpl.class);
		bind(EndpointDAO.class).to(EndpointDAOImpl.class);
		bind(ProjectService.class).to(ProjectServiceImpl.class);
		bind(ProjectDAO.class).to(ProjectDAOImpl.class);
	}

	/**
	 * Create and return UserInfo object.
	 */
	private UserInfo getUserInfo() {
		UserInfo userInfo = new UserInfo(LOGIN_NAME, TOKEN);
		userInfo.addRole("test");
		userInfo.addRole("dev");
		return userInfo;
	}

	/**
	 * Creates and returns the mock object for authentication service.
	 */
	private RESTService createAuthenticationService() {
		return new RESTService() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> T post(String path, Object parameter, Class<T> clazz) {
				if (LOGIN.equals(path)) {
					return authorize((UserCredentialDTO) parameter);
				} else if (GET_USER_INFO.equals(path) && TOKEN.equals(parameter) && clazz.equals(UserInfo.class)) {
					return (T) getUserInfo();
				} else if (GET_USER_INFO.equals(path) && !TOKEN.equals(parameter) && clazz.equals(UserInfo.class)) {
					return null;
				} else if (LOGOUT.equals(path)) {
					return (T) Response.ok().build();
				}
				throw new UnsupportedOperationException(OPERATION_IS_NOT_SUPPORTED);
			}

			@SuppressWarnings("unchecked")
			private <T> T authorize(UserCredentialDTO credential) {
				if (LOGIN_NAME.equals(credential.getUsername())) {
					return (T) Response.ok(TOKEN).build();
				} else {
					return (T) unauthorized();
				}
			}

			private Response unauthorized() {
				return Response.status(Response.Status.UNAUTHORIZED)
						.entity(new ErrorDTO(Response.Status.UNAUTHORIZED.getStatusCode(), "Username or password" +
								" is invalid"))
						.type(MediaType.APPLICATION_JSON_TYPE)
						.build();
			}

			@Override
			public <T> T get(String path, Class<T> clazz) {
				throw new UnsupportedOperationException(OPERATION_IS_NOT_SUPPORTED);
			}

			@Override
			public <T> T get(String path, String accessToken, Class<T> clazz) {
				throw new UnsupportedOperationException(OPERATION_IS_NOT_SUPPORTED);
			}

			@Override
			public <T> T post(String path, String accessToken, Object parameter, Class<T> clazz) {
				throw new UnsupportedOperationException(OPERATION_IS_NOT_SUPPORTED);
			}
		};
	}
}
