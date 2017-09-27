/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.EdgeAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.epam.dlab.UserInstanceStatus.*;

/**
 * Provides the REST API to manage(start/stop) edge node
 */
@Path("/infrastructure/edge")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class EdgeResource implements EdgeAPI {
    @Inject
    private KeyDAO keyDAO;

    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    /**
     * Starts EDGE node for user.
     *
     * @param userInfo user info.
     * @return Request Id.
     * @throws DlabException
     */
    @POST
    @Path("/start")
    public String start(@Auth UserInfo userInfo) {
        log.debug("Starting EDGE node for user {}", userInfo.getName());
        UserInstanceStatus status = UserInstanceStatus.of(keyDAO.getEdgeStatus(userInfo.getName()));
        if (status == null || !status.in(STOPPED)) {
            log.error("Could not start EDGE node for user {} because the status of instance is {}", userInfo.getName(), status);
            throw new DlabException("Could not start EDGE node because the status of instance is " + status);
        }

        try {
            return action(userInfo, EDGE_START, STARTING);
        } catch (DlabException e) {
            log.error("Could not start EDGE node for user {}", userInfo.getName(), e);
            throw new DlabException("Could not start EDGE node: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Stop EDGE node for user.
     *
     * @param userInfo user info.
     * @return Request Id.
     */
    @POST
    @Path("/stop")
    public String stop(@Auth UserInfo userInfo) {
        log.debug("Stopping EDGE node for user {}", userInfo.getName());
        UserInstanceStatus status = UserInstanceStatus.of(keyDAO.getEdgeStatus(userInfo.getName()));
        if (status == null || !status.in(RUNNING)) {
            log.error("Could not stop EDGE node for user {} because the status of instance is {}", userInfo.getName(), status);
            throw new DlabException("Could not stop EDGE node because the status of instance is " + status);
        }

        try {
            return action(userInfo, EDGE_STOP, STOPPING);
        } catch (DlabException e) {
            log.error("Could not stop EDGE node for user {}", userInfo.getName(), e);
            throw new DlabException("Could not stop EDGE node: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Sends the post request to the provisioning service and update the status of EDGE node.
     *
     * @param userInfo user info.
     * @param action   action for EDGE node.
     * @param status   status of EDGE node.
     * @return Request Id.
     */
    private String action(UserInfo userInfo, String action, UserInstanceStatus status) {
        try {
            keyDAO.updateEdgeStatus(userInfo.getName(), status.toString());
            ResourceSysBaseDTO<?> dto = RequestBuilder.newEdgeAction(userInfo);
            String uuid = provisioningService.post(action, userInfo.getAccessToken(), dto, String.class);
            RequestId.put(userInfo.getName(), uuid);
            return uuid;
        } catch (Throwable t) {
            keyDAO.updateEdgeStatus(userInfo.getName(), FAILED.toString());
            throw new DlabException("Could not " + action + " EDGE node " + ": " + t.getLocalizedMessage(), t);
        }
    }
}