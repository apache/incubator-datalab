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
import com.epam.dlab.backendapi.annotation.Audit;
import com.epam.dlab.backendapi.annotation.BudgetLimited;
import com.epam.dlab.backendapi.annotation.Info;
import com.epam.dlab.backendapi.annotation.Project;
import com.epam.dlab.backendapi.annotation.ResourceName;
import com.epam.dlab.backendapi.annotation.User;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.ComputationalTemplatesDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.InfrastructureTemplateService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.service.TagService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.aws.computational.ClusterConfig;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.computational.ComputationalClusterConfigDTO;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.dto.computational.SparkStandaloneClusterResource;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.dlab.backendapi.domain.AuditActionEnum.CREATE_DATA_ENGINE;
import static com.epam.dlab.backendapi.domain.AuditActionEnum.CREATE_DATA_ENGINE_SERVICE;
import static com.epam.dlab.backendapi.domain.AuditActionEnum.TERMINATE_COMPUTATIONAL;
import static com.epam.dlab.dto.UserInstanceStatus.CREATING;
import static com.epam.dlab.dto.UserInstanceStatus.FAILED;
import static com.epam.dlab.dto.UserInstanceStatus.RECONFIGURING;
import static com.epam.dlab.dto.UserInstanceStatus.STARTING;
import static com.epam.dlab.dto.UserInstanceStatus.STOPPING;
import static com.epam.dlab.dto.UserInstanceStatus.TERMINATING;
import static com.epam.dlab.dto.base.DataEngineType.CLOUD_SERVICE;
import static com.epam.dlab.dto.base.DataEngineType.SPARK_STANDALONE;
import static com.epam.dlab.rest.contracts.ComputationalAPI.COMPUTATIONAL_CREATE_CLOUD_SPECIFIC;

@Singleton
@Slf4j
public class ComputationalServiceImpl implements ComputationalService {

	private static final String COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT = "Could not update the status of " +
			"computational resource {} for user {}";
	private static final EnumMap<DataEngineType, String> DATA_ENGINE_TYPE_TERMINATE_URLS;
	private static final String DATAENGINE_NOT_PRESENT_FORMAT = "There is no %s dataengine %s for exploratory %s";
	private static final String RUNNING_COMP_RES_NOT_FOUND = "Running computational resource with " +
			"name %s for exploratory %s not found";

	static {
		DATA_ENGINE_TYPE_TERMINATE_URLS = new EnumMap<>(DataEngineType.class);
		DATA_ENGINE_TYPE_TERMINATE_URLS.put(SPARK_STANDALONE, ComputationalAPI.COMPUTATIONAL_TERMINATE_SPARK);
		DATA_ENGINE_TYPE_TERMINATE_URLS.put(CLOUD_SERVICE, ComputationalAPI.COMPUTATIONAL_TERMINATE_CLOUD_SPECIFIC);
	}

	private final ProjectService projectService;
	private final ExploratoryDAO exploratoryDAO;
	private final ComputationalDAO computationalDAO;
	private final RESTService provisioningService;
	private final RequestBuilder requestBuilder;
	private final RequestId requestId;
	private final TagService tagService;
	private final EndpointService endpointService;
	private final InfrastructureTemplateService templateService;

	@Inject
	public ComputationalServiceImpl(ProjectService projectService, ExploratoryDAO exploratoryDAO, ComputationalDAO computationalDAO,
									@Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
									RequestBuilder requestBuilder, RequestId requestId, TagService tagService,
									EndpointService endpointService, InfrastructureTemplateService templateService) {
		this.projectService = projectService;
		this.exploratoryDAO = exploratoryDAO;
		this.computationalDAO = computationalDAO;
		this.provisioningService = provisioningService;
		this.requestBuilder = requestBuilder;
		this.requestId = requestId;
		this.tagService = tagService;
		this.endpointService = endpointService;
		this.templateService = templateService;
	}


