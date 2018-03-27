package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SettingsDAO;
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
import java.util.HashMap;
import java.util.List;
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
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;

	@Override
	public List<ExploratoryMetadataDTO> getExploratoryTemplates(UserInfo user) {

		log.debug("Loading list of exploratory templates for user {}", user.getName());
		try {
			ExploratoryMetadataDTO[] array =
					provisioningService.get(DOCKER_EXPLORATORY, user.getAccessToken(), ExploratoryMetadataDTO[].class);

			return Arrays.stream(array)
					.peek(e -> e.setImage(getSimpleImageName(e.getImage())))
					.filter(e -> exploratoryGpuIssuesAzureFilter(e) &&
							UserRoles.checkAccess(user, RoleType.EXPLORATORY, e.getImage()))
					.peek(e -> filterShapes(user, e.getExploratoryEnvironmentShapes()))
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
	 */
	private void filterShapes(UserInfo user, HashMap<String, List<ComputationalResourceShapeDto>> environmentShapes) {
		environmentShapes.forEach((k, v) -> v.removeIf(compResShapeDto ->
				!UserRoles.checkAccess(user, RoleType.EXPLORATORY_SHAPES, compResShapeDto.getType())));
	}

	@Override
	public List<FullComputationalTemplate> getComputationalTemplates(UserInfo user) {

		log.debug("Loading list of computational templates for user {}", user.getName());
		try {
			ComputationalMetadataDTO[] array =
					provisioningService.get(DOCKER_COMPUTATIONAL, user.getAccessToken(), ComputationalMetadataDTO[]
							.class);

			return Arrays.stream(array)
					.peek(e -> e.setImage(getSimpleImageName(e.getImage())))
					.filter(e -> UserRoles.checkAccess(user, RoleType.COMPUTATIONAL, e.getImage()))
					.map(this::fullComputationalTemplate)
					.collect(Collectors.toList());

		} catch (DlabException e) {
			log.error("Could not load list of computational templates for user: {}", user.getName(), e);
			throw e;
		}
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
