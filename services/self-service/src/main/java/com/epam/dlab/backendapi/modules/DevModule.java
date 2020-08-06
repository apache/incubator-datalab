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
import com.epam.dlab.auth.contract.SecurityAPI;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthorizer;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.AuditDAO;
import com.epam.dlab.backendapi.dao.AuditDAOImpl;
import com.epam.dlab.backendapi.dao.BackupDAO;
import com.epam.dlab.backendapi.dao.BackupDAOImpl;
import com.epam.dlab.backendapi.dao.BaseBillingDAO;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.dao.EndpointDAO;
import com.epam.dlab.backendapi.dao.EndpointDAOImpl;
import com.epam.dlab.backendapi.dao.ImageExploratoryDAO;
import com.epam.dlab.backendapi.dao.ImageExploratoryDAOImpl;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.dao.ProjectDAOImpl;
import com.epam.dlab.backendapi.dao.UserGroupDAO;
import com.epam.dlab.backendapi.dao.UserGroupDAOImpl;
import com.epam.dlab.backendapi.dao.UserRoleDAO;
import com.epam.dlab.backendapi.dao.UserRoleDAOImpl;
import com.epam.dlab.backendapi.service.AccessKeyService;
import com.epam.dlab.backendapi.service.ApplicationSettingService;
import com.epam.dlab.backendapi.service.ApplicationSettingServiceImpl;
import com.epam.dlab.backendapi.service.AuditService;
import com.epam.dlab.backendapi.service.BackupService;
import com.epam.dlab.backendapi.service.BucketService;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ExternalLibraryService;
import com.epam.dlab.backendapi.service.GitCredentialService;
import com.epam.dlab.backendapi.service.GuacamoleService;
import com.epam.dlab.backendapi.service.ImageExploratoryService;
import com.epam.dlab.backendapi.service.InactivityService;
import com.epam.dlab.backendapi.service.KeycloakService;
import com.epam.dlab.backendapi.service.KeycloakServiceImpl;
import com.epam.dlab.backendapi.service.LibraryService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.service.ReuploadKeyService;
import com.epam.dlab.backendapi.service.SchedulerJobService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.backendapi.service.SecurityServiceImpl;
import com.epam.dlab.backendapi.service.SystemInfoService;
import com.epam.dlab.backendapi.service.TagService;
import com.epam.dlab.backendapi.service.TagServiceImpl;
import com.epam.dlab.backendapi.service.UserGroupService;
import com.epam.dlab.backendapi.service.UserRoleService;
import com.epam.dlab.backendapi.service.UserRoleServiceImpl;
import com.epam.dlab.backendapi.service.UserSettingService;
import com.epam.dlab.backendapi.service.UserSettingServiceImpl;
import com.epam.dlab.backendapi.service.impl.AccessKeyServiceImpl;
import com.epam.dlab.backendapi.service.impl.AuditServiceImpl;
import com.epam.dlab.backendapi.service.impl.BackupServiceImpl;
import com.epam.dlab.backendapi.service.impl.BucketServiceImpl;
import com.epam.dlab.backendapi.service.impl.ComputationalServiceImpl;
import com.epam.dlab.backendapi.service.impl.EndpointServiceImpl;
import com.epam.dlab.backendapi.service.impl.EnvironmentServiceImpl;
import com.epam.dlab.backendapi.service.impl.ExploratoryServiceImpl;
import com.epam.dlab.backendapi.service.impl.GitCredentialServiceImpl;
import com.epam.dlab.backendapi.service.impl.GuacamoleServiceImpl;
import com.epam.dlab.backendapi.service.impl.ImageExploratoryServiceImpl;
import com.epam.dlab.backendapi.service.impl.InactivityServiceImpl;
import com.epam.dlab.backendapi.service.impl.LibraryServiceImpl;
import com.epam.dlab.backendapi.service.impl.MavenCentralLibraryService;
import com.epam.dlab.backendapi.service.impl.ProjectServiceImpl;
import com.epam.dlab.backendapi.service.impl.ReuploadKeyServiceImpl;
import com.epam.dlab.backendapi.service.impl.SchedulerJobServiceImpl;
import com.epam.dlab.backendapi.service.impl.SystemInfoServiceImpl;
import com.epam.dlab.backendapi.service.impl.UserGroupServiceImpl;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.mongo.MongoService;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.google.inject.name.Names;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.logging.LoggingFeature;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.client.Client;
import java.util.EnumSet;

/**
 * Mock class for an application configuration of SelfService for developer mode.
 */
public class DevModule extends ModuleBase<SelfServiceApplicationConfiguration> implements SecurityAPI, DockerAPI {

	public static final String TOKEN = "token123";

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
		configureCors(environment);
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
		bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.BUCKET_SERVICE_NAME))
				.toInstance(configuration.getBucketFactory()
						.build(environment, ServiceConsts.BUCKET_SERVICE_NAME));
		bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.BILLING_SERVICE_NAME))
				.toInstance(configuration.getBillingFactory()
						.build(environment, ServiceConsts.BILLING_SERVICE_NAME));
		bind(ImageExploratoryService.class).to(ImageExploratoryServiceImpl.class);
		bind(ImageExploratoryDAO.class).to(ImageExploratoryDAOImpl.class);
		bind(BackupService.class).to(BackupServiceImpl.class);
		bind(BackupDAO.class).to(BackupDAOImpl.class);
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
		bind(ReuploadKeyService.class).to(ReuploadKeyServiceImpl.class);
		bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.MAVEN_SEARCH_API))
				.toInstance(configuration.getMavenApiFactory().build(environment, ServiceConsts.MAVEN_SEARCH_API));

		bind(ExternalLibraryService.class).to(MavenCentralLibraryService.class);
		bind(SystemInfoService.class).to(SystemInfoServiceImpl.class);
		bind(UserGroupService.class).to(UserGroupServiceImpl.class);
		bind(UserRoleService.class).to(UserRoleServiceImpl.class);
		bind(UserRoleDAO.class).to(UserRoleDAOImpl.class);
		bind(UserGroupDAO.class).to(UserGroupDAOImpl.class);
		bind(ApplicationSettingService.class).to(ApplicationSettingServiceImpl.class);
		bind(UserSettingService.class).to(UserSettingServiceImpl.class);
		bind(GuacamoleService.class).to(GuacamoleServiceImpl.class);
		bind(EndpointService.class).to(EndpointServiceImpl.class);
		bind(EndpointDAO.class).to(EndpointDAOImpl.class);
		bind(ProjectService.class).to(ProjectServiceImpl.class);
		bind(AuditService.class).to(AuditServiceImpl.class);
		bind(ProjectDAO.class).to(ProjectDAOImpl.class);
		bind(BillingDAO.class).to(BaseBillingDAO.class);
		bind(AuditDAO.class).to(AuditDAOImpl.class);
		bind(BucketService.class).to(BucketServiceImpl.class);
	}

	private void configureCors(Environment environment) {
		final FilterRegistration.Dynamic cors =
				environment.servlets().addFilter("CORS", CrossOriginFilter.class);

		cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
		cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin," +
				"Authorization");
		cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
		cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");

		cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

	}
}
