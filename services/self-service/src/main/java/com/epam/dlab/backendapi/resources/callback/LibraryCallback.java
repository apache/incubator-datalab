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
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.domain.ExploratoryLibCache;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.exploratory.LibInstallStatusDTO;
import com.epam.dlab.dto.exploratory.LibListStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/infrastructure_provision/library")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class LibraryCallback {

	@Inject
	private ExploratoryLibDAO libraryDAO;
	@Inject
	private RequestId requestId;

	/**
	 * Changes the status of installed libraries for exploratory environment.
	 *
	 * @param dto description of status.
	 * @return 200 OK - if request success.
	 */
	@POST
	@Path("/lib_status")
	public Response libInstallStatus(LibInstallStatusDTO dto) {
		log.debug("Updating status of libraries for exploratory environment {} for user {} to {}",
				dto.getExploratoryName(), dto.getUser(), dto);
		requestId.checkAndRemove(dto.getRequestId());
		try {
			libraryDAO.updateLibraryFields(dto);
		} catch (DlabException e) {
			log.error("Cannot update status of libraries for exploratory environment {} for user {} to {}",
					dto.getExploratoryName(), dto.getUser(), dto, e);
			throw new DlabException("Cannot update status of libaries for exploratory environment " + dto.getExploratoryName() +
					" for user " + dto.getUser() + ": " + e.getLocalizedMessage(), e);
		}

		return Response.ok().build();
	}


	/**
	 * Updates the list of libraries.
	 *
	 * @param dto DTO the list of libraries.
	 * @return Always return code 200 (OK).
	 */
	@POST
	@Path("/update_lib_list")
	public Response updateLibList(LibListStatusDTO dto) {
		log.debug("Updating the list of libraries for image {}", dto.getImageName());
		requestId.checkAndRemove(dto.getRequestId());
		try {
			if (UserInstanceStatus.FAILED == UserInstanceStatus.of(dto.getStatus())) {
				log.warn("Request for the list of libraries fails: {}", dto.getErrorMessage());
				ExploratoryLibCache.getCache().removeLibList(dto.getImageName());
			} else {
				ExploratoryLibCache.getCache().updateLibList(dto.getImageName(), dto.getLibs());
			}
		} catch (Exception e) {
			log.warn("Cannot update the list of libs: {}", e.getLocalizedMessage(), e);
		}
		// Always necessary send OK for status request
		return Response.ok().build();
	}
}
