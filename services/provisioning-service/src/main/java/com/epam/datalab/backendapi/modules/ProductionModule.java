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
import com.epam.datalab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.datalab.backendapi.core.DockerWarmuper;
import com.epam.datalab.backendapi.core.MetadataHolder;
import com.epam.datalab.backendapi.core.commands.CommandExecutor;
import com.epam.datalab.backendapi.core.commands.ICommandExecutor;
import com.epam.datalab.backendapi.core.response.handlers.dao.CallbackHandlerDao;
import com.epam.datalab.backendapi.core.response.handlers.dao.FileSystemCallbackHandlerDao;
import com.epam.datalab.backendapi.service.BucketService;
import com.epam.datalab.backendapi.service.CheckInactivityService;
import com.epam.datalab.backendapi.service.OdahuService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.service.RestoreCallbackHandlerService;
import com.epam.datalab.backendapi.service.impl.CheckInactivityServiceImpl;
import com.epam.datalab.backendapi.service.impl.OdahuServiceImpl;
import com.epam.datalab.backendapi.service.impl.ProjectServiceImpl;
import com.epam.datalab.backendapi.service.impl.RestoreCallbackHandlerServiceImpl;
import com.epam.datalab.backendapi.service.impl.aws.BucketServiceAwsImpl;
import com.epam.datalab.backendapi.service.impl.azure.BucketServiceAzureImpl;
import com.epam.datalab.backendapi.service.impl.gcp.BucketServiceGcpImpl;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.name.Names;
import io.dropwizard.setup.Environment;

/**
 * Production class for an application configuration of SelfService.
 */
public class ProductionModule extends ModuleBase<ProvisioningServiceApplicationConfiguration> {

    /**
     * Instantiates an application configuration of SelfService for production environment.
     *
     * @param configuration application configuration of SelfService.
     * @param environment   environment of SelfService.
     */
    ProductionModule(ProvisioningServiceApplicationConfiguration configuration, Environment environment) {
        super(configuration, environment);
    }

    @Override
    protected void configure() {
        bind(ProvisioningServiceApplicationConfiguration.class).toInstance(configuration);

        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.SECURITY_SERVICE_NAME))
                .toInstance(configuration.getSecurityFactory()
                        .build(environment, ServiceConsts.SECURITY_SERVICE_NAME, ServiceConsts
                                .PROVISIONING_USER_AGENT));

        bind(RESTService.class).toInstance(configuration.getSelfFactory().build(environment, ServiceConsts
                .SELF_SERVICE_NAME));
        bind(MetadataHolder.class).to(DockerWarmuper.class);
        bind(ICommandExecutor.class).to(CommandExecutor.class).asEagerSingleton();
        bind(ObjectMapper.class).toInstance(new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true));
        bind(CallbackHandlerDao.class).to(FileSystemCallbackHandlerDao.class);
        bind(RestoreCallbackHandlerService.class).to(RestoreCallbackHandlerServiceImpl.class);
        bind(CheckInactivityService.class).to(CheckInactivityServiceImpl.class);
        bind(ProjectService.class).to(ProjectServiceImpl.class);
        bind(OdahuService.class).to(OdahuServiceImpl.class);
        if (configuration.getCloudProvider() == CloudProvider.GCP) {
            bind(BucketService.class).to(BucketServiceGcpImpl.class);
        } else if (configuration.getCloudProvider() == CloudProvider.AWS) {
            bind(BucketService.class).to(BucketServiceAwsImpl.class);
        } else if (configuration.getCloudProvider() == CloudProvider.AZURE) {
            bind(BucketService.class).to(BucketServiceAzureImpl.class);
        }
    }
}
