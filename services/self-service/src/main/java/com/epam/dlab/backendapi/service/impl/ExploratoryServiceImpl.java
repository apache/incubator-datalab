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
import com.epam.dlab.backendapi.annotation.BudgetLimited;
import com.epam.dlab.backendapi.annotation.Project;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.GitCredsDAO;
import com.epam.dlab.backendapi.dao.ImageExploratoryDao;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ExploratoryCreatePopUp;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.service.TagService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.StatusEnvBaseDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.aws.computational.ClusterConfig;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.ExploratoryActionDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.dto.exploratory.ExploratoryReconfigureSparkClusterActionDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.exploratory.Exploratory;
import com.epam.dlab.model.library.Library;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.dlab.dto.UserInstanceStatus.CREATING;
import static com.epam.dlab.dto.UserInstanceStatus.FAILED;
import static com.epam.dlab.dto.UserInstanceStatus.STARTING;
import static com.epam.dlab.dto.UserInstanceStatus.STOPPED;
import static com.epam.dlab.dto.UserInstanceStatus.STOPPING;
import static com.epam.dlab.dto.UserInstanceStatus.TERMINATED;
import static com.epam.dlab.dto.UserInstanceStatus.TERMINATING;
import static com.epam.dlab.rest.contracts.ExploratoryAPI.EXPLORATORY_CREATE;
import static com.epam.dlab.rest.contracts.ExploratoryAPI.EXPLORATORY_RECONFIGURE_SPARK;
import static com.epam.dlab.rest.contracts.ExploratoryAPI.EXPLORATORY_START;
import static com.epam.dlab.rest.contracts.ExploratoryAPI.EXPLORATORY_STOP;
import static com.epam.dlab.rest.contracts.ExploratoryAPI.EXPLORATORY_TERMINATE;

@Slf4j
@Singleton
public class ExploratoryServiceImpl implements ExploratoryService {

	@Inject
	private ProjectService projectService;
	@Inject
	private ExploratoryDAO exploratoryDAO;
	@Inject
	private ComputationalDAO computationalDAO;
	@Inject
	private GitCredsDAO gitCredsDAO;
	@Inject
	private ImageExploratoryDao imageExploratoryDao;
	@Inject
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;
	@Inject
	private RequestBuilder requestBuilder;
	@Inject
	private RequestId requestId;
	@Inject
	private TagService tagService;
	@Inject
	private EndpointService endpointService;

	@BudgetLimited
	@Override
	public String start(UserInfo userInfo, String exploratoryName, @Project String project) {
		return action(userInfo, exploratoryName, EXPLORATORY_START, STARTING);
	}

	@Override
	public String stop(UserInfo userInfo, String exploratoryName) {
		return action(userInfo, exploratoryName, EXPLORATORY_STOP, STOPPING);
	}

	@Override
	public String terminate(UserInfo userInfo, String exploratoryName) {
		return action(userInfo, exploratoryName, EXPLORATORY_TERMINATE, TERMINATING);
	}

	@BudgetLimited
	@Override
	public String create(UserInfo userInfo, Exploratory exploratory, @Project String project) {
		boolean isAdded = false;
		try {
			final ProjectDTO projectDTO = projectService.get(project);
			final EndpointDTO endpointDTO = endpointService.get(exploratory.getEndpoint());
			final UserInstanceDTO userInstanceDTO = getUserInstanceDTO(userInfo, exploratory, project, endpointDTO.getCloudProvider());
			exploratoryDAO.insertExploratory(userInstanceDTO);
			isAdded = true;
			final ExploratoryGitCredsDTO gitCreds = gitCredsDAO.findGitCreds(userInfo.getName());
			log.debug("Created exploratory environment {} for user {}", exploratory.getName(), userInfo.getName());
			final String uuid =
					provisioningService.post(endpointDTO.getUrl() + EXPLORATORY_CREATE,
							userInfo.getAccessToken(),
							requestBuilder.newExploratoryCreate(projectDTO, endpointDTO, exploratory, userInfo,
									gitCreds, userInstanceDTO.getTags()),
							String.class);
			requestId.put(userInfo.getName(), uuid);
			return uuid;
		} catch (Exception t) {
			log.error("Could not update the status of exploratory environment {} with name {} for user {}",
					exploratory.getDockerImage(), exploratory.getName(), userInfo.getName(), t);
			if (isAdded) {
				updateExploratoryStatusSilent(userInfo.getName(), exploratory.getName(), FAILED);
			}
			throw new DlabException("Could not create exploratory environment " + exploratory.getName() + " for user "
					+ userInfo.getName() + ": " + Optional.ofNullable(t.getCause()).map(Throwable::getMessage).orElse(t.getMessage()), t);
		}
	}

