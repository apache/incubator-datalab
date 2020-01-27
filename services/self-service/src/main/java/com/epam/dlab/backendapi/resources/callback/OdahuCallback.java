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

import com.epam.dlab.backendapi.dao.OdahuDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.odahu.OdahuResult;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path("odahu/status")
@Consumes(MediaType.APPLICATION_JSON)
public class OdahuCallback {

    private final OdahuDAO odahuDAO;
    private final RequestId requestId;

    @Inject
    public OdahuCallback(OdahuDAO odahuDAO, RequestId requestId) {
        this.odahuDAO = odahuDAO;
        this.requestId = requestId;
    }

    @POST
    public Response updateOdahuStatus(OdahuResult odahuResult) {
        requestId.checkAndRemove(odahuResult.getRequestId());
        final UserInstanceStatus status = UserInstanceStatus.of(odahuResult.getStatus());
        Optional.ofNullable(status)
                .orElseThrow(() -> new DlabException(String.format("Cannot convert %s to UserInstanceStatus", status)));

        odahuDAO.updateStatus(odahuResult.getName(), odahuResult.getProjectName(), odahuResult.getEndpointName(),
                odahuResult.getResourceUrls(), status);

        return Response.ok().build();
    }
}
