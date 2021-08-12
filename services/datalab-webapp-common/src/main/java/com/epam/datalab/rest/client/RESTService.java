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

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.Boundary;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA_TYPE;

@Slf4j
public class RESTService {
    private Client client;
    private String url;
    private String userAgent;

    public RESTService() {
    }

    RESTService(Client client, String url, String userAgent) {
        this.client = client;
        this.url = url;
        this.userAgent = userAgent;
    }

    RESTService(Client client, String userAgent) {
        this.client = client;
        this.userAgent = userAgent;
    }

    public <T> T get(String path, Class<T> clazz) {
        return get(path, null, clazz);
    }

    public <T> T get(URI path, Class<T> clazz) {
        log.debug("REST get {}", path);
        return client.target(URI.create(url + path.toString()))
                .request()
                .get(clazz);
    }

    public <T> T getWithMediaTypes(String path, String accessToken, Class<T> clazz, String requestMediaType, String acceptMediaType) {
        return get(path, accessToken, clazz, requestMediaType, acceptMediaType);
    }

    public <T> T get(String path, String accessToken, Class<T> clazz) {
        return get(path, accessToken, clazz, APPLICATION_JSON, APPLICATION_JSON);
    }

    private <T> T get(String path, String accessToken, Class<T> clazz, String requestMediaType, String acceptMediaType) {
        Invocation.Builder builder = getBuilder(path, accessToken, Collections.emptyMap(), requestMediaType, acceptMediaType);
        log.debug("REST get secured {} {}", path, accessToken);
        return builder.get(clazz);
    }

    public <T> T get(String path, GenericType<T> genericType) {
        return get(path, null, genericType);
    }

    public <T> T get(String path, String accessToken, GenericType<T> genericType) {
        return get(path, accessToken, genericType, Collections.emptyMap());
    }

    public <T> T get(String path, String accessToken, GenericType<T> genericType, Map<String, Object> queryParams) {
        Invocation.Builder builder = getBuilder(path, accessToken, queryParams, APPLICATION_JSON, APPLICATION_JSON);
        log.debug("REST get secured {} {}", path, accessToken);
        return builder.get(genericType);
    }

    public <T> T post(String path, Object parameter, Class<T> clazz) {
        return post(path, null, parameter, clazz);
    }

    public <T> T post(String path, String accessToken, Object parameter, Class<T> clazz) {
        return post(path, accessToken, parameter, clazz, Collections.emptyMap(), APPLICATION_JSON, APPLICATION_JSON);
    }

    public <T> T delete(String path, String accessToken, Class<T> clazz, String requestMediaType, String acceptMediaType) {
        return delete(path, accessToken, clazz, Collections.emptyMap(), requestMediaType, acceptMediaType);
    }

    private <T> T delete(String path, String accessToken, Class<T> clazz, Map<String, Object> queryParams,
                         String requestMediaType, String acceptMediaType) {
        Invocation.Builder builder = getBuilder(path, accessToken, queryParams, requestMediaType, acceptMediaType);
        log.debug("REST delete secured {} {}", path, accessToken);
        return builder.delete(clazz);
    }

    private <T> T post(String path, String accessToken, Object parameter, Class<T> clazz, Map<String, Object> queryParams,
                       String requestMediaType, String acceptMediaType) {
        Invocation.Builder builder = getBuilder(path, accessToken, queryParams, requestMediaType, acceptMediaType);
        log.debug("REST post secured {} {}", path, accessToken);
        return builder.post(Entity.json(parameter), clazz);
    }


    private Invocation.Builder getBuilder(String path, String token, Map<String, Object> queryParams,
                                          String requestMediaType, String acceptMediaType) {
        WebTarget webTarget = getWebTarget(path);
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            webTarget = webTarget.queryParam(entry.getKey(), entry.getValue());
        }

        Invocation.Builder builder = webTarget
                .request(requestMediaType)
                .accept(acceptMediaType);

        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (userAgent != null) {
            builder.header(HttpHeaders.USER_AGENT, userAgent);
        }

        return builder;
    }

    public <T> T postForm(String path, String token, FormDataMultiPart form, Class<T> clazz) {
        WebTarget webTarget = getWebTarget(path);
        Invocation.Builder builder = webTarget.request();
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (userAgent != null) {
            builder.header(HttpHeaders.USER_AGENT, userAgent);
        }

        MediaType mediaType = Boundary.addBoundary(MULTIPART_FORM_DATA_TYPE);
        return builder.post(Entity.entity(form, mediaType), clazz);
    }


    private WebTarget getWebTarget(String path) {
        return url != null ? client.target(url).path(path) : client.target(path);
    }
}