	@Override
	public void updateExploratoryStatuses(String user, UserInstanceStatus status) {
		exploratoryDAO.fetchUserExploratoriesWhereStatusNotIn(user, TERMINATED, FAILED)
				.forEach(ui -> updateExploratoryStatus(ui.getExploratoryName(), status, user));
	}

	@Override
	public void updateProjectExploratoryStatuses(String project, String endpoint, UserInstanceStatus status) {
		exploratoryDAO.fetchProjectExploratoriesWhereStatusNotIn(project, endpoint, TERMINATED, FAILED)
				.forEach(ui -> updateExploratoryStatus(ui.getExploratoryName(), status, ui.getUser()));
	}

	/**
	 * Updates parameter 'reuploadKeyRequired' for corresponding user's exploratories with allowable statuses.
	 *
	 * @param user                user.
	 * @param reuploadKeyRequired true/false.
	 * @param exploratoryStatuses allowable exploratories' statuses.
	 */
	@Override
	public void updateExploratoriesReuploadKeyFlag(String user, boolean reuploadKeyRequired,
												   UserInstanceStatus... exploratoryStatuses) {
		exploratoryDAO.updateReuploadKeyForExploratories(user, reuploadKeyRequired, exploratoryStatuses);
	}

	/**
	 * Returns list of user's exploratories and corresponding computational resources where both of them have
	 * predefined statuses.
	 *
	 * @param user                user.
	 * @param exploratoryStatus   status for exploratory environment.
	 * @param computationalStatus status for computational resource affiliated with the exploratory.
	 * @return list with user instances.
	 */
	@Override
	public List<UserInstanceDTO> getInstancesWithStatuses(String user, UserInstanceStatus exploratoryStatus,
														  UserInstanceStatus computationalStatus) {
		return getExploratoriesWithStatus(user, exploratoryStatus).stream()
				.map(e -> e.withResources(computationalResourcesWithStatus(e, computationalStatus)))
				.collect(Collectors.toList());
	}

	@Override
	public void updateClusterConfig(UserInfo userInfo, String exploratoryName, List<ClusterConfig> config) {
		final String userName = userInfo.getName();
		final String token = userInfo.getAccessToken();
		final UserInstanceDTO userInstanceDTO = exploratoryDAO.fetchRunningExploratoryFields(userName,
				exploratoryName);
		EndpointDTO endpointDTO = endpointService.get(userInstanceDTO.getEndpoint());
		final ExploratoryReconfigureSparkClusterActionDTO updateClusterConfigDTO =
				requestBuilder.newClusterConfigUpdate(userInfo, userInstanceDTO, config, endpointDTO);
		final String uuid = provisioningService.post(endpointDTO.getUrl() + EXPLORATORY_RECONFIGURE_SPARK,
				token, updateClusterConfigDTO,
				String.class);
		requestId.put(userName, uuid);
		exploratoryDAO.updateExploratoryFields(new ExploratoryStatusDTO()
				.withUser(userName)
				.withExploratoryName(exploratoryName)
				.withConfig(config)
				.withStatus(UserInstanceStatus.RECONFIGURING.toString()));
	}

	/**
	 * Returns user instance's data by it's name.
	 *
	 * @param user            user.
	 * @param exploratoryName name of exploratory.
	 * @return corresponding user instance's data or empty data if resource doesn't exist.
	 */
	@Override
	public Optional<UserInstanceDTO> getUserInstance(String user, String exploratoryName) {
		try {
			return Optional.of(exploratoryDAO.fetchExploratoryFields(user, exploratoryName));
		} catch (DlabException e) {
			log.warn("User instance with exploratory name {} for user {} not found.", exploratoryName, user);
		}
		return Optional.empty();
	}

