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

package com.epam.datalab;

import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.mongo.MongoServiceFactory;
import com.epam.datalab.rest.client.RESTServiceFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ServiceConfiguration extends Configuration {

    @JsonProperty
    private long inactiveUserTimeoutMillSec = 2 * 60L * 60L * 1000L;

    @NotNull
    @JsonProperty
    private CloudProvider cloudProvider;

    @Valid
    @JsonProperty
    private boolean devMode = false;

    @Valid
    @NotNull
    @JsonProperty(ServiceConsts.MONGO_NAME)
    private MongoServiceFactory mongoFactory = new MongoServiceFactory();

    @Valid
    @NotNull
    @JsonProperty(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTServiceFactory provisioningFactory = new RESTServiceFactory();

    @Valid
    @NotNull
    @JsonProperty(ServiceConsts.BUCKET_SERVICE_NAME)
    private RESTServiceFactory bucketFactory = new RESTServiceFactory();

    @Valid
    @NotNull
    @JsonProperty(ServiceConsts.BILLING_SERVICE_NAME)
    private RESTServiceFactory billingFactory = new RESTServiceFactory();

    @Valid
    @NotNull
    @JsonProperty(ServiceConsts.SECURITY_SERVICE_NAME)
    private RESTServiceFactory securityFactory;

    @Valid
    @NotNull
    @JsonProperty(ServiceConsts.SELF_SERVICE_NAME)
    private RESTServiceFactory selfFactory = new RESTServiceFactory();

    public CloudProvider getCloudProvider() {
        return cloudProvider;
    }

    public long getInactiveUserTimeoutMillSec() {
        return inactiveUserTimeoutMillSec;
    }

    /**
     * Returns <b>true</b> if service is a mock.
     */
    public boolean isDevMode() {
        return devMode;
    }

    public MongoServiceFactory getMongoFactory() {
        return mongoFactory;
    }

    public RESTServiceFactory getProvisioningFactory() {
        return provisioningFactory;
    }

    public RESTServiceFactory getBucketFactory() {
        return bucketFactory;
    }

    public RESTServiceFactory getBillingFactory() {
        return billingFactory;
    }

    public RESTServiceFactory getSecurityFactory() {
        return securityFactory;
    }

    public RESTServiceFactory getSelfFactory() {
        return selfFactory;
    }

}
