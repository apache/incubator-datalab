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

package com.epam.dlab.backendapi.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EnvStatusDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.dto.status.EnvStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.epam.dlab.rest.contracts.InfrasctructureAPI;
import com.google.inject.Inject;

import io.dropwizard.auth.Auth;

/** Provides the REST API for the basic information about infrastructure.
 */
@Path("/infrastructure")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InfrastructureResource implements InfrasctructureAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfrastructureResource.class);

    @Inject
    private EnvStatusDAO envDAO;

    /** Return status of self-service.
     */
    @GET
    public Response status() {
        return Response.status(Response.Status.OK).build();
    }

    /** Returns the status of infrastructure: edge.
     * @param userInfo user info.
     */
    @GET
    @Path(ApiCallbacks.STATUS_URI)
    public HealthStatusPageDTO status(@Auth UserInfo userInfo, @QueryParam("full") @DefaultValue("0") int fullReport) throws DlabException {
    	LOGGER.debug("Request the status of resources for user {}, report type {}", userInfo.getName(), fullReport);
    	try {
    		HealthStatusPageDTO status = envDAO.getHealthStatusPageDTO(userInfo.getName(), fullReport != 0);
    		LOGGER.debug("Return the status of resources for user {}: {}", userInfo.getName(), status);
    		return status;
    	} catch (Throwable e) {
    		LOGGER.warn("Could not return status of resources for user {}: {}", userInfo.getName(), e.getLocalizedMessage(), e);
    		throw new DlabException("Could not return status of resources: " + e.getLocalizedMessage(), e);
    	}
    }
    
    /** Updates the status of the resources for user.
     * @param dto DTO info about the statuses of resources.
     * @return Always return code 200 (OK).
     */
    @POST
    @Path(ApiCallbacks.STATUS_URI)
    public Response status(EnvStatusDTO dto) {
        LOGGER.trace("Updating the status of resources for user {}: {}", dto.getUser(), dto);
        RequestId.checkAndRemove(dto.getRequestId());
        try {
        	if (UserInstanceStatus.FAILED == UserInstanceStatus.of(dto.getStatus())) {
        		LOGGER.warn("Request for the status of resources for user {} fails: {}", dto.getUser(), dto.getErrorMessage());
        	} else {
        		envDAO.updateEnvStatus(dto.getUser(), dto.getResourceList());
        	}
        } catch (Throwable e) {
        	LOGGER.warn("Could not update status of resources for user {}: {}", dto.getUser(), e.getLocalizedMessage(), e);
        }
        // Always necessary send OK for status request
        return Response.ok().build();
    }

}
