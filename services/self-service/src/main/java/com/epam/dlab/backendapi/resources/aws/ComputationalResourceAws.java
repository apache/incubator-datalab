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


package com.epam.dlab.backendapi.resources.aws;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.dto.aws.AwsComputationalCreateForm;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.dto.aws.computational.AwsComputationalResource;
import com.epam.dlab.dto.base.DataEngineType;
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
 * Provides the REST API for the computational resource on AWS.
 */
@Path("/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ComputationalResourceAws implements ComputationalAPI {

	@Inject
	private SelfServiceApplicationConfiguration configuration;
	@Inject
	private ComputationalService computationalService;


	/**
	 * Asynchronously creates EMR cluster
	 *
	 * @param userInfo user info.
	 * @param form     DTO info about creation of the computational resource.
	 * @return 200 OK - if request success, 302 Found - for duplicates.
	 * @throws IllegalArgumentException if docker image name is malformed
	 */
	@PUT
	@Path("dataengine-service")
	public Response createDataEngineService(@Auth UserInfo userInfo, @Valid @NotNull AwsComputationalCreateForm form) {

		log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);

		if (DataEngineType.CLOUD_SERVICE == DataEngineType.fromDockerImageName(form.getImage())) {

			validate(userInfo, form);

			AwsComputationalResource awsComputationalResource = AwsComputationalResource.builder()
					.computationalName(form.getName())
					.imageName(form.getImage())
					.templateName(form.getTemplateName())
					.status(CREATING.toString())
					.masterShape(form.getMasterInstanceType())
					.slaveShape(form.getSlaveInstanceType())
					.slaveSpot(form.getSlaveInstanceSpot())
					.slaveSpotPctPrice(form.getSlaveInstanceSpotPctPrice())
					.slaveNumber(form.getInstanceCount())
					.version(form.getVersion()).build();
			boolean resourceAdded = computationalService.createDataEngineService(userInfo, form,
					awsComputationalResource);
			return resourceAdded ? Response.ok().build() : Response.status(Response.Status.FOUND).build();
		}

		throw new IllegalArgumentException("Malformed image " + form.getImage());
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

	private void validate(@Auth UserInfo userInfo, AwsComputationalCreateForm formDTO) {
		if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, formDTO.getImage())) {
			log.warn("Unauthorized attempt to create a {} by user {}", formDTO.getImage(), userInfo.getName());
			throw new DlabException("You do not have the privileges to create a " + formDTO.getTemplateName());
		}

		int slaveInstanceCount = Integer.parseInt(formDTO.getInstanceCount());
		if (slaveInstanceCount < configuration.getMinEmrInstanceCount() || slaveInstanceCount > configuration
				.getMaxEmrInstanceCount()) {
			log.debug("Creating computational resource {} for user {} fail: Limit exceeded to creation slave " +
							"instances" +
							". Minimum is {}, maximum is {}",
					formDTO.getName(), userInfo.getName(), configuration.getMinEmrInstanceCount(), configuration
							.getMaxEmrInstanceCount());
			throw new DlabException("Limit exceeded to creation slave instances. Minimum is " + configuration
					.getMinEmrInstanceCount() +
					", maximum is " + configuration.getMaxEmrInstanceCount() + ".");
		}

		int slaveSpotInstanceBidPct = formDTO.getSlaveInstanceSpotPctPrice();
		if (formDTO.getSlaveInstanceSpot() && (slaveSpotInstanceBidPct < configuration.getMinEmrSpotInstanceBidPct()
				|| slaveSpotInstanceBidPct > configuration.getMaxEmrSpotInstanceBidPct())) {
			log.debug("Creating computational resource {} for user {} fail: Spot instances bidding percentage value " +
							"out of the boundaries. Minimum is {}, maximum is {}",
					formDTO.getName(), userInfo.getName(), configuration.getMinEmrSpotInstanceBidPct(), configuration
							.getMaxEmrSpotInstanceBidPct());
			throw new DlabException("Spot instances bidding percentage value out of the boundaries. Minimum is " +
					configuration.getMinEmrSpotInstanceBidPct() +
					", maximum is " + configuration.getMaxEmrSpotInstanceBidPct() + ".");
		}
	}
}
