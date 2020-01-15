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

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.dao.EndpointDAO;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.List;

@Slf4j
public class EndpointServiceImpl implements EndpointService {
    private static final String URL = "url";
    private final EndpointDAO endpointDAO;

    @Inject
    public EndpointServiceImpl(EndpointDAO endpointDAO) {
        this.endpointDAO = endpointDAO;
    }

    @Override
    public String getEndpointUrl(String name) {
        return (String) getOrError(name).get(URL);
    }

    @Override
    public void create(String name, String url) {
        List<Document> all = endpointDAO.findAll();
        if (all.isEmpty()) {
            endpointDAO.create(name, url);
            log.debug("Successfully created connection with selfservice url: {}", url);
        } else {
            log.error("Endpoint is already connected, excising {}, url {}", all, url);
            throw new DlabException("Endpoint is already connected");
        }
    }

    private Document getOrError(String name) {
        return endpointDAO.findOne(name).orElseThrow(() ->
                new DlabException("Cannot find selfservice endpoint url with name " + name));
    }
}
