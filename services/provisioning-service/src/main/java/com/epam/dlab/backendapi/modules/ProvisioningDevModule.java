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
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.DockerWarmuper;
import com.epam.dlab.backendapi.core.MetadataHolder;
import com.epam.dlab.backendapi.core.commands.CommandExecutorMock;
import com.epam.dlab.backendapi.core.commands.ICommandExecutor;
import com.epam.dlab.backendapi.core.response.handlers.dao.CallbackHandlerDao;
import com.epam.dlab.backendapi.core.response.handlers.dao.FileSystemCallbackHandlerDao;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.service.RestoreCallbackHandlerService;
import com.epam.dlab.backendapi.service.CheckInactivityService;
import com.epam.dlab.backendapi.service.RestoreCallbackHandlerService;
import com.epam.dlab.backendapi.service.impl.CheckInactivityServiceImpl;
import com.epam.dlab.backendapi.service.impl.ProjectServiceImpl;
import com.epam.dlab.backendapi.service.impl.RestoreCallbackHandlerServiceImpl;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.name.Names;
import io.dropwizard.setup.Environment;

import javax.ws.rs.core.Response;

/**
 * Mock class for an application configuration of Provisioning Service for tests.
 */
public class ProvisioningDevModule extends ModuleBase<ProvisioningServiceApplicationConfiguration> implements
		SecurityAPI, DockerAPI {

	private static final String TOKEN = "token123";
	private static final String OPERATION_IS_NOT_SUPPORTED = "Operation is not supported";

	/**
	 * Instantiates an application configuration of Provisioning Service for tests.
	 *
	 * @param configuration application configuration of Provisioning Service.
	 * @param environment   environment of Provisioning Service.
	 */
	ProvisioningDevModule(ProvisioningServiceApplicationConfiguration configuration, Environment environment) {
		super(configuration, environment);
	}

	@Override
	protected void configure() {
		bind(ProvisioningServiceApplicationConfiguration.class).toInstance(configuration);
		bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.SECURITY_SERVICE_NAME)).toInstance
				(createAuthenticationService());
		bind(RESTService.class).toInstance(configuration.getSelfFactory().build(environment, ServiceConsts
				.SELF_SERVICE_NAME));
		bind(MetadataHolder.class).to(DockerWarmuper.class);
		bind(ICommandExecutor.class).toInstance(new CommandExecutorMock(configuration.getCloudProvider()));
		bind(ObjectMapper.class).toInstance(new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true));
		bind(CallbackHandlerDao.class).to(FileSystemCallbackHandlerDao.class);
		bind(RestoreCallbackHandlerService.class).to(RestoreCallbackHandlerServiceImpl.class);
		bind(CheckInactivityService.class).to(CheckInactivityServiceImpl.class);
		bind(ProjectService.class).to(ProjectServiceImpl.class);
	}

	/**
	 * Creates and returns the mock object for authentication service.
	 */
	@SuppressWarnings("unchecked")
	private RESTService createAuthenticationService() {
		return new RESTService() {
			@Override
			public <T> T post(String path, Object parameter, Class<T> clazz) {
				if (LOGIN.equals(path)) {
					return authorize((UserCredentialDTO) parameter);
				} else if (GET_USER_INFO.equals(path) && TOKEN.equals(parameter) && clazz.equals(UserInfo.class)) {
					return (T) getUserInfo();
				}
				throw new UnsupportedOperationException(OPERATION_IS_NOT_SUPPORTED);
			}

			private <T> T authorize(UserCredentialDTO credential) {
				if ("test".equals(credential.getUsername())) {
					return (T) Response.ok(TOKEN).build();
				} else {
					return (T) Response.status(Response.Status.UNAUTHORIZED)
							.entity("Username or password is invalid")
							.build();
				}
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

	/**
	 * Create and return UserInfo object.
	 */
	private UserInfo getUserInfo() {
		UserInfo userInfo = new UserInfo("test", TOKEN);
		userInfo.addRole("test");
		userInfo.addRole("dev");
		return userInfo;
	}
}
