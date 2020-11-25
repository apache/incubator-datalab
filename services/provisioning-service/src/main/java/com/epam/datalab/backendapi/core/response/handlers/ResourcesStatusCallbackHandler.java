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
import com.epam.datalab.dto.status.EnvResource;
import com.epam.datalab.dto.status.EnvResourceList;
import com.epam.datalab.dto.status.EnvStatusDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.epam.datalab.rest.contracts.ApiCallbacks.INFRASTRUCTURE;
import static com.epam.datalab.rest.contracts.ApiCallbacks.STATUS_URI;

@Slf4j
public class ResourcesStatusCallbackHandler extends ResourceCallbackHandler<EnvStatusDTO> {

    private final Map<String, EnvResource> datalabHostResources;

    @JsonCreator
    public ResourcesStatusCallbackHandler(@JacksonInject RESTService selfService, @JsonProperty("action") DockerAction action,
                                          @JsonProperty("uuid") String uuid, @JsonProperty("user") String user, EnvResourceList resourceList) {
        super(selfService, user, uuid, action);
        this.datalabHostResources = getEnvResources(resourceList.getHostList());
    }

    @Override
    protected EnvStatusDTO parseOutResponse(JsonNode resultNode, EnvStatusDTO baseStatus) {
        if (resultNode == null) {
            return baseStatus;
        }

        EnvResourceList cloudResourceList;
        try {
            cloudResourceList = mapper.readValue(resultNode.toString(), EnvResourceList.class);
        } catch (IOException e) {
            throw new DatalabException("Docker response for UUID " + getUUID() + " not valid: " + e.getLocalizedMessage(), e);
        }

        EnvResourceList envResourceList = new EnvResourceList();
        if (CollectionUtils.isNotEmpty(cloudResourceList.getHostList())) {
            envResourceList.withHostList(getChangedEnvResources(cloudResourceList.getHostList()));
        } else {
            envResourceList.withHostList(Collections.emptyList());
        }

        baseStatus
                .withResourceList(envResourceList)
                .withUptime(Date.from(Instant.now()));

        log.trace("Inner status {}", baseStatus);

        return baseStatus;
    }

    @Override
    public boolean handle(String fileName, byte[] content) {
        try {
            return super.handle(fileName, content);
        } catch (Exception e) {
            log.warn("Could not retrieve the status of resources for UUID {} and user {}: {}",
                    getUUID(), getUser(), e.getLocalizedMessage(), e);
        }
        return true; // Always necessary return true for status response
    }

    @Override
    protected String getCallbackURI() {
        return INFRASTRUCTURE + STATUS_URI;
    }

    @Override
    public void handleError(String errorMessage) {
        // Nothing action for status response
    }

    private List<EnvResource> getChangedEnvResources(List<EnvResource> envResources) {
        return envResources
                .stream()
                .filter(e -> !e.getStatus().equals(datalabHostResources.get(e.getId()).getStatus()))
                .map(e -> datalabHostResources.get(e.getId())
                        .withStatus(e.getStatus()))
                .collect(Collectors.toList());
    }

    private Map<String, EnvResource> getEnvResources(List<EnvResource> envResources) {
        return envResources
                .stream()
                .collect(Collectors.toMap(EnvResource::getId, e -> e));
    }
}
