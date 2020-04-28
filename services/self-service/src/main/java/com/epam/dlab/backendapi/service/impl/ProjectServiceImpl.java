package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.annotation.BudgetLimited;
import com.epam.dlab.backendapi.annotation.Project;
import com.epam.dlab.backendapi.annotation.ProjectAdmin;
import com.epam.dlab.backendapi.annotation.User;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.dao.UserGroupDao;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.domain.UpdateProjectDTO;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
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
	private static final String STOP_ACTION = "stop";
	private static final String TERMINATE_ACTION = "terminate";

	private final ProjectDAO projectDAO;
	private final ExploratoryService exploratoryService;
	private final UserGroupDao userGroupDao;
	private final RESTService provisioningService;
	private final RequestId requestId;
	private final RequestBuilder requestBuilder;
	private final EndpointService endpointService;
	private final ExploratoryDAO exploratoryDAO;
	private final SecurityService securityService;

	@Inject
	public ProjectServiceImpl(ProjectDAO projectDAO, ExploratoryService exploratoryService,
							  UserGroupDao userGroupDao,
							  @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
							  RequestId requestId, RequestBuilder requestBuilder, EndpointService endpointService,
							  ExploratoryDAO exploratoryDAO, SecurityService securityService) {
		this.projectDAO = projectDAO;
		this.exploratoryService = exploratoryService;
		this.userGroupDao = userGroupDao;
		this.provisioningService = provisioningService;
		this.requestId = requestId;
		this.requestBuilder = requestBuilder;
		this.endpointService = endpointService;
		this.exploratoryDAO = exploratoryDAO;
		this.securityService = securityService;
	}

	@Override
	public List<ProjectDTO> getProjects() {
		return projectDAO.getProjects();
	}

	@Override
	public List<ProjectDTO> getProjects(UserInfo user) {
		return projectDAO.getProjects()
				.stream()
				.filter(project -> UserRoles.isProjectAdmin(user, project.getGroups()) || UserRoles.isAdmin(user))
				.collect(Collectors.toList());
	}

	@Override
	public List<ProjectDTO> getUserProjects(UserInfo userInfo, boolean active) {
		return projectDAO.getUserProjects(userInfo, active);
	}

	@Override
	public List<ProjectDTO> getProjectsByEndpoint(String endpointName) {
		return projectDAO.getProjectsByEndpoint(endpointName);
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
	public void terminateEndpoint(UserInfo userInfo, String endpoint, String name) {
		projectActionOnCloud(userInfo, name, TERMINATE_PRJ_API, endpoint);
		projectDAO.updateEdgeStatus(name, endpoint, UserInstanceStatus.TERMINATING);
		exploratoryService.updateProjectExploratoryStatuses(name, endpoint, UserInstanceStatus.TERMINATING);
	}

	@ProjectAdmin
	@Override
	public void terminateEndpoint(@User UserInfo userInfo, List<String> endpoints, @Project String name) {
		System.out.println("sd");
		endpoints.forEach(endpoint -> terminateEndpoint(userInfo, endpoint, name));
	}

	@BudgetLimited
	@Override
	public void start(UserInfo userInfo, String endpoint, @Project String name) {
		projectActionOnCloud(userInfo, name, START_PRJ_API, endpoint);
		projectDAO.updateEdgeStatus(name, endpoint, UserInstanceStatus.STARTING);
	}

	@ProjectAdmin
	@Override
	public void start(@User UserInfo userInfo, List<String> endpoints, @Project String name) {
		endpoints.forEach(endpoint -> start(userInfo, endpoint, name));
	}

	@Override
	public void stop(UserInfo userInfo, String endpoint, String name) {
		projectActionOnCloud(userInfo, name, STOP_PRJ_API, endpoint);
		projectDAO.updateEdgeStatus(name, endpoint, UserInstanceStatus.STOPPING);
	}

	@ProjectAdmin
	@Override
	public void stopWithResources(@User UserInfo userInfo, List<String> endpoints, @Project String projectName) {
		List<ProjectEndpointDTO> endpointDTOs = get(projectName)
				.getEndpoints()
				.stream()
				.filter(projectEndpointDTO -> endpoints.contains(projectEndpointDTO.getName()))
				.collect(Collectors.toList());
		checkProjectRelatedResourcesInProgress(projectName, endpointDTOs, STOP_ACTION);

		exploratoryDAO.fetchRunningExploratoryFieldsForProject(projectName,
				endpointDTOs
						.stream()
						.map(ProjectEndpointDTO::getName)
						.collect(Collectors.toList()))
				.forEach(e -> exploratoryService.stop(new UserInfo(e.getUser(), userInfo.getAccessToken()), projectName, e.getExploratoryName()));

		endpointDTOs.stream().filter(e -> !Arrays.asList(UserInstanceStatus.TERMINATED,
				UserInstanceStatus.TERMINATING, UserInstanceStatus.STOPPED, UserInstanceStatus.FAILED).contains(e.getStatus()))
				.forEach(e -> stop(userInfo, e.getName(), projectName));
	}

	@ProjectAdmin
	@Override
	public void update(@User UserInfo userInfo, UpdateProjectDTO projectDTO, @Project String projectName) {
		final ProjectDTO project = projectDAO.get(projectDTO.getName()).orElseThrow(projectNotFound());
		final Set<String> endpoints = project.getEndpoints()
				.stream()
				.map(ProjectEndpointDTO::getName)
				.collect(toSet());
		final HashSet<String> newEndpoints = new HashSet<>(projectDTO.getEndpoints());
		newEndpoints.removeAll(endpoints);
		final List<ProjectEndpointDTO> endpointsToBeCreated = newEndpoints.stream()
				.map(e -> new ProjectEndpointDTO(e, UserInstanceStatus.CREATING, null))
				.collect(Collectors.toList());
		project.getEndpoints().addAll(endpointsToBeCreated);
		projectDAO.update(new ProjectDTO(project.getName(), projectDTO.getGroups(), project.getKey(),
				project.getTag(), project.getBudget(), project.getEndpoints(), projectDTO.isSharedImageEnabled()));
		endpointsToBeCreated.forEach(e -> createEndpoint(userInfo, project, e.getName()));
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

	@Override
	public boolean checkExploratoriesAndComputationalProgress(String projectName, List<String> endpoints) {
		return exploratoryDAO.fetchProjectEndpointExploratoriesWhereStatusIn(projectName, endpoints, Arrays.asList(
				UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.CREATING_IMAGE,
				UserInstanceStatus.CONFIGURING, UserInstanceStatus.RECONFIGURING, UserInstanceStatus.STOPPING,
				UserInstanceStatus.TERMINATING),
				UserInstanceStatus.CREATING, UserInstanceStatus.CONFIGURING, UserInstanceStatus.STARTING,
				UserInstanceStatus.RECONFIGURING, UserInstanceStatus.CREATING_IMAGE, UserInstanceStatus.STOPPING,
				UserInstanceStatus.TERMINATING).isEmpty();
	}

	private void createProjectOnCloud(UserInfo user, ProjectDTO projectDTO) {
		try {
			projectDTO.getEndpoints().forEach(endpoint -> createEndpoint(user, projectDTO,
					endpoint.getName()));
		} catch (Exception e) {
			log.error("Can not create project due to: {}", e.getMessage());
			projectDAO.updateStatus(projectDTO.getName(), ProjectDTO.Status.FAILED);
		}
	}

	private void createEndpoint(UserInfo user, ProjectDTO projectDTO, String endpointName) {
		EndpointDTO endpointDTO = endpointService.get(endpointName);
		String uuid = provisioningService.post(endpointDTO.getUrl() + CREATE_PRJ_API, user.getAccessToken(),
						requestBuilder.newProjectCreate(user, projectDTO, endpointDTO), String.class);
		requestId.put(user.getName(), uuid);
	}

	private void projectActionOnCloud(UserInfo user, String projectName, String provisioningApiUri, String endpoint) {
		try {
			EndpointDTO endpointDTO = endpointService.get(endpoint);
			String uuid = provisioningService.post(endpointDTO.getUrl() + provisioningApiUri, user.getAccessToken(),
					requestBuilder.newProjectAction(user, projectName, endpointDTO), String.class);
			requestId.put(user.getName(), uuid);
		} catch (Exception e) {
			log.error("Can not terminate project due to: {}", e.getMessage());
			projectDAO.updateStatus(projectName, ProjectDTO.Status.FAILED);
		}
	}

	private void checkProjectRelatedResourcesInProgress(String projectName, List<ProjectEndpointDTO> endpoints, String action) {
        boolean edgeProgress = endpoints.stream().anyMatch(e ->
                Arrays.asList(UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.STOPPING,
                        UserInstanceStatus.TERMINATING).contains(e.getStatus()));

		List<String> endpointsName = endpoints.stream().map(ProjectEndpointDTO::getName).collect(Collectors.toList());
		if (edgeProgress || !checkExploratoriesAndComputationalProgress(projectName, endpointsName)) {
			throw new ResourceConflictException((String.format("Can not %s environment because one of project " +
					"resource is in processing stage", action)));
		}
	}

	private Supplier<ResourceNotFoundException> projectNotFound() {
		return () -> new ResourceNotFoundException("Project with passed name not found");
	}
}
