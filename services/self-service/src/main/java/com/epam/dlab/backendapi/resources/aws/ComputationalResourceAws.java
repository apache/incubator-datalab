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


package com.epam.dlab.backendapi.resources.aws;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.dto.aws.AwsComputationalCreateForm;
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.dto.aws.computational.AwsComputationalResource;
import com.epam.dlab.dto.aws.computational.ClusterConfig;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.epam.dlab.dto.UserInstanceStatus.CREATING;
import static com.epam.dlab.dto.base.DataEngineType.SPARK_STANDALONE;


/**
 * Provides the REST API for the computational resource on AWS.
 */
@Path("/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Service for computational resources on AWS (NOTE: available only on AWS platform)",
		authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH), hidden = true)
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
	@ApiOperation("Creates EMR cluster on AWS")
	@ApiResponses({
			@ApiResponse(code = 302, message = "EMR cluster on AWS with current parameters already exists"),
			@ApiResponse(code = 200, message = "EMR cluster on AWS successfully created")
	})
	public Response createDataEngineService(@ApiParam(hidden = true) @Auth UserInfo userInfo,
											@ApiParam(value = "AWS form DTO for EMR creation", required = true)
											@Valid @NotNull AwsComputationalCreateForm form) {

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
					.config(form.getConfig())
					.version(form.getVersion())
					.build();
			boolean resourceAdded = computationalService.createDataEngineService(userInfo, form,
					awsComputationalResource, form.getProject());
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
	@ApiOperation("Creates Spark cluster on AWS")
	@ApiResponses({
			@ApiResponse(code = 302, message = "Spark cluster on AWS with current parameters already exists"),
			@ApiResponse(code = 200, message = "Spark cluster on AWS successfully created")
	})
	public Response createDataEngine(@ApiParam(hidden = true) @Auth UserInfo userInfo,
									 @ApiParam(value = "Spark cluster create form DTO", required = true)
									 @Valid @NotNull SparkStandaloneClusterCreateForm form) {
		log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);

		validate(form);
		return computationalService.createSparkCluster(userInfo, form, form.getProject())
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
	@ApiOperation("Terminates computational resource (EMR/Spark cluster) on AWS")
	@ApiResponses(@ApiResponse(code = 200, message = "EMR/Spark cluster on AWS successfully terminated"))
	public Response terminate(@ApiParam(hidden = true) @Auth UserInfo userInfo,
							  @ApiParam(value = "Notebook's name", required = true)
							  @PathParam("exploratoryName") String exploratoryName,
							  @ApiParam(value = "Computational resource's name for terminating", required = true)
							  @PathParam("computationalName") String computationalName) {
		log.debug("Terminating computational resource {} for user {}", computationalName, userInfo.getName());

		computationalService.terminateComputational(userInfo, exploratoryName, computationalName);

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
	@Path("/{project}/{exploratoryName}/{computationalName}/stop")
	@ApiOperation("Stops Spark cluster on AWS")
	@ApiResponses(@ApiResponse(code = 200, message = "Spark cluster on AWS successfully stopped"))
	public Response stop(@ApiParam(hidden = true) @Auth UserInfo userInfo,
						 @ApiParam(value = "Notebook's name corresponding to Spark cluster", required = true)
						 @PathParam("exploratoryName") String exploratoryName,
						 @ApiParam(value = "Spark cluster's name for stopping", required = true)
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
	@Path("/{project}/{exploratoryName}/{computationalName}/start")
	@ApiOperation("Starts Spark cluster on AWS")
	@ApiResponses(@ApiResponse(code = 200, message = "Spark cluster on AWS successfully started"))
	public Response start(@ApiParam(hidden = true) @Auth UserInfo userInfo,
						  @ApiParam(value = "Notebook's name corresponding to Spark cluster", required = true)
						  @PathParam("exploratoryName") String exploratoryName,
						  @ApiParam(value = "Spark cluster's name for starting", required = true)
						  @PathParam("computationalName") String computationalName,
						  @ApiParam(value = "Project name", required = true)
						  @PathParam("project") String project) {
		log.debug("Starting computational resource {} for user {}", computationalName, userInfo.getName());

		computationalService.startSparkCluster(userInfo, exploratoryName, computationalName, project);

		return Response.ok().build();
	}

	@PUT
	@Path("dataengine/{exploratoryName}/{computationalName}/config")
	@ApiOperation("Updates Spark cluster configuration on AWS")
	@ApiResponses(
			@ApiResponse(code = 200, message = "Spark cluster configuration on AWS successfully updated")
	)
	public Response updateDataEngineConfig(@ApiParam(hidden = true) @Auth UserInfo userInfo,
										   @ApiParam(value = "Notebook's name corresponding to Spark cluster",
												   required = true)
										   @PathParam("exploratoryName") String exploratoryName,
										   @ApiParam(value = "Spark cluster's name for reconfiguring", required = true)
										   @PathParam("computationalName") String computationalName,
										   @ApiParam(value = "Spark cluster config", required = true)
										   @Valid @NotNull List<ClusterConfig> config) {

		computationalService.updateSparkClusterConfig(userInfo, exploratoryName, computationalName, config);
		return Response.ok().build();
	}

	@GET
	@Path("{exploratoryName}/{computationalName}/config")
	@ApiOperation("Returns Spark cluster configuration on AWS")
	@ApiResponses(
			@ApiResponse(code = 200, message = "Spark cluster configuration on AWS successfully returned")
	)
	public Response getClusterConfig(@ApiParam(hidden = true) @Auth UserInfo userInfo,
									 @ApiParam(value = "Notebook's name corresponding to Spark cluster",
											 required = true)
									 @PathParam("exploratoryName") String exploratoryName,
									 @ApiParam(value = "Spark cluster's name for reconfiguring", required = true)
									 @PathParam("computationalName") String computationalName) {
		return Response.ok(computationalService.getClusterConfig(userInfo, exploratoryName, computationalName)).build();
	}

	private void validate(SparkStandaloneClusterCreateForm form) {

		int instanceCount = Integer.parseInt(form.getDataEngineInstanceCount());

		if (instanceCount < configuration.getMinSparkInstanceCount()
				|| instanceCount > configuration.getMaxSparkInstanceCount()) {
			throw new IllegalArgumentException(String.format("Instance count should be in range [%d..%d]",
					configuration.getMinSparkInstanceCount(), configuration.getMaxSparkInstanceCount()));
		}

		if (DataEngineType.fromDockerImageName(form.getImage()) != SPARK_STANDALONE) {
			throw new IllegalArgumentException(String.format("Unknown data engine %s", form.getImage()));
		}
	}

	private void validate(UserInfo userInfo, AwsComputationalCreateForm formDTO) {
		if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, formDTO.getImage(), userInfo.getRoles())) {
			log.warn("Unauthorized attempt to create a {} by user {}", formDTO.getImage(), userInfo.getName());
			throw new DlabException("You do not have the privileges to create a " + formDTO.getTemplateName());
		}

		int slaveInstanceCount = Integer.parseInt(formDTO.getInstanceCount());
		if (slaveInstanceCount < configuration.getMinEmrInstanceCount() || slaveInstanceCount >
				configuration.getMaxEmrInstanceCount()) {
			log.debug("Creating computational resource {} for user {} fail: Limit exceeded to creation slave " +
							"instances. Minimum is {}, maximum is {}",
					formDTO.getName(), userInfo.getName(), configuration.getMinEmrInstanceCount(),
					configuration.getMaxEmrInstanceCount());
			throw new DlabException("Limit exceeded to creation slave instances. Minimum is " +
					configuration.getMinEmrInstanceCount() + ", maximum is " + configuration.getMaxEmrInstanceCount() +
					".");
		}

		if (formDTO.getSlaveInstanceSpotPctPrice() != null){
			int slaveSpotInstanceBidPct = formDTO.getSlaveInstanceSpotPctPrice();
			if (formDTO.getSlaveInstanceSpot() && (slaveSpotInstanceBidPct < configuration.getMinEmrSpotInstanceBidPct()
					|| slaveSpotInstanceBidPct > configuration.getMaxEmrSpotInstanceBidPct())) {
				log.debug("Creating computational resource {} for user {} fail: Spot instances bidding percentage value " +
								"out of the boundaries. Minimum is {}, maximum is {}",
						formDTO.getName(), userInfo.getName(), configuration.getMinEmrSpotInstanceBidPct(),
						configuration.getMaxEmrSpotInstanceBidPct());
				throw new DlabException("Spot instances bidding percentage value out of the boundaries. Minimum is " +
						configuration.getMinEmrSpotInstanceBidPct() + ", maximum is " +
						configuration.getMaxEmrSpotInstanceBidPct() + ".");
			}
		}
	}
}
