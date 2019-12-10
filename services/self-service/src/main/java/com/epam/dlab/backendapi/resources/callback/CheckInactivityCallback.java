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
import com.epam.dlab.backendapi.service.InactivityService;
import com.epam.dlab.dto.computational.CheckInactivityStatusDTO;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;

import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneId.systemDefault;

@Path("/infrastructure/inactivity/callback")
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class CheckInactivityCallback {

	@Inject
	private RequestId requestId;
	@Inject
	private InactivityService inactivityService;

	@POST
	@Path("exploratory")
	public Response updateExploratoryLastActivity(CheckInactivityStatusDTO dto) {
		requestId.checkAndRemove(dto.getRequestId());
		inactivityService.updateLastActivityForExploratory(new UserInfo(dto.getUser(), null), dto.getExploratoryName(),
				toLocalDateTime(dto.getLastActivityUnixTime()));
		return Response.ok().build();
	}

	@POST
	@Path("computational")
	public Response updateComputationalLastActivity(CheckInactivityStatusDTO dto) {
		requestId.checkAndRemove(dto.getRequestId());
		inactivityService.updateLastActivityForComputational(new UserInfo(dto.getUser(), null),
				dto.getExploratoryName(),
				dto.getComputationalName(), toLocalDateTime(dto.getLastActivityUnixTime()));
		return Response.ok().build();
	}

	private LocalDateTime toLocalDateTime(long unixTime) {
		return ofEpochSecond(unixTime).atZone(systemDefault()).toLocalDateTime();
	}
}
