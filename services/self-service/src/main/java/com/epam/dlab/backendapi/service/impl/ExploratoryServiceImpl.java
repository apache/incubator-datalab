package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.GitCredsDAO;
import com.epam.dlab.backendapi.dao.ImageExploratoryDao;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.StatusEnvBaseDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.*;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.ResourceType;
import com.epam.dlab.model.exloratory.Exploratory;
import com.epam.dlab.model.library.Library;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.epam.dlab.UserInstanceStatus.*;
import static com.epam.dlab.rest.contracts.ExploratoryAPI.*;

@Slf4j
@Singleton
public class ExploratoryServiceImpl implements ExploratoryService {

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

	@Override
	public String start(UserInfo userInfo, String exploratoryName) {
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

	@Override
	public String create(UserInfo userInfo, Exploratory exploratory) {
		boolean isAdded = false;
		try {
			exploratoryDAO.insertExploratory(getUserInstanceDTO(userInfo, exploratory));
			isAdded = true;
			final ExploratoryGitCredsDTO gitCreds = gitCredsDAO.findGitCreds(userInfo.getName());
			log.debug("Created exploratory environment {} for user {}", exploratory.getName(), userInfo.getName());
			final String uuid = provisioningService.post(EXPLORATORY_CREATE, userInfo.getAccessToken(),
					requestBuilder.newExploratoryCreate(exploratory, userInfo, gitCreds), String.class);
			requestId.put(userInfo.getName(), uuid);
			return uuid;
		} catch (Exception t) {
			log.error("Could not update the status of exploratory environment {} with name {} for user {}",
					exploratory.getDockerImage(), exploratory.getName(), userInfo.getName(), t);
			if (isAdded) {
				updateExploratoryStatusSilent(userInfo.getName(), exploratory.getName(), FAILED);
			}
			throw new DlabException("Could not create exploratory environment " + exploratory.getName() + " for user "
					+ userInfo.getName() + ": " + t.getLocalizedMessage(), t);
		}
	}

	@Override
	public void updateExploratoryStatuses(String user, UserInstanceStatus status) {
		exploratoryDAO.fetchUserExploratoriesWhereStatusNotIn(user, TERMINATED, FAILED)
				.forEach(ui -> updateExploratoryStatus(ui.getExploratoryName(), status, user));
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
	 * Returns list which contains full names of user's exploratories and computational resources with predefined
	 * statuses.
	 *
	 * @param user                user.
	 * @param serviceBaseName     service base name.
	 * @param exploratoryStatus   status for exploratory environment.
	 * @param computationalStatus status for computational resource affiliated with the exploratory.
	 * @return list with names of user's resources (notebooks and clusters) in format
	 * 'SBN-user-nb-notebookName' (notebook), 'SBN-user-de/des-notebookName-clusterName' (cluster).
	 */
	public List<String> getResourcesForKeyReuploading(String user, String serviceBaseName,
													  UserInstanceStatus exploratoryStatus,
													  UserInstanceStatus computationalStatus) {
		Map<String, List<String>> populatedResources = getPopulatedExploratoriesWithComputationalResources(user,
				serviceBaseName, exploratoryStatus, computationalStatus);
		List<String> resourceNames = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : populatedResources.entrySet()) {
			resourceNames.add(entry.getKey());
			resourceNames.addAll(entry.getValue());
		}
		return resourceNames;
	}

	/**
	 * Returns list of user's exploratories with computational resources where both of them have predefined statuses.
	 *
	 * @param user                user.
	 * @param serviceBaseName     service base name.
	 * @param exploratoryStatus   status for exploratory environment.
	 * @param computationalStatus status for computational resource affiliated with the exploratory.
	 * @return map with elements [key: exploratoryName in format 'SBN-user-nb-notebookName', value: list of
	 * computational resources' names in format 'SBN-user-de/des-notebookName-clusterName'].
	 */
	private Map<String, List<String>> getPopulatedExploratoriesWithComputationalResources(String user,
																						  String serviceBaseName,
																						  UserInstanceStatus
																								  exploratoryStatus,
																						  UserInstanceStatus
																								  computationalStatus) {
		Map<String, List<Map<String, String>>> resourceMap =
				getExploratoriesWithPredefinedComputationalStatus(user, exploratoryStatus, computationalStatus);
		Map<String, List<String>> populated = new HashMap<>();
		resourceMap.forEach((k, v) -> populated.put(
				populatedExploratoryName(serviceBaseName, user, k),
				v.stream().filter(map -> !map.isEmpty())
						.map(e -> getPopulatedComputationalName(user, serviceBaseName, k, e))
						.filter(Objects::nonNull)
						.collect(Collectors.toList())));
		return populated;
	}

	/**
	 * Returns list of user's exploratories with computational resources where both of them have predefined statuses.
	 *
	 * @param user                user.
	 * @param exploratoryStatus   status for exploratory environment.
	 * @param computationalStatus status for computational resource affiliated with the exploratory.
	 * @return map with elements [key: exploratoryName, value: list of computational resources' names with its type -
	 * 'de' (dataengine) or 'des' (dataengine-service)].
	 */
	private Map<String, List<Map<String, String>>> getExploratoriesWithPredefinedComputationalStatus(String user,
																									 UserInstanceStatus
																											 exploratoryStatus,
																									 UserInstanceStatus
																											 computationalStatus) {
		List<UserInstanceDTO> exploratoriesWithPredefinedStatus =
				getExploratoriesWithPredefinedStatus(user, exploratoryStatus);
		if (exploratoriesWithPredefinedStatus.isEmpty()) {
			return Collections.emptyMap();
		}
		List<UserInstanceDTO> exploratoriesWithPredefinedComputationalTypeAndStatus =
				exploratoriesWithPredefinedStatus.stream().map(e ->
						e.withResources(computationalResourcesWithStatus(e, computationalStatus)))
						.collect(Collectors.toList());
		return exploratoriesWithPredefinedComputationalTypeAndStatus.stream()
				.collect(Collectors.toMap(UserInstanceDTO::getExploratoryName,
						uiDto -> uiDto.getResources().stream().map(this::computationalData)
								.collect(Collectors.toList())));
	}

	private List<UserComputationalResource> computationalResourcesWithStatus(UserInstanceDTO userInstance,
																			 UserInstanceStatus computationalStatus) {
		return userInstance.getResources().stream()
				.filter(resource -> resource.getStatus().equals(computationalStatus.toString()))
				.collect(Collectors.toList());
	}

	private Map<String, String> computationalData(UserComputationalResource compResource) {
		Map<String, String> compResourceData = new HashMap<>();
		if (Objects.nonNull(compResource)) {
			compResourceData.put(compResource.getComputationalName(),
					DataEngineType.fromDockerImageName(compResource.getImageName()) ==
							DataEngineType.SPARK_STANDALONE ? "de" : "des");
		}
		return compResourceData;
	}


	/**
	 * Returns list of user's exploratories with predefined status.
	 *
	 * @param user   user.
	 * @param status status for exploratory environment.
	 * @return list of user's instances.
	 */
	private List<UserInstanceDTO> getExploratoriesWithPredefinedStatus(String user, UserInstanceStatus status) {
		return exploratoryDAO.fetchUserExploratoriesWhereStatusIn(user, status);
	}

	private String getPopulatedComputationalName(String user, String serviceBaseName, String exploratoryName,
												 Map<String, String> computationalData) {
		Optional<Map.Entry<String, String>> entry = computationalData.entrySet().stream().findAny();
		return entry.isPresent() ? populatedComputationalName(serviceBaseName, user, entry.get().getValue(),
				exploratoryName, entry.get().getKey()) : null;
	}

	private String populatedExploratoryName(String serviceBaseName, String user, String exploratoryName) {
		return String.join("-", serviceBaseName, user, "nb", exploratoryName);
	}

	private String populatedComputationalName(String serviceBaseName, String user, String computationalType,
											  String exploratoryName, String computationalName) {
		return String.join("-", serviceBaseName, user, computationalType, exploratoryName,
				computationalName);
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
			final String uuid = provisioningService.post(action, userInfo.getAccessToken(),
					getExploratoryActionDto(userInfo, status, userInstance), String.class);
			requestId.put(userInfo.getName(), uuid);
			return uuid;
		} catch (Exception t) {
			log.error("Could not " + action + " exploratory environment {} for user {}", exploratoryName, userInfo
					.getName(), t);
			updateExploratoryStatusSilent(userInfo.getName(), exploratoryName, FAILED);
			throw new DlabException("Could not " + action + " exploratory environment " + exploratoryName + ": " +
					t.getLocalizedMessage(), t);
		}
	}

