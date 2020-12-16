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


package com.epam.datalab.backendapi;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.core.DirectoriesCreator;
import com.epam.datalab.backendapi.core.DockerWarmuper;
import com.epam.datalab.backendapi.core.response.handlers.ComputationalConfigure;
import com.epam.datalab.backendapi.modules.CloudModuleConfigurator;
import com.epam.datalab.backendapi.modules.ModuleFactory;
import com.epam.datalab.backendapi.resources.BackupResource;
import com.epam.datalab.backendapi.resources.BucketResource;
import com.epam.datalab.backendapi.resources.CallbackHandlerResource;
import com.epam.datalab.backendapi.resources.DockerResource;
import com.epam.datalab.backendapi.resources.GitExploratoryResource;
import com.epam.datalab.backendapi.resources.ImageResource;
import com.epam.datalab.backendapi.resources.InfrastructureResource;
import com.epam.datalab.backendapi.resources.LibraryResource;
import com.epam.datalab.backendapi.resources.OdahuResource;
import com.epam.datalab.backendapi.resources.ProjectResource;
import com.epam.datalab.backendapi.resources.ProvisioningHealthCheckResource;
import com.epam.datalab.backendapi.resources.base.KeyResource;
import com.epam.datalab.backendapi.service.impl.RestoreCallbackHandlerServiceImpl;
import com.epam.datalab.cloud.CloudModule;
import com.epam.datalab.process.model.DatalabProcess;
import com.epam.datalab.rest.client.RESTService;
import com.epam.datalab.rest.mappers.JsonProcessingExceptionMapper;
import com.epam.datalab.rest.mappers.RuntimeExceptionMapper;
import com.epam.datalab.util.ServiceUtils;
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
import io.dropwizard.forms.MultiPartBundle;
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
        bootstrap.addBundle(new MultiPartBundle());
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
        DatalabProcess.getInstance().setProcessTimeout(configuration.getProcessTimeout());
        DatalabProcess.getInstance().setMaxProcessesPerBox(configuration.getProcessMaxThreadsPerJvm());
        DatalabProcess.getInstance().setMaxProcessesPerUser(configuration.getProcessMaxThreadsPerUser());

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
        jersey.register(injector.getInstance(OdahuResource.class));
        jersey.register(injector.getInstance(ProvisioningHealthCheckResource.class));
        environment.jersey().register(injector.getInstance(BucketResource.class));
    }
}
