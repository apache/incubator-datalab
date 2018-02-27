/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
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

package com.epam.dlab.backendapi.modules;

import com.epam.dlab.ModuleBase;
import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.SystemUserInfoServiceImpl;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.DockerWarmuper;
import com.epam.dlab.backendapi.core.MetadataHolder;
import com.epam.dlab.backendapi.core.commands.CommandExecutorMock;
import com.epam.dlab.backendapi.core.commands.ICommandExecutor;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.mongo.MongoService;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.epam.dlab.rest.contracts.SecurityAPI;
import com.google.inject.name.Names;
import io.dropwizard.setup.Environment;

import javax.ws.rs.core.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mock class for an application configuration of Provisioning Service for tests.
 */
public class ProvisioningDevModule extends ModuleBase<ProvisioningServiceApplicationConfiguration> implements SecurityAPI, DockerAPI {

	public static final String TOKEN = "token123";

	/**
	 * Instantiates an application configuration of Provisioning Service for tests.
	 *
	 * @param configuration application configuration of Provisioning Service.
	 * @param environment   environment of Provisioning Service.
	 */
	public ProvisioningDevModule(ProvisioningServiceApplicationConfiguration configuration, Environment environment) {
		super(configuration, environment);
	}

	@Override
	protected void configure() {
		bind(ProvisioningServiceApplicationConfiguration.class).toInstance(configuration);
		bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.SECURITY_SERVICE_NAME)).toInstance(createAuthenticationService());
		bind(RESTService.class).toInstance(configuration.getSelfFactory().build(environment, ServiceConsts.SELF_SERVICE_NAME));
		bind(MetadataHolder.class).to(DockerWarmuper.class);
		bind(ICommandExecutor.class).toInstance(new CommandExecutorMock(configuration.getCloudProvider()));
		bind(SystemUserInfoService.class).to(SystemUserInfoServiceImpl.class);
		bind(MongoService.class).toInstance(configuration.getMongoFactory().build(environment));
	}

	/**
	 * Creates and returns the mock object for authentication service.
	 */
	private RESTService createAuthenticationService() {
		RESTService result = mock(RESTService.class);
		when(result.post(eq(LOGIN), any(), any())).then(invocationOnMock -> Response.ok(TOKEN).build());
		when(result.post(eq(GET_USER_INFO), eq(TOKEN), eq(UserInfo.class)))
				.thenReturn(new UserInfo("test", TOKEN));
		return result;
	}
}
