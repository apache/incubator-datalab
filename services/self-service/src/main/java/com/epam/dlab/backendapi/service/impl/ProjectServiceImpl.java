package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.dao.UserGroupDao;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.domain.UpdateProjectDTO;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.project.ProjectActionDTO;
import com.epam.dlab.dto.project.ProjectCreateDTO;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;

@Slf4j
public class ProjectServiceImpl implements ProjectService {

	private static final String CREATE_PRJ_API = "infrastructure/project/create";
	private static final String TERMINATE_PRJ_API = "infrastructure/project/terminate";
	private static final String START_PRJ_API = "infrastructure/project/start";
	private static final String STOP_PRJ_API = "infrastructure/project/stop";
	private final ProjectDAO projectDAO;
	private final EnvironmentService environmentService;
	private final UserGroupDao userGroupDao;
	private final RESTService provisioningService;
	private final RequestId requestId;

	@Inject
	public ProjectServiceImpl(ProjectDAO projectDAO, EnvironmentService environmentService,
							  UserGroupDao userGroupDao,
							  @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
							  RequestId requestId) {
		this.projectDAO = projectDAO;
		this.environmentService = environmentService;
		this.userGroupDao = userGroupDao;
		this.provisioningService = provisioningService;
		this.requestId = requestId;
	}

	@Override
	public List<ProjectDTO> getProjects() {
		return projectDAO.getProjects();
	}

	@Override
	public List<ProjectDTO> getUserProjects(UserInfo userInfo) {
		return projectDAO.getUserProjectsWithStatus(userInfo, ProjectDTO.Status.ACTIVE);
	}

	@Override
	public List<ProjectDTO> getProjectsWithStatus(ProjectDTO.Status status) {
		return projectDAO.getProjectsWithStatus(status);
	}

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
		projectActionOnCloud(userInfo, name, TERMINATE_PRJ_API);
		environmentService.terminateProjectEnvironment(name);
		projectDAO.updateStatus(name, ProjectDTO.Status.DELETING);
	}

	@Override
	public void start(UserInfo userInfo, String name) {
		projectActionOnCloud(userInfo, name, START_PRJ_API);
		projectDAO.updateStatus(name, ProjectDTO.Status.ACTIVATING);
	}

	@Override
	public void stop(UserInfo userInfo, String name) {
		projectActionOnCloud(userInfo, name, STOP_PRJ_API);
		projectDAO.updateStatus(name, ProjectDTO.Status.DEACTIVATING);
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
	public boolean isAnyProjectAssigned(UserInfo userInfo) {
		final Set<String> userGroups = concat(userInfo.getRoles().stream(),
				userGroupDao.getUserGroups(userInfo.getName()).stream())
				.collect(toSet());
		return projectDAO.isAnyProjectAssigned(userGroups);
	}

	private void createProjectOnCloud(UserInfo user, ProjectDTO projectDTO) {
		try {
			final ProjectCreateDTO projectDto = ProjectCreateDTO.builder()
					.key(projectDTO.getKey())
					.name(projectDTO.getName())
					.tag(projectDTO.getTag())
					.build();
			String uuid = provisioningService.post(CREATE_PRJ_API, user.getAccessToken(), projectDto, String.class);
			requestId.put(user.getName(), uuid);
		} catch (Exception e) {
			log.error("Can not create project due to: {}", e.getMessage());
			projectDAO.updateStatus(projectDTO.getName(), ProjectDTO.Status.FAILED);
		}
	}


	private void projectActionOnCloud(UserInfo user, String projectName, String provisioningApiUri) {
		try {
			String uuid = provisioningService.post(provisioningApiUri, user.getAccessToken(),
					new ProjectActionDTO(projectName), String.class);
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
