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


package com.epam.dlab.backendapi;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.DirectoriesCreator;
import com.epam.dlab.backendapi.core.DockerWarmuper;
import com.epam.dlab.backendapi.core.response.handlers.ComputationalConfigure;
import com.epam.dlab.backendapi.modules.CloudModuleConfigurator;
import com.epam.dlab.backendapi.modules.ModuleFactory;
import com.epam.dlab.backendapi.resources.*;
import com.epam.dlab.backendapi.resources.base.KeyResource;
import com.epam.dlab.backendapi.service.impl.RestoreCallbackHandlerServiceImpl;
import com.epam.dlab.cloud.CloudModule;
import com.epam.dlab.process.model.DlabProcess;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.mappers.JsonProcessingExceptionMapper;
import com.epam.dlab.rest.mappers.RuntimeExceptionMapper;
import com.epam.dlab.util.ServiceUtils;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.ahus1.keycloak.dropwizard.AbstractKeycloakAuthenticator;
import de.ahus1.keycloak.dropwizard.KeycloakBundle;
import de.ahus1.keycloak.dropwizard.KeycloakConfiguration;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.keycloak.KeycloakSecurityContext;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

public class ProvisioningServiceApplication extends Application<ProvisioningServiceApplicationConfiguration> {

	public static void main(String[] args) throws Exception {
		if (ServiceUtils.printAppVersion(ProvisioningServiceApplication.class, args)) {
			return;
		}
		new ProvisioningServiceApplication().run(args);
	}

	@Override
	public void initialize(Bootstrap<ProvisioningServiceApplicationConfiguration> bootstrap) {
		bootstrap.addBundle(new TemplateConfigBundle(
				new TemplateConfigBundleConfiguration().fileIncludePath(ServiceUtils.getConfPath())
		));
		bootstrap.addBundle(new KeycloakBundle<ProvisioningServiceApplicationConfiguration>() {
			@Override
			protected KeycloakConfiguration getKeycloakConfiguration(ProvisioningServiceApplicationConfiguration configuration) {
				return configuration.getKeycloakConfiguration();
			}

			@Override
			protected Class<? extends Principal> getUserClass() {
				return UserInfo.class;
			}

			@Override
			protected Authorizer createAuthorizer() {
				return (Authorizer<UserInfo>) (principal, role) -> principal.getRoles().contains(role);
			}

			@Override
			protected Authenticator createAuthenticator(KeycloakConfiguration configuration) {
				class KeycloakAuthenticator extends AbstractKeycloakAuthenticator<UserInfo> {

					private KeycloakAuthenticator(KeycloakConfiguration keycloakConfiguration) {
						super(keycloakConfiguration);
					}

					@Override
					protected UserInfo prepareAuthentication(KeycloakSecurityContext keycloakSecurityContext,
															 HttpServletRequest httpServletRequest,
															 KeycloakConfiguration keycloakConfiguration) {
						return new UserInfo(keycloakSecurityContext.getToken().getPreferredUsername(),
								keycloakSecurityContext.getIdTokenString());
					}
				}
				return new KeycloakAuthenticator(configuration);
			}
		});
	}

	@Override
	public void run(ProvisioningServiceApplicationConfiguration configuration, Environment environment) {
		DlabProcess.getInstance().setProcessTimeout(configuration.getProcessTimeout());
		DlabProcess.getInstance().setMaxProcessesPerBox(configuration.getProcessMaxThreadsPerJvm());
		DlabProcess.getInstance().setMaxProcessesPerUser(configuration.getProcessMaxThreadsPerUser());

		CloudModule cloudModule = CloudModuleConfigurator.getCloudModule(configuration);
		Injector injector = Guice.createInjector(ModuleFactory.getModule(configuration, environment), cloudModule);
		cloudModule.init(environment, injector);


		final ObjectMapper mapper = injector.getInstance(ObjectMapper.class);
		final InjectableValues.Std injectableValues = new InjectableValues.Std();
		injectableValues.addValue(RESTService.class, injector.getInstance(RESTService.class));
		injectableValues.addValue(ComputationalConfigure.class, injector.getInstance(ComputationalConfigure.class));
		mapper.setInjectableValues(injectableValues);

		environment.lifecycle().manage(injector.getInstance(DirectoriesCreator.class));
		if (configuration.isHandlersPersistenceEnabled()) {
			environment.lifecycle().manage(injector.getInstance(RestoreCallbackHandlerServiceImpl.class));
		}
		environment.lifecycle().manage(injector.getInstance(DockerWarmuper.class));


		JerseyEnvironment jersey = environment.jersey();
		jersey.register(configuration.getCloudProvider());
		jersey.register(new RuntimeExceptionMapper());
		jersey.register(new JsonProcessingExceptionMapper());

		jersey.register(injector.getInstance(DockerResource.class));
		jersey.register(injector.getInstance(GitExploratoryResource.class));
		jersey.register(injector.getInstance(LibraryResource.class));
		jersey.register(injector.getInstance(InfrastructureResource.class));
		jersey.register(injector.getInstance(ImageResource.class));
		jersey.register(injector.getInstance(BackupResource.class));
		jersey.register(injector.getInstance(KeyResource.class));
		jersey.register(injector.getInstance(CallbackHandlerResource.class));
		jersey.register(injector.getInstance(ProjectResource.class));

	}
}
