/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi;

import com.epam.dlab.client.mongo.MongoServiceFactory;
import com.epam.dlab.client.restclient.RESTServiceFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import static com.epam.dlab.auth.SecurityRestAuthenticator.SECURITY_SERVICE;

public class SelfServiceApplicationConfiguration extends Configuration {
    public static final String MONGO = "mongo";
    public static final String PROVISIONING_SERVICE = "provisioningService";

    @Valid
    @JsonProperty
    private boolean mocked;

    @Valid
    @NotNull
    @JsonProperty(MONGO)
    private MongoServiceFactory mongoFactory = new MongoServiceFactory();

    @Valid
    @NotNull
    @JsonProperty(SECURITY_SERVICE)
    private RESTServiceFactory securityFactory;

    @Valid
    @NotNull
    @JsonProperty(PROVISIONING_SERVICE)
    private RESTServiceFactory provisioningFactory = new RESTServiceFactory();


    public boolean isMocked() {
        return mocked;
    }

    public MongoServiceFactory getMongoFactory() {
        return mongoFactory;
    }

    public RESTServiceFactory getSecurityFactory() {
        return securityFactory;
    }

    public RESTServiceFactory getProvisioningFactory() {
        return provisioningFactory;
    }

}
