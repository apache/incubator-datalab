/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.dto.computational.CheckInactivityClusterStatusDTO;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/infrastructure/check_inactivity/clusters/callback")
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class CheckInactiveClusterCallback {

	@Inject
	private RequestId requestId;
	@Inject
	private ComputationalService computationalService;

	@Context
	private UriInfo uriInfo;

	@POST
	public Response checkInactiveClusterResponse(CheckInactivityClusterStatusDTO dto) {
		requestId.checkAndRemove(dto.getRequestId());
		computationalService.stopClustersByCondition(dto);
		computationalService.updateLastActivityForClusters(dto);
		return Response.ok(uriInfo.getRequestUri()).build();
	}
}