	@Override
	public ComputationalTemplatesDTO getComputationalNamesAndTemplates(UserInfo user, String project, String endpoint) {
		List<FullComputationalTemplate> computationalTemplates = templateService.getComputationalTemplates(user, project, endpoint);
		List<UserInstanceDTO> userInstances = exploratoryDAO.fetchExploratoryFieldsForProjectWithComp(project);

		List<String> projectComputations = userInstances
				.stream()
				.map(UserInstanceDTO::getResources)
				.flatMap(Collection::stream)
				.map(UserComputationalResource::getComputationalName)
				.collect(Collectors.toList());
		List<String> userComputations = userInstances
				.stream()
				.filter(instance -> instance.getUser().equalsIgnoreCase(user.getName()))
				.map(UserInstanceDTO::getResources)
				.flatMap(Collection::stream)
				.map(UserComputationalResource::getComputationalName)
				.collect(Collectors.toList());

		return new ComputationalTemplatesDTO(computationalTemplates, userComputations, projectComputations);
	}

	@BudgetLimited
	@Audit(action = CREATE_DATA_ENGINE)
	@Override
	public boolean createSparkCluster(@User UserInfo userInfo, @ResourceName String resourceName, SparkStandaloneClusterCreateForm form, @Project String project,
									  @Info List<String> auditInfo) {
		final ProjectDTO projectDTO = projectService.get(project);
		final UserInstanceDTO instance =
				exploratoryDAO.fetchExploratoryFields(userInfo.getName(), project, form.getNotebookName());
		final SparkStandaloneClusterResource compResource = createInitialComputationalResource(form);
		compResource.setTags(tagService.getResourceTags(userInfo, instance.getEndpoint(), project,
				form.getCustomTag()));
		if (computationalDAO.addComputational(userInfo.getName(), form.getNotebookName(), project, compResource)) {
			try {
				EndpointDTO endpointDTO = endpointService.get(instance.getEndpoint());
				ComputationalBase<?> dto = requestBuilder.newComputationalCreate(userInfo, projectDTO, instance, form, endpointDTO);

				String uuid =
						provisioningService.post(endpointDTO.getUrl() + ComputationalAPI.COMPUTATIONAL_CREATE_SPARK,
								userInfo.getAccessToken(), dto, String.class);
				requestId.put(userInfo.getName(), uuid);
				return true;
			} catch (RuntimeException e) {
				try {
					updateComputationalStatus(userInfo.getName(), project, form.getNotebookName(), form.getName(), FAILED);
				} catch (DlabException d) {
					log.error(COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT, form.getName(), userInfo.getName(), d);
				}
				throw e;
			}
		} else {
			log.debug("Computational with name {} is already existing for user {}", form.getName(),
					userInfo.getName());
			return false;
		}
	}

	@Audit(action = TERMINATE_COMPUTATIONAL)
	@Override
	public void terminateComputational(@User UserInfo userInfo, String resourceCreator, String project, String exploratoryName, @ResourceName String computationalName,
									   @Info List<String> auditInfo) {
		try {
			updateComputationalStatus(resourceCreator, project, exploratoryName, computationalName, TERMINATING);

			final UserInstanceDTO userInstanceDTO = exploratoryDAO.fetchExploratoryFields(resourceCreator, project, exploratoryName);
			UserComputationalResource compResource = computationalDAO.fetchComputationalFields(resourceCreator, project, exploratoryName, computationalName);

			final DataEngineType dataEngineType = compResource.getDataEngineType();
			EndpointDTO endpointDTO = endpointService.get(userInstanceDTO.getEndpoint());
			ComputationalTerminateDTO dto = requestBuilder.newComputationalTerminate(resourceCreator, userInstanceDTO, compResource, endpointDTO);

			final String provisioningUrl = Optional.ofNullable(DATA_ENGINE_TYPE_TERMINATE_URLS.get(dataEngineType))
					.orElseThrow(UnsupportedOperationException::new);
			final String uuid = provisioningService.post(endpointDTO.getUrl() + provisioningUrl, userInfo.getAccessToken(), dto, String.class);
			requestId.put(resourceCreator, uuid);
		} catch (RuntimeException re) {
			try {
				updateComputationalStatus(resourceCreator, project, exploratoryName, computationalName, FAILED);
			} catch (DlabException e) {
				log.error(COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT, computationalName, resourceCreator, e);
			}
			throw re;
		}
	}