	private void updateExploratoryStatus(String exploratoryName, UserInstanceStatus status, String user) {
		updateExploratoryStatus(user, exploratoryName, status);

		if (status == STOPPING) {
			updateComputationalStatuses(user, exploratoryName, STOPPING, TERMINATING);
		} else if (status == TERMINATING) {
			updateComputationalStatuses(user, exploratoryName, TERMINATING);
		}
	}

	private ExploratoryActionDTO<?> getExploratoryActionDto(UserInfo userInfo, UserInstanceStatus status,
															UserInstanceDTO userInstance) {
		ExploratoryActionDTO<?> dto;
		if (status != UserInstanceStatus.STARTING) {
			dto = requestBuilder.newExploratoryStop(userInfo, userInstance);
		} else {
			dto = requestBuilder.newExploratoryStart(
					userInfo, userInstance, gitCredsDAO.findGitCreds(userInfo.getName()));

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

	/**
	 * Updates the computational status of exploratory environment.
	 *
	 * @param user            user name
	 * @param exploratoryName name of exploratory environment.
	 * @param status          status for exploratory environment.
	 */
	private void updateComputationalStatuses(String user, String exploratoryName, UserInstanceStatus status) {
		log.debug("updating status for all computational resources of {} for user {}: {}", exploratoryName, user,
				status);
		StatusEnvBaseDTO<?> exploratoryStatus = createStatusDTO(user, exploratoryName, status);
		computationalDAO.updateComputationalStatusesForExploratory(exploratoryStatus);
	}

	private void updateComputationalStatuses(String user, String exploratoryName, UserInstanceStatus
			dataEngineStatus, UserInstanceStatus dataEngineServiceStatus) {
		log.debug("updating status for all computational resources of {} for user {}: DataEngine {}, " +
				"dataengine-service {}", exploratoryName, user, dataEngineStatus, dataEngineServiceStatus);
		computationalDAO.updateComputationalStatusesForExploratory(user, exploratoryName, dataEngineStatus,
				dataEngineServiceStatus);
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

	private UserInstanceDTO getUserInstanceDTO(UserInfo userInfo, Exploratory exploratory) {
		final UserInstanceDTO userInstance = new UserInstanceDTO()
				.withUser(userInfo.getName())
				.withExploratoryName(exploratory.getName())
				.withStatus(CREATING.toString())
				.withImageName(exploratory.getDockerImage())
				.withImageVersion(exploratory.getVersion())
				.withTemplateName(exploratory.getTemplateName())
				.withShape(exploratory.getShape());
		if (StringUtils.isNotBlank(exploratory.getImageName())) {
			final List<LibInstallDTO> libInstallDtoList = getImageRelatedLibraries(userInfo, exploratory
					.getImageName());
			userInstance.withLibs(libInstallDtoList);
		}
		return userInstance;
	}

	private List<LibInstallDTO> getImageRelatedLibraries(UserInfo userInfo, String imageFullName) {
		final List<Library> libraries = imageExploratoryDao.getLibraries(userInfo.getName(), imageFullName,
				ResourceType.EXPLORATORY, LibStatus.INSTALLED);
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
