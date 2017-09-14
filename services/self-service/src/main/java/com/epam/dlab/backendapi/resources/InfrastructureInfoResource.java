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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EnvStatusDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.dto.aws.edge.EdgeInfoAws;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.epam.dlab.rest.contracts.InfrasctructureAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides the REST API for the basic information about infrastructure.
 */
@Path("/infrastructure")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class InfrastructureInfoResource implements InfrasctructureAPI {

    private static final String EDGE_IP = "edge_node_ip";

    @Inject
    private ExploratoryDAO expDAO;
    @Inject
    private KeyDAO keyDAO;
    @Inject
    private EnvStatusDAO envDAO;

    /**
     * Return status of self-service.
     */
    @GET
    public Response status() {
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Returns the status of infrastructure: edge.
     *
     * @param userInfo user info.
     */
    @GET
    @Path(ApiCallbacks.STATUS_URI)
    public HealthStatusPageDTO status(@Auth UserInfo userInfo, @QueryParam("full") @DefaultValue("0") int fullReport) throws DlabException {
        log.debug("Request the status of resources for user {}, report type {}", userInfo.getName(), fullReport);
        try {
            HealthStatusPageDTO status = envDAO.getHealthStatusPageDTO(userInfo.getName(), fullReport != 0);
            log.debug("Return the status of resources for user {}: {}", userInfo.getName(), status);
            return status;
        } catch (DlabException e) {
            log.warn("Could not return status of resources for user {}: {}", userInfo.getName(), e.getLocalizedMessage(), e);
            throw e;
        }
    }

    /**
     * Returns the list of the provisioned user resources.
     *
     * @param userInfo user info.
     */
    @GET
    @Path("/info")
    public Iterable<Document> getUserResources(@Auth UserInfo userInfo) throws DlabException {
        log.debug("Loading list of provisioned resources for user {}", userInfo.getName());
        try {
            Iterable<Document> documents = expDAO.findExploratory(userInfo.getName());
            EdgeInfoAws edgeInfo = keyDAO.getEdgeInfo(userInfo.getName(), EdgeInfoAws.class, new EdgeInfoAws());

            int i = 0;
            for (Document d : documents) {
                d.append(EDGE_IP, edgeInfo.getPublicIp())
                        .append(EdgeInfoAws.USER_OWN_BUCKET_NAME, edgeInfo.getUserOwnBucketName())
                        .append(EdgeInfoAws.SHARED_BUCKET_NAME, edgeInfo.getSharedBucketName());

                log.trace("Notebook[{}]: {}", ++i, d);
            }
            return documents;
        } catch (DlabException e) {
            log.error("Could not load list of provisioned resources for user: {}", userInfo.getName(), e);
            throw e;
        }
    }
}
