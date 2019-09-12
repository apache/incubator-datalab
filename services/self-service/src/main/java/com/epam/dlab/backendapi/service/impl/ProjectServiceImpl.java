package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.annotation.BudgetLimited;
import com.epam.dlab.backendapi.annotation.Project;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.dao.UserGroupDao;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectManagingDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.domain.UpdateProjectDTO;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

@Slf4j
public class ProjectServiceImpl implements ProjectService {

	private static final String CREATE_PRJ_API = "infrastructure/project/create";
	private static final String TERMINATE_PRJ_API = "infrastructure/project/terminate";
	private static final String START_PRJ_API = "infrastructure/project/start";
	private static final String STOP_PRJ_API = "infrastructure/project/stop";
	private static final String ANY_USER_ROLE = "$anyuser";

	private static final String STOP_ACTION = "stop";
	private static final String TERMINATE_ACTION = "terminate";

	private final ProjectDAO projectDAO;
	private final ExploratoryService exploratoryService;
	private final UserGroupDao userGroupDao;
	private final RESTService provisioningService;
	private final RequestId requestId;
	private final RequestBuilder requestBuilder;
	private final SystemUserInfoService systemUserInfoService;
	private final ExploratoryDAO exploratoryDAO;

	@Inject
	public ProjectServiceImpl(ProjectDAO projectDAO, ExploratoryService exploratoryService,
							  UserGroupDao userGroupDao,
							  @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
							  RequestId requestId, RequestBuilder requestBuilder,
							  SystemUserInfoService systemUserInfoService,
							  ExploratoryDAO exploratoryDAO) {
		this.projectDAO = projectDAO;
		this.exploratoryService = exploratoryService;
		this.userGroupDao = userGroupDao;
		this.provisioningService = provisioningService;
		this.requestId = requestId;
		this.requestBuilder = requestBuilder;
		this.systemUserInfoService = systemUserInfoService;
		this.exploratoryDAO = exploratoryDAO;
	}

	@Override
	public List<ProjectDTO> getProjects() {
		return projectDAO.getProjects();
	}

	@Override
	public List<ProjectManagingDTO> getProjectsForManaging() {
		return projectDAO.getProjects().stream().map(p -> new ProjectManagingDTO(
				p.getName(), p.getStatus().toString(), p.getBudget(),
				!exploratoryDAO.fetchProjectExploratoriesWhereStatusIn(p.getName(),
						Arrays.asList(UserInstanceStatus.CREATING, UserInstanceStatus.STARTING,
								UserInstanceStatus.CREATING_IMAGE, UserInstanceStatus.RUNNING)).isEmpty()))
				.collect(Collectors.toList());
	}

	@Override
	public List<ProjectDTO> getUserProjects(UserInfo userInfo) {
		userInfo.getRoles().add(ANY_USER_ROLE);
		return projectDAO.getUserProjectsWithStatus(userInfo, ProjectDTO.Status.ACTIVE);
	}

	@Override
	public List<ProjectDTO> getProjectsWithStatus(ProjectDTO.Status status) {
		return projectDAO.getProjectsWithStatus(status);
	}

	@BudgetLimited
	@Override
	public void create(UserInfo user, ProjectDTO projectDTO) {
		if (!projectDAO.get(projectDTO.getName()).isPresent()) {
			projectDAO.create(projectDTO);
			createProjectOnCloud(user, projectDTO);
		} else {
			throw new ResourceConflictException("Project with passed name already exist in system");
		}
	}

	@Override
	public ProjectDTO get(String name) {
		return projectDAO.get(name)
				.orElseThrow(projectNotFound());
	}

	@Override
	public void terminate(UserInfo userInfo, String name) {
		checkProjectRelatedResourcesInProgress(name, TERMINATE_ACTION);
		projectActionOnCloud(userInfo, name, TERMINATE_PRJ_API, getEndpoint(name));
		exploratoryService.updateProjectExploratoryStatuses(name, UserInstanceStatus.TERMINATING);
		projectDAO.updateStatus(name, ProjectDTO.Status.DELETING);
	}

