/***************************************************************************

 Copyright (c) 2016, EPAM SYSTEMS INC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 ****************************************************************************/

package com.epam.dlab.rest.client;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

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

    public <T> T get(String path, Class<T> clazz) {
        Invocation.Builder builder = getBuilder(path);
        log.debug("REST get {}", path);
        return builder.get(clazz);
    }

    public <T> T post(String path, Object parameter, Class<T> clazz) {
        Invocation.Builder builder = getBuilder(path);
        log.debug("REST post {}", path);
        return builder.post(Entity.json(parameter), clazz);
    }

    public <T> T get(String path, String accessToken, Class<T> clazz) {
        Invocation.Builder builder = getBuilder(path, accessToken);
        log.debug("REST get secured {} {}", path, accessToken);
        return builder.get(clazz);
    }

    public <T> T post(String path, String accessToken, Object parameter, Class<T> clazz) {
        Invocation.Builder builder = getBuilder(path, accessToken);
        log.debug("REST post secured {} {}", path, accessToken);
        return builder.post(Entity.json(parameter), clazz);
    }

    private Invocation.Builder getBuilder(String path) {
        return getBuilder(path, null);
    }

    private Invocation.Builder getBuilder(String path, String token) {
        Invocation.Builder builder = getWebTarget(path)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }

        if (userAgent != null) {
            builder.header(HttpHeaders.USER_AGENT, userAgent);
        }

        return builder;
    }

    private WebTarget getWebTarget(String path) {
        return client
                .target(url)
                .path(path);
    }
}
