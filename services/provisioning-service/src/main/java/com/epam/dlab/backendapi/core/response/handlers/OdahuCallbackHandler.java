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

package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.ResourceURL;
import com.epam.dlab.dto.base.odahu.OdahuResult;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class OdahuCallbackHandler extends ResourceCallbackHandler<OdahuResult> {

    private static final String ODAHU_URLS_FIELD = "odahu_urls";
    private final String callbackUri;
    private final String name;
    private final String projectName;
    private final String endpointName;

    public OdahuCallbackHandler(RESTService selfService, String user, String uuid, DockerAction action,
                                String callbackUri, String name, String projectName, String endpointName) {
        super(selfService, user, uuid, action);
        this.callbackUri = callbackUri;
        this.name = name;
        this.projectName = projectName;
        this.endpointName = endpointName;
    }

    @Override
    protected String getCallbackURI() {
        return callbackUri;
    }

    @Override
    protected OdahuResult parseOutResponse(JsonNode resultNode, OdahuResult result) {
        result.setName(name);
        result.setProjectName(projectName);
        result.setEndpointName(endpointName);

        if (resultNode == null) {
            return result;
        }
        final JsonNode nodeUrl = resultNode.get(ODAHU_URLS_FIELD);
        List<ResourceURL> urls;
        if (nodeUrl != null) {
            try {
                urls = mapper.readValue(nodeUrl.toString(), new TypeReference<List<ResourceURL>>() {
                });
                result.setResourceUrls(urls);
            } catch (IOException e) {
                log.warn("Cannot parse field {} for UUID {} in JSON",
                        RESPONSE_NODE + "." + RESULT_NODE + "." + ODAHU_URLS_FIELD, getUUID(), e);
            }
        }

        return result;
    }
}