	@Override
	public List<ClusterConfig> getClusterConfig(UserInfo user, String exploratoryName) {
		return exploratoryDAO.getClusterConfig(user.getName(), exploratoryName);
	}

	@Override
	public ExploratoryCreatePopUp getUserInstances(UserInfo user) {
		List<ProjectDTO> userProjects = projectService.getUserProjects(user, false);
		Map<String, List<String>> collect = userProjects.stream()
				.collect(Collectors.toMap(ProjectDTO::getName, this::getProjectExploratoryNames));
		return new ExploratoryCreatePopUp(userProjects, collect);
	}

	private List<String> getProjectExploratoryNames(ProjectDTO project) {
		return exploratoryDAO.fetchExploratoryFieldsForProject(project.getName()).stream()
				.map(UserInstanceDTO::getExploratoryName)
				.collect(Collectors.toList());
	}


	private List<UserComputationalResource> computationalResourcesWithStatus(UserInstanceDTO userInstance,
																			 UserInstanceStatus computationalStatus) {
		return userInstance.getResources().stream()
				.filter(resource -> resource.getStatus().equals(computationalStatus.toString()))
				.collect(Collectors.toList());
	}

	/**
	 * Returns list of user's exploratories with predefined status.
	 *
	 * @param user   user.
	 * @param status status for exploratory environment.
	 * @return list of user's instances.
	 */
	private List<UserInstanceDTO> getExploratoriesWithStatus(String user, UserInstanceStatus status) {
		return exploratoryDAO.fetchUserExploratoriesWhereStatusIn(user, true, status);
	}

