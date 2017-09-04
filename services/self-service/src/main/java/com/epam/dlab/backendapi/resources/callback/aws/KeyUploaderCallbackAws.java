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
import com.epam.dlab.backendapi.resources.callback.KeyUploaderCallback;
import com.epam.dlab.dto.aws.keyload.UploadFileResultAws;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/user/access_key")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class KeyUploaderCallbackAws {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Inject
    private KeyUploaderCallback keyUploaderCallback;

    public KeyUploaderCallbackAws() {
        logger.info("{} is initialized", getClass().getSimpleName());
    }

    /**
     * Stores the result of the upload the user key.
     *
     * @param dto result of the upload the user key.
     * @return 200 OK
     */
    @POST
    @Path("/callback")
    public Response loadKeyResponse(UploadFileResultAws dto) throws DlabException {
        logger.debug("Upload the key result and EDGE node info for user {}: {}", dto.getUser(), dto);
        RequestId.checkAndRemove(dto.getRequestId());
        keyUploaderCallback.handleCallback(dto.getStatus(), dto.getUser(), dto.retrieveEdgeInfo());

        return Response.ok().build();

    }
}
