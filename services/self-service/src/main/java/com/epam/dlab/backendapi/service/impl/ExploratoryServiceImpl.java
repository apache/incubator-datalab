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

import java.util.List;
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
			updateExploratoryStatus(userInfo.getName(), exploratoryName, status);

			if (status == STOPPING || status == TERMINATING) {
				updateComputationalStatuses(userInfo.getName(), exploratoryName, TERMINATING);
			}

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