	/**
	 * Sends the post request to the provisioning service and update the status of exploratory environment.
	 *
	 * @param userInfo        user info.
	 * @param exploratoryName name of exploratory environment.
	 * @param action          action for exploratory environment.
	 * @param status          status for exploratory environment.
	 * @return Invocation request as JSON string.
	 */
	private String action(UserInfo userInfo, String exploratoryName, String action, UserInstanceStatus status) {
		try {
			updateExploratoryStatus(exploratoryName, status, userInfo.getName());

			UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), exploratoryName);
			EndpointDTO endpointDTO = endpointService.get(userInstance.getEndpoint());
			final String uuid =
					provisioningService.post(endpointDTO.getUrl() + action, userInfo.getAccessToken(),
							getExploratoryActionDto(userInfo, status, userInstance, endpointDTO), String.class);
			requestId.put(userInfo.getName(), uuid);
			return uuid;
		} catch (Exception t) {
			log.error("Could not {} exploratory environment {} for user {}",
					StringUtils.substringAfter(action, "/"), exploratoryName, userInfo.getName(), t);
			updateExploratoryStatusSilent(userInfo.getName(), exploratoryName, FAILED);
			final String errorMsg = String.format("Could not %s exploratory environment %s: %s",
					StringUtils.substringAfter(action, "/"), exploratoryName,
					Optional.ofNullable(t.getCause()).map(Throwable::getMessage).orElse(t.getMessage()));
			throw new DlabException(errorMsg, t);
		}
	}

	private void updateExploratoryStatus(String exploratoryName, UserInstanceStatus status, String user) {
		updateExploratoryStatus(user, exploratoryName, status);

		if (status == STOPPING) {
			updateComputationalStatuses(user, exploratoryName, STOPPING, TERMINATING, FAILED, TERMINATED, STOPPED);
		} else if (status == TERMINATING) {
			updateComputationalStatuses(user, exploratoryName, TERMINATING, TERMINATING, TERMINATED, FAILED);
		} else if (status == TERMINATED) {
			updateComputationalStatuses(user, exploratoryName, TERMINATED, TERMINATED, TERMINATED, FAILED);
		}
	}

	private ExploratoryActionDTO<?> getExploratoryActionDto(UserInfo userInfo, UserInstanceStatus status,
															UserInstanceDTO userInstance, EndpointDTO endpointDTO) {
		ExploratoryActionDTO<?> dto;
		if (status != UserInstanceStatus.STARTING) {
			dto = requestBuilder.newExploratoryStop(userInfo, userInstance, endpointDTO);
		} else {
			dto = requestBuilder.newExploratoryStart(
					userInfo, userInstance, endpointDTO, gitCredsDAO.findGitCreds(userInfo.getName()));

		}
		return dto;
	}


	/**
	 * Updates the status of exploratory environment.
	 *
	 * @param user            user name
	 * @param exploratoryName name of exploratory environment.
	 * @param status          status for exploratory environment.
	 */
	private void updateExploratoryStatus(String user, String exploratoryName, UserInstanceStatus status) {
		StatusEnvBaseDTO<?> exploratoryStatus = createStatusDTO(user, exploratoryName, status);
		exploratoryDAO.updateExploratoryStatus(exploratoryStatus);
	}

	/**
	 * Updates the status of exploratory environment without exceptions. If exception occurred then logging it.
	 *
	 * @param user            user name
	 * @param exploratoryName name of exploratory environment.
	 * @param status          status for exploratory environment.
	 */
	private void updateExploratoryStatusSilent(String user, String exploratoryName, UserInstanceStatus status) {
		try {
			updateExploratoryStatus(user, exploratoryName, status);
		} catch (DlabException e) {
			log.error("Could not update the status of exploratory environment {} for user {} to {}",
					exploratoryName, user, status, e);
		}
	}

	private void updateComputationalStatuses(String user, String exploratoryName, UserInstanceStatus
			dataEngineStatus, UserInstanceStatus dataEngineServiceStatus, UserInstanceStatus... excludedStatuses) {
		log.debug("updating status for all computational resources of {} for user {}: DataEngine {}, " +
				"dataengine-service {}", exploratoryName, user, dataEngineStatus, dataEngineServiceStatus);
		computationalDAO.updateComputationalStatusesForExploratory(user, exploratoryName, dataEngineStatus,
				dataEngineServiceStatus, excludedStatuses);
	}

	/**
	 * Instantiates and returns the descriptor of exploratory environment status.
	 *
	 * @param user            user name
	 * @param exploratoryName name of exploratory environment.
	 * @param status          status for exploratory environment.
	 */
	private StatusEnvBaseDTO<?> createStatusDTO(String user, String exploratoryName, UserInstanceStatus status) {
		return new ExploratoryStatusDTO()
				.withUser(user)
				.withExploratoryName(exploratoryName)
				.withStatus(status);
	}

	private UserInstanceDTO getUserInstanceDTO(UserInfo userInfo, Exploratory exploratory, String project, CloudProvider cloudProvider) {
		final UserInstanceDTO userInstance = new UserInstanceDTO()
				.withUser(userInfo.getName())
				.withExploratoryName(exploratory.getName())
				.withStatus(CREATING.toString())
				.withImageName(exploratory.getDockerImage())
				.withImageVersion(exploratory.getVersion())
				.withTemplateName(exploratory.getTemplateName())
				.withClusterConfig(exploratory.getClusterConfig())
				.withShape(exploratory.getShape())
				.withProject(project)
				.withEndpoint(exploratory.getEndpoint())
				.withCloudProvider(cloudProvider.toString())
				.withTags(tagService.getResourceTags(userInfo, exploratory.getEndpoint(), project,
						exploratory.getExploratoryTag()));
		if (StringUtils.isNotBlank(exploratory.getImageName())) {
			final List<LibInstallDTO> libInstallDtoList = getImageRelatedLibraries(userInfo, exploratory.getImageName(),
					project, exploratory.getEndpoint());
			userInstance.withLibs(libInstallDtoList);
		}
		return userInstance;
	}

	private List<LibInstallDTO> getImageRelatedLibraries(UserInfo userInfo, String imageFullName, String project,
														 String endpoint) {
		final List<Library> libraries = imageExploratoryDao.getLibraries(userInfo.getName(), imageFullName, project,
				endpoint, LibStatus.INSTALLED);
		return toLibInstallDtoList(libraries);
	}

	private List<LibInstallDTO> toLibInstallDtoList(List<Library> libraries) {
		return libraries
				.stream()
				.map(this::toLibInstallDto)
				.collect(Collectors.toList());
	}

	private LibInstallDTO toLibInstallDto(Library l) {
		return new LibInstallDTO(l.getGroup(), l.getName(), l.getVersion())
				.withStatus(l.getStatus().toString())
				.withErrorMessage(l.getErrorMessage());
	}
}
