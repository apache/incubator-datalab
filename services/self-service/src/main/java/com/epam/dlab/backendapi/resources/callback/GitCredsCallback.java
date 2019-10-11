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

package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/user/git_creds")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class GitCredsCallback {

	@Inject
	private RequestId requestId;

    /**
     * Update GIT credentials status in Mongo DB for user.
     *
     * @param dto description of status.
     * @return 200 OK - if request success.
     */
    @POST
    @Path(ApiCallbacks.STATUS_URI)
    public Response status(ExploratoryStatusDTO dto) {
        if (UserInstanceStatus.CREATED != UserInstanceStatus.of(dto.getStatus())) {
            //TODO Handle error status?
            log.error("Git creds has not been updated for exploratory environment {} for user {}, status is {}",
                    dto.getExploratoryName(), dto.getUser(), dto.getStatus());
        } else {
            log.debug("Git creds has been updated for exploratory environment {} for user {}, status is {}",
                    dto.getExploratoryName(), dto.getUser(), dto.getStatus());
        }
		requestId.checkAndRemove(dto.getRequestId());
        return Response.ok().build();
    }
}
