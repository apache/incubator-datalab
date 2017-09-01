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

package com.epam.dlab.backendapi.resources.callback.aws;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.callback.EdgeCallback;
import com.epam.dlab.dto.aws.keyload.UploadFileResultAws;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(ApiCallbacks.EDGE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EdgeCallbackAws extends EdgeCallback {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EdgeCallbackAws() {
        logger.info("{} is initialized", getClass().getSimpleName());
    }

    /**
     * Stores the result of the upload the user key.
     *
     * @param dto result of the upload the user key.
     * @return 200 OK
     */
    @POST
    @Path(ApiCallbacks.STATUS_URI)
    public Response status(UploadFileResultAws dto) throws DlabException {
        RequestId.checkAndRemove(dto.getRequestId());
        handleEdgeCallback(dto.getUser(), dto.getStatus());
        return Response.ok().build();
    }
}
