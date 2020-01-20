package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EndpointDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.EndpointResourcesDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class EndpointServiceImpl implements EndpointService {
	private static final String HEALTHCHECK = "healthcheck";
	private final EndpointDAO endpointDAO;
	private final ProjectService projectService;
	private final ExploratoryDAO exploratoryDAO;
	private final RESTService provisioningService;

	@Inject
	public EndpointServiceImpl(EndpointDAO endpointDAO, ProjectService projectService, ExploratoryDAO exploratoryDAO,
							   @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService) {

		this.endpointDAO = endpointDAO;
		this.projectService = projectService;
		this.exploratoryDAO = exploratoryDAO;
		this.provisioningService = provisioningService;
	}

	@Override
	public List<EndpointDTO> getEndpoints() {
		return endpointDAO.getEndpoints();
	}

	@Override
	public List<EndpointDTO> getEndpointsWithStatus(EndpointDTO.EndpointStatus status) {
		return endpointDAO.getEndpointsWithStatus(status.name());
	}

	@Override
	public EndpointResourcesDTO getEndpointResources(String endpoint) {
		List<UserInstanceDTO> exploratories = exploratoryDAO.fetchExploratoriesByEndpointWhereStatusNotIn(endpoint,
				Arrays.asList(UserInstanceStatus.TERMINATED, UserInstanceStatus.FAILED));

		List<ProjectDTO> projects = projectService.getProjectsByEndpoint(endpoint);

		return new EndpointResourcesDTO(exploratories, projects);
	}

	@Override
	public EndpointDTO get(String name) {
		return endpointDAO.get(name)
				.orElseThrow(() -> new ResourceNotFoundException("Endpoint with name " + name + " not found"));
	}

	@Override
	public void create(UserInfo userInfo, EndpointDTO endpointDTO) {
		checkEndpointUrl(userInfo, endpointDTO.getUrl());
		if (!endpointDAO.get(endpointDTO.getName()).isPresent()) {
			endpointDAO.create(EndpointDTO.withEndpointStatus(endpointDTO));
		} else {
			throw new ResourceConflictException("Endpoint with passed name already exist in system");
		}
	}

	@Override
	public void updateEndpointStatus(String name, EndpointDTO.EndpointStatus status) {
		endpointDAO.updateEndpointStatus(name, status.name());
	}

	@Override
	public void remove(UserInfo userInfo, String name, boolean withResources) {
		List<ProjectDTO> projects = projectService.getProjectsByEndpoint(name);
		checkProjectEndpointResourcesStatuses(projects, name);

		if (withResources) {
			removeEndpointInAllProjects(userInfo, name, projects);
		}
		endpointDAO.remove(name);
	}

	@Override
	public void removeEndpointInAllProjects(UserInfo userInfo, String endpointName, List<ProjectDTO> projects) {
		projects.forEach(project -> projectService.terminateEndpoint(userInfo, endpointName, project.getName()));
	}

	@Override
	public void checkEndpointUrl(UserInfo userInfo, String url) {
		Response response;
		try {
			response = provisioningService.get(url + HEALTHCHECK, userInfo.getAccessToken(), Response.class);
		} catch (Exception e) {
			log.error("Cannot connect to url \'{}\'", url);
			throw new DlabException(String.format("Cannot connect to url \'%s\'", url), e);
		}
		if (response.getStatus() != 200) {
			log.warn("Endpoint url {} is not valid", url);
			throw new ResourceNotFoundException(String.format("Endpoint url \'%s\' is not valid", url));
		}
	}

	private void checkProjectEndpointResourcesStatuses(List<ProjectDTO> projects, String endpoint) {
		boolean isTerminationEnabled = projects.stream().anyMatch(p ->
				!projectService.checkExploratoriesAndComputationalProgress(p.getName(), Collections.singletonList(endpoint)) ||
						p.getEndpoints().stream().anyMatch(e -> e.getName().equals(endpoint) &&
								Arrays.asList(UserInstanceStatus.CREATING, UserInstanceStatus.STARTING, UserInstanceStatus.STOPPING,
										UserInstanceStatus.TERMINATING).contains(e.getStatus())));

		if (isTerminationEnabled) {
			throw new ResourceConflictException(("Can not terminate resources of endpoint because one of project " +
					"resource is in processing stage"));
		}
	}
}
