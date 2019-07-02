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

package com.epam.dlab.backendapi.resources.callback.gcp;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.callback.base.KeyUploaderCallback;
import com.epam.dlab.dto.base.keyload.UploadFileResult;
import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/user/access_key")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class KeyUploaderCallbackGcp {

	@Inject
	private KeyUploaderCallback keyUploaderCallback;

	@Inject
	private RequestId requestId;

	public KeyUploaderCallbackGcp() {
		log.info("{} is initialized", getClass().getSimpleName());
	}

	/**
	 * Stores the result of the upload the user key.
	 *
	 * @param dto result of the upload the user key.
	 * @return 200 OK
	 */
	@POST
	@Path("/callback")
	public Response loadKeyResponse(UploadFileResult<EdgeInfoGcp> dto) {
		log.debug("Upload the key result and EDGE node info for user {}: {}", dto.getUser(), dto);
		requestId.checkAndRemove(dto.getRequestId());
		keyUploaderCallback.handleCallback(dto.getStatus(), dto.getUser(), dto.getEdgeInfo());

		return Response.ok().build();

	}
}
