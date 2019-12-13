package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EndpointDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.google.inject.Inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EndpointServiceImpl implements EndpointService {
	private final EndpointDAO endpointDAO;
	private final ProjectService projectService;

	@Inject
	public EndpointServiceImpl(EndpointDAO endpointDAO, ProjectService projectService) {
		this.endpointDAO = endpointDAO;
		this.projectService = projectService;
	}

	@Override
	public List<EndpointDTO> getEndpoints() {
		return endpointDAO.getEndpoints();
	}

	@Override
	public EndpointDTO get(String name) {
		return endpointDAO.get(name)
				.orElseThrow(() -> new ResourceNotFoundException("Endpoint with name " + name + " not found"));
	}

	@Override
	public void create(EndpointDTO endpointDTO) {
		if (!endpointDAO.get(endpointDTO.getName()).isPresent()) {
			endpointDAO.create(endpointDTO);
		} else {
			throw new ResourceConflictException("Endpoint with passed name already exist in system");
		}
	}

	@Override
	public void remove(UserInfo userInfo, String name, boolean withResources) {
		if (withResources) {
			removeEndpointInAllProjects(userInfo, name);
		}
		endpointDAO.remove(name);
	}

	@Override
	public void removeEndpointInAllProjects(UserInfo userInfo, String endpointName) {
		List<ProjectDTO> projects = projectService.getProjectsByEndpoint(endpointName);
		checkProjectEndpointResourcesStatuses(projects, endpointName);

		projects.forEach(project -> projectService.terminateEndpoint(userInfo, endpointName, project.getName()));
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
