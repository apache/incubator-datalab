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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class RESTService {
    private Client client;
    private String url;

    public RESTService() {
    }

    RESTService(Client client, String url) {
        this.client = client;
        this.url = url;
    }

    public <T> T get(String path, Class<T> clazz) {
        return getBuilder(path).get(clazz);
    }

    public <T> T post(String path, Object parameter, Class<T> clazz) {
        return getBuilder(path).post(Entity.json(parameter), clazz);
    }

    public Invocation.Builder getBuilder(String path) {
        return getWebTarget(path)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    public WebTarget getWebTarget(String path) {
        return client.target(url).path(path);
    }
}
