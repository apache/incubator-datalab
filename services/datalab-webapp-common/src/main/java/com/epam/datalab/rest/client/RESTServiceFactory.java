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

package com.epam.datalab.rest.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;

public class RESTServiceFactory {
    @JsonProperty
    private String protocol;

    @JsonProperty
    private String host;

    @JsonProperty
    private int port;

    @Valid
    @NotNull
    @JsonProperty("jerseyClient")
    private JerseyClientConfiguration jerseyClientConfiguration;

    public RESTService build(Environment environment, String name) {
        return build(environment, name, null);
    }

    public RESTService build(Environment environment, String name, String userAgent) {
        Client client = new JerseyClientBuilder(environment).using(jerseyClientConfiguration).build(name).register(MultiPartFeature.class);
        return StringUtils.isNotEmpty(host) ?
                new RESTService(client, getURL(), userAgent) : new RESTService(client, userAgent);
    }

    private String getURL() {
        return String.format("%s://%s:%d", protocol, host, port);
    }
}
