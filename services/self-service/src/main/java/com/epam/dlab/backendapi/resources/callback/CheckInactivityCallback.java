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
import com.epam.dlab.backendapi.service.InactivityService;
import com.epam.dlab.dto.computational.CheckInactivityStatusDTO;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.model.ResourceType;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/infrastructure/inactivity/callback")
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class CheckInactivityCallback {

	@Inject
	private RequestId requestId;
	@Inject
	private InactivityService inactivityService;

	@POST
	public Response checkInactiveClusterResponse(CheckInactivityStatusDTO dto) {
		requestId.checkAndRemove(dto.getRequestId());
		stopClustersByInactivity(dto);
		stopExploratoryByInactivity(dto);
		return Response.ok().build();
	}

	private void stopClustersByInactivity(CheckInactivityStatusDTO dto) {
		final List<EnvResource> clusters = getResources(dto, ResourceType.COMPUTATIONAL);
		inactivityService.stopClustersByInactivity(clusters.stream().map(EnvResource::getId).collect(Collectors.toList()));
		inactivityService.updateLastActivityForClusters(clusters);
	}

	private void stopExploratoryByInactivity(CheckInactivityStatusDTO dto) {
		final List<EnvResource> exploratories = getResources(dto, ResourceType.EXPLORATORY);
		inactivityService.stopByInactivity(exploratories);
		inactivityService.updateLastActivity(exploratories);
	}

	private List<EnvResource> getResources(CheckInactivityStatusDTO dto, ResourceType resourceType) {
		return dto.getResources().stream()
				.filter(r -> r.getResourceType() == resourceType)
				.collect(Collectors.toList());
	}
}
