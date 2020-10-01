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

package com.epam.datalab.backendapi.modules;

import com.epam.datalab.ModuleBase;
import com.epam.datalab.backendapi.auth.SelfServiceSecurityAuthorizer;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.AuditDAO;
import com.epam.datalab.backendapi.dao.AuditDAOImpl;
import com.epam.datalab.backendapi.dao.BackupDAO;
import com.epam.datalab.backendapi.dao.BackupDAOImpl;
import com.epam.datalab.backendapi.dao.BaseBillingDAO;
import com.epam.datalab.backendapi.dao.BillingDAO;
import com.epam.datalab.backendapi.dao.EndpointDAO;
import com.epam.datalab.backendapi.dao.EndpointDAOImpl;
import com.epam.datalab.backendapi.dao.ImageExploratoryDAO;
import com.epam.datalab.backendapi.dao.ImageExploratoryDAOImpl;
import com.epam.datalab.backendapi.dao.ProjectDAO;
import com.epam.datalab.backendapi.dao.ProjectDAOImpl;
import com.epam.datalab.backendapi.dao.UserGroupDAO;
import com.epam.datalab.backendapi.dao.UserGroupDAOImpl;
import com.epam.datalab.backendapi.dao.UserRoleDAO;
import com.epam.datalab.backendapi.dao.UserRoleDAOImpl;
import com.epam.datalab.backendapi.service.AccessKeyService;
import com.epam.datalab.backendapi.service.ApplicationSettingService;
import com.epam.datalab.backendapi.service.ApplicationSettingServiceImpl;
import com.epam.datalab.backendapi.service.AuditService;
import com.epam.datalab.backendapi.service.BackupService;
import com.epam.datalab.backendapi.service.BucketService;
import com.epam.datalab.backendapi.service.ComputationalService;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.EnvironmentService;
import com.epam.datalab.backendapi.service.ExploratoryService;
import com.epam.datalab.backendapi.service.ExternalLibraryService;
import com.epam.datalab.backendapi.service.GitCredentialService;
import com.epam.datalab.backendapi.service.GuacamoleService;
import com.epam.datalab.backendapi.service.ImageExploratoryService;
import com.epam.datalab.backendapi.service.InactivityService;
import com.epam.datalab.backendapi.service.KeycloakService;
import com.epam.datalab.backendapi.service.KeycloakServiceImpl;
import com.epam.datalab.backendapi.service.LibraryService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.service.ReuploadKeyService;
import com.epam.datalab.backendapi.service.SchedulerJobService;
import com.epam.datalab.backendapi.service.SecurityService;
import com.epam.datalab.backendapi.service.SecurityServiceImpl;
import com.epam.datalab.backendapi.service.SystemInfoService;
import com.epam.datalab.backendapi.service.TagService;
import com.epam.datalab.backendapi.service.TagServiceImpl;
import com.epam.datalab.backendapi.service.UserGroupService;
import com.epam.datalab.backendapi.service.UserRoleService;
import com.epam.datalab.backendapi.service.UserRoleServiceImpl;
import com.epam.datalab.backendapi.service.UserSettingService;
import com.epam.datalab.backendapi.service.UserSettingServiceImpl;
import com.epam.datalab.backendapi.service.impl.AccessKeyServiceImpl;
import com.epam.datalab.backendapi.service.impl.AuditServiceImpl;
import com.epam.datalab.backendapi.service.impl.BackupServiceImpl;
import com.epam.datalab.backendapi.service.impl.BucketServiceImpl;
import com.epam.datalab.backendapi.service.impl.ComputationalServiceImpl;
import com.epam.datalab.backendapi.service.impl.EndpointServiceImpl;
import com.epam.datalab.backendapi.service.impl.EnvironmentServiceImpl;
import com.epam.datalab.backendapi.service.impl.ExploratoryServiceImpl;
import com.epam.datalab.backendapi.service.impl.GitCredentialServiceImpl;
import com.epam.datalab.backendapi.service.impl.GuacamoleServiceImpl;
import com.epam.datalab.backendapi.service.impl.ImageExploratoryServiceImpl;
import com.epam.datalab.backendapi.service.impl.InactivityServiceImpl;
import com.epam.datalab.backendapi.service.impl.LibraryServiceImpl;
import com.epam.datalab.backendapi.service.impl.MavenCentralLibraryService;
import com.epam.datalab.backendapi.service.impl.ProjectServiceImpl;
import com.epam.datalab.backendapi.service.impl.ReuploadKeyServiceImpl;
import com.epam.datalab.backendapi.service.impl.SchedulerJobServiceImpl;
import com.epam.datalab.backendapi.service.impl.SystemInfoServiceImpl;
import com.epam.datalab.backendapi.service.impl.UserGroupServiceImpl;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.mongo.MongoService;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.name.Names;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.logging.LoggingFeature;

import javax.ws.rs.client.Client;

/**
 * Production class for an application configuration of SelfService.
 */
public class ProductionModule extends ModuleBase<SelfServiceApplicationConfiguration> {

    /**
     * Instantiates an application configuration of SelfService for production environment.
     *
     * @param configuration application configuration of SelfService.
     * @param environment   environment of SelfService.
     */
    public ProductionModule(SelfServiceApplicationConfiguration configuration, Environment environment) {
        super(configuration, environment);
    }

    @Override
    protected void configure() {
        final Client httpClient =
                new JerseyClientBuilder(environment)
                        .using(configuration.getJerseyClientConfiguration())
                        .build("httpClient")
                        .register(new LoggingFeature());
        bind(SelfServiceApplicationConfiguration.class).toInstance(configuration);
        bind(MongoService.class).toInstance(configuration.getMongoFactory().build(environment));
        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.SECURITY_SERVICE_NAME))
                .toInstance(configuration.getSecurityFactory().build(environment, ServiceConsts
                        .SECURITY_SERVICE_NAME));
        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.PROVISIONING_SERVICE_NAME))
                .toInstance(configuration.getProvisioningFactory().build(environment, ServiceConsts
                        .PROVISIONING_SERVICE_NAME));
        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.BUCKET_SERVICE_NAME))
                .toInstance(configuration.getBucketFactory().build(environment, ServiceConsts
                        .BUCKET_SERVICE_NAME));
        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.BILLING_SERVICE_NAME))
                .toInstance(configuration.getBillingFactory()
                        .build(environment, ServiceConsts.BILLING_SERVICE_NAME));
        bind(ImageExploratoryService.class).to(ImageExploratoryServiceImpl.class);
        bind(ImageExploratoryDAO.class).to(ImageExploratoryDAOImpl.class);
        bind(BackupService.class).to(BackupServiceImpl.class);
        bind(BackupDAO.class).to(BackupDAOImpl.class);
        bind(ExploratoryService.class).to(ExploratoryServiceImpl.class);
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
        bind(InactivityService.class).to(InactivityServiceImpl.class);
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
        bind(TagService.class).to(TagServiceImpl.class);
        bind(SecurityService.class).to(SecurityServiceImpl.class);
        bind(KeycloakService.class).to(KeycloakServiceImpl.class);
        bind(Client.class).toInstance(httpClient);
    }
}
