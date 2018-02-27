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
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthorizer;
import com.epam.dlab.backendapi.dao.BackupDao;
import com.epam.dlab.backendapi.dao.BackupDaoImpl;
import com.epam.dlab.backendapi.dao.ImageExploratoryDao;
import com.epam.dlab.backendapi.dao.ImageExploratoryDaoImpl;
import com.epam.dlab.backendapi.domain.EnvStatusListener;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.*;
import com.epam.dlab.backendapi.service.impl.*;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserCredentialDTO;
import com.epam.dlab.mongo.MongoService;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.epam.dlab.rest.contracts.SecurityAPI;
import com.google.inject.name.Names;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.setup.Environment;
import org.mockito.stubbing.Answer;

import javax.ws.rs.core.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mock class for an application configuration of SelfService for developer mode.
 */
public class DevModule extends ModuleBase<SelfServiceApplicationConfiguration> implements SecurityAPI, DockerAPI {

	public static final String TOKEN = "token123";
	private static final String LOGIN_NAME = "test";

	/**
	 * Instantiates an application configuration of SelfService for developer mode.
	 *
	 * @param configuration application configuration of SelfService.
	 * @param environment   environment of SelfService.
	 */
	public DevModule(SelfServiceApplicationConfiguration configuration, Environment environment) {
		super(configuration, environment);
	}

	@Override
	protected void configure() {
		bind(SelfServiceApplicationConfiguration.class).toInstance(configuration);
		bind(MongoService.class).toInstance(configuration.getMongoFactory().build(environment));
		bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.SECURITY_SERVICE_NAME))
				.toInstance(createAuthenticationService());
		requestStaticInjection(EnvStatusListener.class, RequestId.class, RequestBuilder.class);
		bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.PROVISIONING_SERVICE_NAME))
				.toInstance(configuration.getProvisioningFactory()
						.build(environment, ServiceConsts.PROVISIONING_SERVICE_NAME));
		bind(ImageExploratoryService.class).to(ImageExploratoryServiceImpl.class);
		bind(ImageExploratoryDao.class).to(ImageExploratoryDaoImpl.class);
		bind(BackupService.class).to(BackupServiceImpl.class);
		bind(BackupDao.class).to(BackupDaoImpl.class);
		bind(ExploratoryService.class).to(ExploratoryServiceImpl.class);
		bind(SystemUserInfoService.class).to(SystemUserInfoServiceImpl.class);
		bind(Authorizer.class).to(SelfServiceSecurityAuthorizer.class);
		bind(AccessKeyService.class).to(AccessKeyServiceImpl.class);
		bind(GitCredentialService.class).to(GitCredentialServiceImpl.class);
		bind(ComputationalService.class).to(ComputationalServiceImpl.class);
		bind(LibraryService.class).to(LibraryServiceImpl.class);
		bind(SchedulerJobService.class).to(SchedulerJobServiceImpl.class);
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
		RESTService result = mock(RESTService.class);
		when(result.post(eq(LOGIN), any(), any())).then(
				(Answer<Response>) invocation -> {
					for (Object o : invocation.getArguments()) {
						if (o instanceof UserCredentialDTO) {
							UserCredentialDTO credential = (UserCredentialDTO) o;
							if (LOGIN_NAME.equals(credential.getUsername())) {
								return Response.ok(TOKEN).build();
							}
						}
					}
					return Response.status(Response.Status.UNAUTHORIZED).entity("Username or password are not valid")
							.build();
				});
		when(result.post(eq(GET_USER_INFO), eq(TOKEN), eq(UserInfo.class)))
				.thenReturn(getUserInfo());
		return result;
	}
}