	@BudgetLimited
	@Audit(action = CREATE_DATA_ENGINE_SERVICE)
	@Override
	public boolean createDataEngineService(@User UserInfo userInfo, @ResourceName String resourceName, ComputationalCreateFormDTO formDTO,
										   UserComputationalResource computationalResource, @Project String project, @Info List<String> auditInfo) {
		final ProjectDTO projectDTO = projectService.get(project);
		final UserInstanceDTO instance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), project, formDTO
				.getNotebookName());
		final Map<String, String> tags = tagService.getResourceTags(userInfo, instance.getEndpoint(), project,
				formDTO.getCustomTag());
		computationalResource.setTags(tags);
		boolean isAdded = computationalDAO.addComputational(userInfo.getName(), formDTO.getNotebookName(), project,
				computationalResource);

		if (isAdded) {
			try {
				EndpointDTO endpointDTO = endpointService.get(instance.getEndpoint());
				String uuid =
						provisioningService.post(endpointDTO.getUrl() + COMPUTATIONAL_CREATE_CLOUD_SPECIFIC,
								userInfo.getAccessToken(),
								requestBuilder.newComputationalCreate(userInfo, projectDTO, instance, formDTO, endpointDTO),
								String.class);
				requestId.put(userInfo.getName(), uuid);
				return true;
			} catch (Exception t) {
				try {
					updateComputationalStatus(userInfo.getName(), project, formDTO.getNotebookName(),
							formDTO.getName(), FAILED);
				} catch (DlabException e) {
					log.error(COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT, formDTO.getName(), userInfo.getName(), e);
				}
				throw new DlabException("Could not send request for creation the computational resource " + formDTO
						.getName() + ": " + t.getLocalizedMessage(), t);
			}
		} else {
			log.debug("Used existing computational resource {} for user {}", formDTO.getName(), userInfo.getName());
			return false;
		}
	}

	@Override
	public void stopSparkCluster(UserInfo userInfo, String project, String expName, String compName) {
		final UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), project, expName, true);
		final UserInstanceStatus requiredStatus = UserInstanceStatus.RUNNING;
		if (computationalWithStatusResourceExist(compName, userInstance, requiredStatus)) {
			log.debug("{} spark cluster {} for userInstance {}", STOPPING.toString(), compName, expName);
			updateComputationalStatus(userInfo.getName(), project, expName, compName, STOPPING);
			EndpointDTO endpointDTO = endpointService.get(userInstance.getEndpoint());
			final String uuid =
					provisioningService.post(endpointDTO.getUrl() + ComputationalAPI.COMPUTATIONAL_STOP_SPARK,
							userInfo.getAccessToken(),
							requestBuilder.newComputationalStop(userInfo, userInstance, compName, endpointDTO),
							String.class);
			requestId.put(userInfo.getName(), uuid);
		} else {
			throw new IllegalStateException(String.format(DATAENGINE_NOT_PRESENT_FORMAT,
					requiredStatus.toString(), compName, expName));
		}

	}

	@BudgetLimited
	@Override
	public void startSparkCluster(UserInfo userInfo, String expName, String compName, @Project String project) {
		final UserInstanceDTO userInstance =
				exploratoryDAO.fetchExploratoryFields(userInfo.getName(), project, expName, true);
		final UserInstanceStatus requiredStatus = UserInstanceStatus.STOPPED;
		if (computationalWithStatusResourceExist(compName, userInstance, requiredStatus)) {
			log.debug("{} spark cluster {} for userInstance {}", STARTING.toString(), compName, expName);
			updateComputationalStatus(userInfo.getName(), project, expName, compName, STARTING);
			EndpointDTO endpointDTO = endpointService.get(userInstance.getEndpoint());
			final String uuid =
					provisioningService.post(endpointDTO.getUrl() + ComputationalAPI.COMPUTATIONAL_START_SPARK,
							userInfo.getAccessToken(),
							requestBuilder.newComputationalStart(userInfo, userInstance, compName, endpointDTO),
							String.class);
			requestId.put(userInfo.getName(), uuid);
		} else {
			throw new IllegalStateException(String.format(DATAENGINE_NOT_PRESENT_FORMAT,
					requiredStatus.toString(), compName, expName));
		}
	}

	@Override
	public void updateSparkClusterConfig(UserInfo userInfo, String project, String exploratoryName, String computationalName,
										 List<ClusterConfig> config) {
		final String userName = userInfo.getName();
		final String token = userInfo.getAccessToken();
		final UserInstanceDTO userInstanceDTO = exploratoryDAO
				.fetchExploratoryFields(userName, project, exploratoryName, true);
		final UserComputationalResource compResource = userInstanceDTO
				.getResources()
				.stream()
				.filter(cr -> cr.getComputationalName().equals(computationalName) && cr.getStatus().equals(UserInstanceStatus.RUNNING.toString()))
				.findAny()
				.orElseThrow(() -> new ResourceNotFoundException(String.format(RUNNING_COMP_RES_NOT_FOUND,
						computationalName, exploratoryName)));
		EndpointDTO endpointDTO = endpointService.get(userInstanceDTO.getEndpoint());
		final ComputationalClusterConfigDTO clusterConfigDto = requestBuilder.newClusterConfigUpdate(userInfo,
				userInstanceDTO, compResource, config, endpointDTO);
		final String uuid =
				provisioningService.post(endpointDTO.getUrl() + ComputationalAPI.COMPUTATIONAL_RECONFIGURE_SPARK,
						token, clusterConfigDto, String.class);
		computationalDAO.updateComputationalFields(new ComputationalStatusDTO()
				.withProject(userInstanceDTO.getProject())
				.withComputationalName(computationalName)
				.withExploratoryName(exploratoryName)
				.withConfig(config)
				.withStatus(RECONFIGURING.toString())
				.withUser(userName));
		requestId.put(userName, uuid);

	}

	/**
	 * Returns computational resource's data by name for user's exploratory.
	 *
	 * @param user              user
	 * @param project           name of project
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @return corresponding computational resource's data or empty data if resource doesn't exist.
	 */
	@Override
	public Optional<UserComputationalResource> getComputationalResource(String user, String project, String exploratoryName,
																		String computationalName) {
		try {
			return Optional.of(computationalDAO.fetchComputationalFields(user, project, exploratoryName, computationalName));
		} catch (DlabException e) {
			log.warn("Computational resource {} affiliated with exploratory {} for user {} not found.",
					computationalName, exploratoryName, user);
		}
		return Optional.empty();
	}

	@Override
	public List<ClusterConfig> getClusterConfig(UserInfo userInfo, String project, String exploratoryName, String computationalName) {
		return computationalDAO.getClusterConfig(userInfo.getName(), project, exploratoryName, computationalName);
	}

	/**
	 * Updates the status of computational resource in database.
	 *
	 * @param user              user name.
	 * @param project           project name
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @param status            status
	 */
	private void updateComputationalStatus(String user, String project, String exploratoryName, String computationalName,
										   UserInstanceStatus status) {
		ComputationalStatusDTO computationalStatus = new ComputationalStatusDTO()
				.withUser(user)
				.withProject(project)
				.withExploratoryName(exploratoryName)
				.withComputationalName(computationalName)
				.withStatus(status);

		computationalDAO.updateComputationalStatus(computationalStatus);
	}

	private SparkStandaloneClusterResource createInitialComputationalResource(SparkStandaloneClusterCreateForm form) {

		return SparkStandaloneClusterResource.builder()
				.computationalName(form.getName())
				.imageName(form.getImage())
				.templateName(form.getTemplateName())
				.status(CREATING.toString())
				.dataEngineInstanceCount(form.getDataEngineInstanceCount())
				.dataEngineInstanceShape(form.getDataEngineInstanceShape())
				.config(form.getConfig())
				.build();
	}

	private boolean computationalWithStatusResourceExist(String compName,
														 UserInstanceDTO ui, UserInstanceStatus status) {
		return ui.getResources()
				.stream()
				.anyMatch(c -> computationalWithNameAndStatus(compName, c, status));
	}

	private boolean computationalWithNameAndStatus(String computationalName, UserComputationalResource compResource,
												   UserInstanceStatus status) {
		return compResource.getStatus().equals(status.toString()) &&
				compResource.getDataEngineType() == SPARK_STANDALONE &&
				compResource.getComputationalName().equals(computationalName);
	}
}