	@BudgetLimited
	@Override
	public void start(UserInfo userInfo, @Project String name) {
		getEndpoint(name);
		projectActionOnCloud(userInfo, name, START_PRJ_API, getEndpoint(name));
		projectDAO.updateStatus(name, ProjectDTO.Status.ACTIVATING);
	}

	private String getEndpoint(String project) {
		return projectDAO.get(project).map(ProjectDTO::getEndpoints).orElse(Collections.singleton("")).iterator().next(); //TODO change hardcoded value
	}

	@Override
	public void stop(UserInfo userInfo, String name) {
		projectActionOnCloud(userInfo, name, STOP_PRJ_API, getEndpoint(name));
		projectDAO.updateStatus(name, ProjectDTO.Status.DEACTIVATING);
	}

	@Override
	public void stopProjectWithRelatedResources(UserInfo userInfo, String name) {
		checkProjectRelatedResourcesInProgress(name, STOP_ACTION);
		exploratoryDAO.fetchRunningExploratoryFieldsForProject(name).forEach(this::stopNotebook);
		stop(userInfo, name);
	}

	@Override
	public void update(UpdateProjectDTO projectDTO) {
		if (!projectDAO.update(projectDTO)) {
			throw projectNotFound().get();
		}
	}

	@Override
	public void updateBudget(String project, Integer budget) {
		projectDAO.updateBudget(project, budget);
	}

	@Override
	public void updateBudget(List<ProjectDTO> projects) {
		projects.forEach(p -> projectDAO.updateBudget(p.getName(), p.getBudget()));
	}

	@Override
	public boolean isAnyProjectAssigned(UserInfo userInfo) {
		final Set<String> userGroups = concat(userInfo.getRoles().stream(),
				userGroupDao.getUserGroups(userInfo.getName()).stream())
				.collect(toSet());
		return projectDAO.isAnyProjectAssigned(userGroups);
	}

	private void stopNotebook(UserInstanceDTO instance) {
		final UserInfo userInfo = systemUserInfoService.create(instance.getUser());
		exploratoryService.stop(userInfo, instance.getExploratoryName());
	}

	private void checkProjectRelatedResourcesInProgress(String projectName, String action) {
		List<UserInstanceDTO> userInstanceDTOS = exploratoryDAO.fetchProjectExploratoriesWhereStatusIn(projectName,
				Arrays.asList(UserInstanceStatus.CREATING, UserInstanceStatus.STARTING,
						UserInstanceStatus.CREATING_IMAGE), UserInstanceStatus.CREATING,
				UserInstanceStatus.CONFIGURING, UserInstanceStatus.STARTING, UserInstanceStatus.RECONFIGURING,
				UserInstanceStatus.CREATING_IMAGE);
		if (!userInstanceDTOS.isEmpty()) {
			throw new ResourceConflictException((String.format("Can not %s environment because on of user resource " +
					"is in status CREATING or STARTING", action)));
		}
	}

	private void createProjectOnCloud(UserInfo user, ProjectDTO projectDTO) {
		try {
			String uuid = provisioningService.post(CREATE_PRJ_API, user.getAccessToken(),
					requestBuilder.newProjectCreate(user, projectDTO), String.class);
			requestId.put(user.getName(), uuid);
		} catch (Exception e) {
			log.error("Can not create project due to: {}", e.getMessage());
			projectDAO.updateStatus(projectDTO.getName(), ProjectDTO.Status.FAILED);
		}
	}

	private void projectActionOnCloud(UserInfo user, String projectName, String provisioningApiUri, String endpoint) {
		try {
			String uuid = provisioningService.post(provisioningApiUri, user.getAccessToken(),
					requestBuilder.newProjectAction(user, projectName, endpoint), String.class);
			requestId.put(user.getName(), uuid);
		} catch (Exception e) {
			log.error("Can not terminate project due to: {}", e.getMessage());
			projectDAO.updateStatus(projectName, ProjectDTO.Status.FAILED);
		}
	}

	private Supplier<ResourceNotFoundException> projectNotFound() {
		return () -> new ResourceNotFoundException("Project with passed name not found");
	}
}
