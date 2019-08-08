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

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneConfiguration;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.InfrastructureTemplateService;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ComputationalResourceShapeDto;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.dlab.rest.contracts.DockerAPI.DOCKER_COMPUTATIONAL;
import static com.epam.dlab.rest.contracts.DockerAPI.DOCKER_EXPLORATORY;

@Slf4j
public abstract class InfrastructureTemplateServiceBase implements InfrastructureTemplateService {

	@Inject
	private SelfServiceApplicationConfiguration configuration;

	@Inject
	private SettingsDAO settingsDAO;
	@Inject
	private ProjectDAO projectDAO;


	@Inject
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;

	@Override
	public List<ExploratoryMetadataDTO> getExploratoryTemplates(UserInfo user, String project) {

		log.debug("Loading list of exploratory templates for user {} for project {}", user.getName(), project);
		try {
			ExploratoryMetadataDTO[] array =
					provisioningService.get(DOCKER_EXPLORATORY, user.getAccessToken(), ExploratoryMetadataDTO[].class);

			final Set<String> roles = getRoles(user, project);
			return Arrays.stream(array)
					.peek(e -> e.setImage(getSimpleImageName(e.getImage())))
					.filter(e -> exploratoryGpuIssuesAzureFilter(e) &&
							UserRoles.checkAccess(user, RoleType.EXPLORATORY, e.getImage(), roles))
					.peek(e -> filterShapes(user, e.getExploratoryEnvironmentShapes(), RoleType.EXPLORATORY_SHAPES,
							roles))
					.collect(Collectors.toList());

		} catch (DlabException e) {
			log.error("Could not load list of exploratory templates for user: {}", user.getName(), e);
			throw e;
		}
	}

	/**
	 * Removes shapes for which user does not have an access
	 *
	 * @param user              user
	 * @param environmentShapes shape types
	 * @param roleType
	 * @param roles
	 */
	private void filterShapes(UserInfo user, Map<String, List<ComputationalResourceShapeDto>> environmentShapes,
							  RoleType roleType, Set<String> roles) {
		environmentShapes.forEach((k, v) -> v.removeIf(compResShapeDto ->
				!UserRoles.checkAccess(user, roleType, compResShapeDto.getType(), roles)));
	}

	@Override
	public List<FullComputationalTemplate> getComputationalTemplates(UserInfo user, String project) {

		log.debug("Loading list of computational templates for user {}", user.getName());
		try {
			ComputationalMetadataDTO[] array =
					provisioningService.get(DOCKER_COMPUTATIONAL, user.getAccessToken(), ComputationalMetadataDTO[]
							.class);

			final Set<String> roles = getRoles(user, project);

			return Arrays.stream(array)
					.peek(e -> e.setImage(getSimpleImageName(e.getImage())))
					.peek(e -> filterShapes(user, e.getComputationResourceShapes(), RoleType.COMPUTATIONAL_SHAPES,
							user.getRoles()))
					.filter(e -> UserRoles.checkAccess(user, RoleType.COMPUTATIONAL, e.getImage(), roles))
					.map(this::fullComputationalTemplate)
					.collect(Collectors.toList());

		} catch (DlabException e) {
			log.error("Could not load list of computational templates for user: {}", user.getName(), e);
			throw e;
		}
	}

	private Set<String> getRoles(UserInfo user, String project) {
		return projectDAO.get(project)
				.map(ProjectDTO::getGroups)
				.orElse(user.getRoles());
	}

	protected abstract FullComputationalTemplate getCloudFullComputationalTemplate(ComputationalMetadataDTO
																						   metadataDTO);

	/**
	 * Temporary filter for creation of exploratory env due to Azure issues
	 */
	private boolean exploratoryGpuIssuesAzureFilter(ExploratoryMetadataDTO e) {
		return (!"redhat".equals(settingsDAO.getConfOsFamily()) || configuration.getCloudProvider() != CloudProvider
				.AZURE)
				|| !(e.getImage().endsWith("deeplearning") || e.getImage().endsWith("tensor"));
	}

	/**
	 * Return the image name without suffix version.
	 *
	 * @param imageName the name of image.
	 */
	private String getSimpleImageName(String imageName) {
		int separatorIndex = imageName.indexOf(':');
		return (separatorIndex > 0 ? imageName.substring(0, separatorIndex) : imageName);
	}

	/**
	 * Wraps metadata with limits
	 *
	 * @param metadataDTO metadata
	 * @return wrapped object
	 */

	private FullComputationalTemplate fullComputationalTemplate(ComputationalMetadataDTO metadataDTO) {

		DataEngineType dataEngineType = DataEngineType.fromDockerImageName(metadataDTO.getImage());

		if (dataEngineType == DataEngineType.CLOUD_SERVICE) {
			return getCloudFullComputationalTemplate(metadataDTO);
		} else if (dataEngineType == DataEngineType.SPARK_STANDALONE) {
			return new SparkFullComputationalTemplate(metadataDTO,
					SparkStandaloneConfiguration.builder()
							.maxSparkInstanceCount(configuration.getMaxSparkInstanceCount())
							.minSparkInstanceCount(configuration.getMinSparkInstanceCount())
							.build());
		} else {
			throw new IllegalArgumentException("Unknown data engine " + dataEngineType);
		}
	}

	private class SparkFullComputationalTemplate extends FullComputationalTemplate {
		@JsonProperty("limits")
		private SparkStandaloneConfiguration sparkStandaloneConfiguration;

		SparkFullComputationalTemplate(ComputationalMetadataDTO metadataDTO,
									   SparkStandaloneConfiguration sparkStandaloneConfiguration) {
			super(metadataDTO);
			this.sparkStandaloneConfiguration = sparkStandaloneConfiguration;
		}
	}
}
