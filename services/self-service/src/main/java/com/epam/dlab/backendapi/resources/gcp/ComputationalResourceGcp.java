/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
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

package com.epam.dlab.backendapi.resources.gcp;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.dto.gcp.GcpComputationalCreateForm;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.gcp.computational.GcpComputationalResource;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.epam.dlab.dto.UserInstanceStatus.CREATING;


/**
 * Provides the REST API for the computational resource on GCP.
 */
@Path("/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ComputationalResourceGcp implements ComputationalAPI {

	@Inject
	private SelfServiceApplicationConfiguration configuration;
	@Inject
	private ComputationalService computationalService;


	/**
	 * Asynchronously creates Dataproc cluster
	 *
	 * @param userInfo user info.
	 * @param formDTO  DTO info about creation of the computational resource.
	 * @return 200 OK - if request success, 302 Found - for duplicates.
	 * @throws IllegalArgumentException if docker image name is malformed
	 */
	@PUT
	@Path("dataengine-service")
	public Response createDataEngineService(@Auth UserInfo userInfo, @Valid @NotNull GcpComputationalCreateForm
			formDTO) {

		log.debug("Create computational resources for {} | form is {}", userInfo.getName(), formDTO);

		if (DataEngineType.CLOUD_SERVICE == DataEngineType.fromDockerImageName(formDTO.getImage())) {
			validate(userInfo, formDTO);
			GcpComputationalResource gcpComputationalResource = GcpComputationalResource.builder().computationalName
					(formDTO.getName())
					.imageName(formDTO.getImage())
					.templateName(formDTO.getTemplateName())
					.status(CREATING.toString())
					.masterShape(formDTO.getMasterInstanceType())
					.slaveShape(formDTO.getSlaveInstanceType())
					.slaveNumber(formDTO.getSlaveInstanceCount())
					.masterNumber(formDTO.getMasterInstanceCount())
					.preemptibleNumber(formDTO.getPreemptibleCount())
					.config(formDTO.getConfig())
					.version(formDTO.getVersion())
					.checkInactivityRequired(formDTO.isCheckInactivityRequired()).build();
			boolean resourceAdded = computationalService.createDataEngineService(userInfo, formDTO,
					gcpComputationalResource);
			return resourceAdded ? Response.ok().build() : Response.status(Response.Status.FOUND).build();
		}

		throw new IllegalArgumentException("Malformed image " + formDTO.getImage());
	}


	/**
	 * Asynchronously triggers creation of Spark cluster
	 *
	 * @param userInfo user info.
	 * @param form     DTO info about creation of the computational resource.
	 * @return 200 OK - if request success, 302 Found - for duplicates.
	 */
	@PUT
	@Path("dataengine")
	public Response createDataEngine(@Auth UserInfo userInfo, @Valid @NotNull SparkStandaloneClusterCreateForm form) {
		log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);

		if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, form.getImage())) {
			log.warn("Unauthorized attempt to create a {} by user {}", form.getImage(), userInfo.getName());
			throw new DlabException("You do not have the privileges to create a " + form.getTemplateName());
		}

		return computationalService.createSparkCluster(userInfo, form)
				? Response.ok().build()
				: Response.status(Response.Status.FOUND).build();
	}


	/**
	 * Sends request to provisioning service for termination the computational resource for user.
	 *
	 * @param userInfo          user info.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @return 200 OK if operation is successfully triggered
	 */
	@DELETE
	@Path("/{exploratoryName}/{computationalName}/terminate")
	public Response terminate(@Auth UserInfo userInfo,
							  @PathParam("exploratoryName") String exploratoryName,
							  @PathParam("computationalName") String computationalName) {
		log.debug("Terminating computational resource {} for user {}", computationalName, userInfo.getName());

		computationalService.terminateComputationalEnvironment(userInfo, exploratoryName, computationalName);

		return Response.ok().build();
	}

	/**
	 * Sends request to provisioning service for stopping the computational resource for user.
	 *
	 * @param userInfo          user info.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @return 200 OK if operation is successfully triggered
	 */
	@DELETE
	@Path("/{exploratoryName}/{computationalName}/stop")
	public Response stop(@Auth UserInfo userInfo,
						 @PathParam("exploratoryName") String exploratoryName,
						 @PathParam("computationalName") String computationalName) {
		log.debug("Stopping computational resource {} for user {}", computationalName, userInfo.getName());

		computationalService.stopSparkCluster(userInfo, exploratoryName, computationalName);

		return Response.ok().build();
	}

	/**
	 * Sends request to provisioning service for starting the computational resource for user.
	 *
	 * @param userInfo          user info.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @return 200 OK if operation is successfully triggered
	 */
	@PUT
	@Path("/{exploratoryName}/{computationalName}/start")
	public Response start(@Auth UserInfo userInfo,
						  @PathParam("exploratoryName") String exploratoryName,
						  @PathParam("computationalName") String computationalName) {
		log.debug("Starting computational resource {} for user {}", computationalName, userInfo.getName());

		computationalService.startSparkCluster(userInfo, exploratoryName, computationalName);

		return Response.ok().build();
	}

	/**
	 * Updates 'check_inactivity_required' parameter for user's computational resource in database.
	 *
	 * @param userInfo                user info.
	 * @param exploratoryName         name of exploratory.
	 * @param computationalName       name of computational resource.
	 * @param checkInactivityRequired true/false.
	 * @return 200 OK if operation is successfully triggered
	 */
	@PUT
	@Path("/{exploratoryName}/{computationalName}/inactivity")
	public Response updateInactivity(@Auth UserInfo userInfo,
									 @PathParam("exploratoryName") String exploratoryName,
									 @PathParam("computationalName") String computationalName,
									 @QueryParam("check_inactivity") boolean checkInactivityRequired) {
		log.debug("Updating check inactivity cluster flag to {} for computational resource {} affiliated with " +
						"exploratory {} for user {}", checkInactivityRequired, computationalName, exploratoryName,
				userInfo.getName());
		computationalService.updateCheckInactivityFlag(userInfo, exploratoryName, computationalName,
				checkInactivityRequired);
		return Response.ok().build();
	}

	private void validate(@Auth UserInfo userInfo, GcpComputationalCreateForm formDTO) {
		if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, formDTO.getImage())) {
			log.warn("Unauthorized attempt to create a {} by user {}", formDTO.getImage(), userInfo.getName());
			throw new DlabException("You do not have the privileges to create a " + formDTO.getTemplateName());
		}

		int slaveInstanceCount = Integer.parseInt(formDTO.getSlaveInstanceCount());
		int masterInstanceCount = Integer.parseInt(formDTO.getMasterInstanceCount());
		final int total = slaveInstanceCount + masterInstanceCount;
		if (total < configuration.getMinInstanceCount()
				|| total > configuration.getMaxInstanceCount()) {
			log.debug("Creating computational resource {} for user {} fail: Limit exceeded to creation total " +
							"instances. Minimum is {}, maximum is {}", formDTO.getName(), userInfo.getName(),
					configuration.getMinInstanceCount(), configuration.getMaxInstanceCount());
			throw new DlabException("Limit exceeded to creation slave instances. Minimum is " + configuration
					.getMinInstanceCount() + ", maximum is " + configuration.getMaxInstanceCount());
		}

		final int preemptibleInstanceCount = Integer.parseInt(formDTO.getPreemptibleCount());
		if (preemptibleInstanceCount < configuration.getMinDataprocPreemptibleCount()) {
			log.debug("Creating computational resource {} for user {} fail: Limit exceeded to creation preemptible " +
							"instances. Minimum is {}",
					formDTO.getName(), userInfo.getName(), configuration.getMinDataprocPreemptibleCount());
			throw new DlabException("Limit exceeded to creation preemptible instances. " +
					"Minimum is " + configuration.getMinDataprocPreemptibleCount());

		}
	}
}
