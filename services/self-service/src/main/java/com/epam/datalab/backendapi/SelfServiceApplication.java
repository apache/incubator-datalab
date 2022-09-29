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

import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.IndexCreator;
import com.epam.datalab.backendapi.domain.ExploratoryLibCache;
import com.epam.datalab.backendapi.dropwizard.bundles.DatalabKeycloakBundle;
import com.epam.datalab.backendapi.dropwizard.listeners.MongoStartupListener;
import com.epam.datalab.backendapi.dropwizard.listeners.RestoreHandlerStartupListener;
import com.epam.datalab.backendapi.healthcheck.MongoHealthCheck;
import com.epam.datalab.backendapi.modules.ModuleFactory;
import com.epam.datalab.backendapi.resources.*;
import com.epam.datalab.backendapi.resources.callback.BackupCallback;
import com.epam.datalab.backendapi.resources.callback.CheckInactivityCallback;
import com.epam.datalab.backendapi.resources.callback.ComputationalCallback;
import com.epam.datalab.backendapi.resources.callback.EnvironmentStatusCallback;
import com.epam.datalab.backendapi.resources.callback.ExploratoryCallback;
import com.epam.datalab.backendapi.resources.callback.GitCredsCallback;
import com.epam.datalab.backendapi.resources.callback.ImageCallback;
import com.epam.datalab.backendapi.resources.callback.LibraryCallback;
import com.epam.datalab.backendapi.resources.callback.OdahuCallback;
import com.epam.datalab.backendapi.resources.callback.ProjectCallback;
import com.epam.datalab.backendapi.resources.callback.ReuploadKeyCallback;
import com.epam.datalab.backendapi.schedulers.internal.ManagedScheduler;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.servlet.guacamole.GuacamoleServlet;
import com.epam.datalab.cloud.CloudModule;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.migration.mongo.DatalabMongoMigration;
import com.epam.datalab.mongo.MongoServiceFactory;
import com.epam.datalab.rest.client.RESTService;
import com.epam.datalab.rest.mappers.DatalabValidationExceptionMapper;
import com.epam.datalab.rest.mappers.JsonProcessingExceptionMapper;
import com.epam.datalab.rest.mappers.ResourceConflictExceptionMapper;
import com.epam.datalab.rest.mappers.ResourceNotFoundExceptionMapper;
import com.epam.datalab.rest.mappers.ResourceQuoteReachedExceptionMapper;
import com.epam.datalab.rest.mappers.RuntimeExceptionMapper;
import com.epam.datalab.rest.mappers.ValidationExceptionMapper;
import com.epam.datalab.util.ServiceUtils;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.BiDiGzipHandler;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerWrapper;

/**
 * Self Service based on Dropwizard application.
 */
@Slf4j
public class SelfServiceApplication extends Application<SelfServiceApplicationConfiguration> {
    public static final String GUACAMOLE_SERVLET_PATH = "/api/tunnel";
    private static Injector appInjector;

    public static Injector getInjector() {
        return appInjector;
    }

    public static void setInjector(Injector injector) {
        SelfServiceApplication.appInjector = injector;
    }

    public static void main(String... args) throws Exception {
        if (ServiceUtils.printAppVersion(SelfServiceApplication.class, args)) {
            return;
        }
        new SelfServiceApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<SelfServiceApplicationConfiguration> bootstrap) {
        super.initialize(bootstrap);
        bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new AssetsBundle("/webapp/dist", "/", "index.html"));
        bootstrap.addBundle(new TemplateConfigBundle(
                new TemplateConfigBundleConfiguration().fileIncludePath(ServiceUtils.getConfPath())
        ));

