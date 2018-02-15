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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.EnvStatusDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.backendapi.resources.dto.InfrastructureInfo;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.contracts.InfrasctructureAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides the REST API for the basic information about infrastructure.
 */
@Path("/infrastructure")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class InfrastructureInfoResource implements InfrasctructureAPI {

	@Inject
	private ExploratoryDAO expDAO;
	@Inject
	private KeyDAO keyDAO;
	@Inject
	private EnvStatusDAO envDAO;
	@Inject
	private InfrastructureInfoService infrastructureInfoService;
	@Inject
	private SelfServiceApplicationConfiguration configuration;

	/**
	 * Return status of self-service.
	 */
	@GET
	public Response status() {
		return Response.status(Response.Status.OK).build();
	}

	/**
	 * Returns the status of infrastructure: edge.
	 *
	 * @param userInfo user info.
	 */
	@GET
	@Path("/status")
	public HealthStatusPageDTO status(@Auth UserInfo userInfo, @QueryParam("full") @DefaultValue("0") int fullReport) {
		log.debug("Request the status of resources for user {}, report type {}", userInfo.getName(), fullReport);
		try {
			HealthStatusPageDTO status = envDAO.getHealthStatusPageDTO(userInfo.getName(), fullReport != 0)
					.withBillingEnabled(configuration.isBillingSchedulerEnabled())
					.withBackupAllowed(UserRoles.checkAccess(userInfo, RoleType.PAGE, UserRoles.BACKUP, false));
			log.debug("Return the status of resources for user {}: {}", userInfo.getName(), status);
			return status;
		} catch (DlabException e) {
			log.warn("Could not return status of resources for user {}: {}",
					userInfo.getName(), e.getLocalizedMessage(), e);
			throw e;
		}
	}

	/**
	 * Returns the list of the provisioned user resources.
	 *
	 * @param userInfo user info.
	 */
	@GET
	@Path("/info")
	public InfrastructureInfo getUserResources(@Auth UserInfo userInfo) {
		log.debug("Loading list of provisioned resources for user {}", userInfo.getName());
		try {
			Iterable<Document> documents = expDAO.findExploratory(userInfo.getName());
			EdgeInfo edgeInfo = keyDAO.getEdgeInfo(userInfo.getName());

			InfrastructureInfo infrastructureInfo =
					new InfrastructureInfo(infrastructureInfoService.getSharedInfo(edgeInfo), documents);

			log.trace("Infrastructure info: {}", infrastructureInfo);
			return infrastructureInfo;
		} catch (DlabException e) {
			log.error("Could not load list of provisioned resources for user: {}", userInfo.getName(), e);
			throw e;
		}
	}
}
