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
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.edge.EdgeInfo;
import com.epam.datalab.dto.base.project.ProjectResult;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class ProjectCallbackHandler extends ResourceCallbackHandler<ProjectResult> {


    private final String callbackUri;
    private final String projectName;
    private final Class<? extends EdgeInfo> clazz;
    private final String endpointName;

    public ProjectCallbackHandler(RESTService selfService, String user,
                                  String uuid, DockerAction action, String callbackUri, String projectName,
                                  Class<? extends EdgeInfo> clazz, String endpointName) {
        super(selfService, user, uuid, action);
        this.callbackUri = callbackUri;
        this.projectName = projectName;
        this.clazz = clazz;
        this.endpointName = endpointName;
    }

    @Override
    protected String getCallbackURI() {
        return callbackUri;
    }

    @Override
    protected ProjectResult parseOutResponse(JsonNode resultNode, ProjectResult baseStatus) {
        baseStatus.setProjectName(projectName);
        baseStatus.setEndpointName(endpointName);
        if (resultNode != null &&
                Arrays.asList(DockerAction.CREATE, DockerAction.RECREATE).contains(getAction()) &&
                UserInstanceStatus.of(baseStatus.getStatus()) != UserInstanceStatus.FAILED) {
            try {
                final EdgeInfo projectEdgeInfo = mapper.readValue(resultNode.toString(), clazz);
                baseStatus.setEdgeInfo(projectEdgeInfo);
            } catch (IOException e) {
                throw new DatalabException("Cannot parse the EDGE info in JSON: " + e.getLocalizedMessage(), e);
            }
        }
        return baseStatus;
    }
}