        bootstrap.addBundle(new DatalabKeycloakBundle());
    }

    @Override
    public void run(SelfServiceApplicationConfiguration configuration, Environment environment) {

        CloudModule cloudModule = ModuleFactory.getCloudProviderModule(configuration);
        Injector injector = Guice.createInjector(ModuleFactory.getModule(configuration, environment), cloudModule);
        setInjector(injector);

        cloudModule.init(environment, injector);
        if (configuration.isMongoMigrationEnabled()) {
            environment.lifecycle().addServerLifecycleListener(server -> applyMongoMigration(configuration));
        }
        environment.lifecycle().addServerLifecycleListener(injector.getInstance(MongoStartupListener.class));
        final RestoreHandlerStartupListener restoreHandlerStartupListener =
                new RestoreHandlerStartupListener(injector.getInstance(Key.get(RESTService.class,
                        Names.named(ServiceConsts.PROVISIONING_SERVICE_NAME))), injector.getInstance(EndpointService.class));
        environment.lifecycle().addServerLifecycleListener(restoreHandlerStartupListener);
        environment.lifecycle().addServerLifecycleListener(this::disableGzipHandlerForGuacamoleServlet);
        environment.lifecycle().manage(injector.getInstance(IndexCreator.class));
        environment.lifecycle().manage(injector.getInstance(ExploratoryLibCache.class));
        environment.lifecycle().manage(injector.getInstance(ManagedScheduler.class));
        environment.healthChecks().register(ServiceConsts.MONGO_NAME, injector.getInstance(MongoHealthCheck.class));

        environment.servlets().addServlet("GuacamoleServlet", injector.getInstance(GuacamoleServlet.class))
                .addMapping(GUACAMOLE_SERVLET_PATH);


        JerseyEnvironment jersey = environment.jersey();

        jersey.register(new RuntimeExceptionMapper());
        jersey.register(new JsonProcessingExceptionMapper());
        jersey.register(new ResourceConflictExceptionMapper());
        jersey.register(new ResourceNotFoundExceptionMapper());
        jersey.register(new DatalabValidationExceptionMapper());
        jersey.register(new ValidationExceptionMapper());
        jersey.register(new ResourceQuoteReachedExceptionMapper());

        jersey.register(injector.getInstance(InfrastructureTemplateResource.class));
        jersey.register(injector.getInstance(InfrastructureInfoResource.class));

        jersey.register(injector.getInstance(EnvironmentStatusCallback.class));

        jersey.register(injector.getInstance(ComputationalCallback.class));

        jersey.register(injector.getInstance(UserSettingsResource.class));

        jersey.register(injector.getInstance(ExploratoryResource.class));
        jersey.register(injector.getInstance(ExploratoryCallback.class));

        jersey.register(injector.getInstance(LibExploratoryResource.class));
        jersey.register(injector.getInstance(LibraryCallback.class));

        jersey.register(injector.getInstance(GitCredsResource.class));
        jersey.register(injector.getInstance(GitCredsCallback.class));
        jersey.register(injector.getInstance(SchedulerJobResource.class));
        jersey.register(injector.getInstance(ImageExploratoryResource.class));
        jersey.register(injector.getInstance(ImageCallback.class));
        jersey.register(injector.getInstance(BackupResource.class));
        jersey.register(injector.getInstance(BackupCallback.class));
        jersey.register(injector.getInstance(EnvironmentResource.class));
        jersey.register(injector.getInstance(ReuploadKeyCallback.class));
	    jersey.register(injector.getInstance(CheckInactivityCallback.class));
	    jersey.register(injector.getInstance(SystemInfoResource.class));
	    jersey.register(injector.getInstance(UserGroupResource.class));
	    jersey.register(injector.getInstance(UserRoleResource.class));
	    jersey.register(injector.getInstance(ApplicationSettingResource.class));
	    jersey.register(injector.getInstance(KeycloakResource.class));
	    jersey.register(injector.getInstance(EndpointResource.class));
	    jersey.register(injector.getInstance(ProjectResource.class));
	    jersey.register(injector.getInstance(AuditResource.class));
	    jersey.register(injector.getInstance(ProjectCallback.class));
	    jersey.register(injector.getInstance(OdahuResource.class));
	    jersey.register(injector.getInstance(OdahuCallback.class));
	    jersey.register(injector.getInstance(ChangePropertiesResource.class));
	    jersey.register(injector.getInstance(ConnectedPlatformResource.class));
    }

    private void disableGzipHandlerForGuacamoleServlet(Server server) {
        Handler handler = server.getHandler();
        while (handler instanceof HandlerWrapper) {
            handler = ((HandlerWrapper) handler).getHandler();
            if (handler instanceof BiDiGzipHandler) {
                log.debug("Disabling Gzip handler for guacamole servlet");
                ((BiDiGzipHandler) handler).setExcludedPaths(GUACAMOLE_SERVLET_PATH);
            }
        }
    }

    private void applyMongoMigration(SelfServiceApplicationConfiguration configuration) {
        final MongoServiceFactory mongoFactory = configuration.getMongoFactory();

        new DatalabMongoMigration(mongoFactory.getHost(), mongoFactory.getPort(), mongoFactory.getUsername(),
                mongoFactory.getPassword(), mongoFactory.getDatabase()).migrate();
    }

}
