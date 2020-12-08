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

package com.epam.datalab.backendapi.core.response.handlers;

import com.epam.datalab.backendapi.core.commands.DockerAction;
import com.epam.datalab.dto.ResourceURL;
import com.epam.datalab.dto.base.odahu.OdahuResult;
import com.epam.datalab.rest.client.RESTService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
public class OdahuCallbackHandler extends ResourceCallbackHandler<OdahuResult> {

    private static final String ODAHU_URLS_FIELD = "odahu_urls";
    private static final String GRAFANA_ADMIN = "grafana_admin";
    private static final String GRAFANA_PASSWORD = "grafana_pass";
    private static final String OAUTH_COOKIE_SECRET = "oauth_cookie_secret";
    private static final String DECRYPT_TOKEN = "odahuflow_connection_decrypt_token";
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
        result.setGrafanaAdmin(getTextValue(resultNode.get(GRAFANA_ADMIN)));
        result.setGrafanaPassword(getTextValue(resultNode.get(GRAFANA_PASSWORD)));
        result.setOauthCookieSecret(getTextValue(resultNode.get(OAUTH_COOKIE_SECRET)));
        result.setDecryptToken(getTextValue(resultNode.get(DECRYPT_TOKEN)));

        final JsonNode odahuUrls = resultNode.get(ODAHU_URLS_FIELD);
        List<ResourceURL> urls = null;
        if (odahuUrls != null) {
            try {
                urls = mapper.readValue(odahuUrls.toString(), new TypeReference<List<ResourceURL>>() {
                });
                result.setResourceUrls(urls);
            } catch (IOException e) {
                log.warn("Cannot parse field {} for UUID {} in JSON",
                        RESPONSE_NODE + "." + RESULT_NODE + "." + ODAHU_URLS_FIELD, getUUID(), e);
            }
        }

        if (getAction() == DockerAction.CREATE && Objects.isNull(urls)) {
            log.warn("There are no odahu urls in response file while creating {}", result);
        }

        return result;
    }
}
