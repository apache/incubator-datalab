package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.EndpointDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.UserRoleDao;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.EndpointResourcesDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.cloud.CloudProvider;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class EndpointServiceImpl implements EndpointService {
	private static final String ENDPOINT = "infrastructure/endpoint";
	private static final String HEALTH_CHECK = "infrastructure/endpoint/healthcheck";
	private final EndpointDAO endpointDAO;
	private final ProjectService projectService;
	private final ExploratoryDAO exploratoryDAO;
	private final RESTService provisioningService;
	private final UserRoleDao userRoleDao;

	@Inject
	public EndpointServiceImpl(EndpointDAO endpointDAO, ProjectService projectService, ExploratoryDAO exploratoryDAO,
							   @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
							   UserRoleDao userRoleDao) {

		this.endpointDAO = endpointDAO;
		this.projectService = projectService;
		this.exploratoryDAO = exploratoryDAO;
		this.provisioningService = provisioningService;
		this.userRoleDao = userRoleDao;
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
		if (!endpointDAO.get(endpointDTO.getName()).isPresent()) {
			CloudProvider cloudProvider = connectEndpoint(userInfo, endpointDTO.getUrl(), endpointDTO.getName());

			Optional.ofNullable(cloudProvider)
					.orElseThrow(() -> new DlabException("CloudProvider is not defined for endpoint"));

			endpointDAO.create(new EndpointDTO(endpointDTO.getName(), endpointDTO.getUrl(), endpointDTO.getAccount(),
					endpointDTO.getTag(), EndpointDTO.EndpointStatus.ACTIVE, cloudProvider));
			userRoleDao.updateMissingRoles(cloudProvider);
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
		Optional<EndpointDTO> endpointDTO = endpointDAO.get(name);
		endpointDTO.orElseThrow(() -> new ResourceNotFoundException(String.format("Endpoint %s does not exist", name)));
		List<ProjectDTO> projects = projectService.getProjectsByEndpoint(name);
		checkProjectEndpointResourcesStatuses(projects, name);

		if (withResources) {
			removeEndpointInAllProjects(userInfo, name, projects);
		}
		CloudProvider cloudProvider = endpointDTO.get().getCloudProvider();
		endpointDAO.remove(name);
		List<CloudProvider> remainingProviders = endpointDAO.getEndpoints().stream()
				.map(EndpointDTO::getCloudProvider)
				.collect(Collectors.toList());
		userRoleDao.removeUnnecessaryRoles(cloudProvider, remainingProviders);
	}

	@Override
	public void removeEndpointInAllProjects(UserInfo userInfo, String endpointName, List<ProjectDTO> projects) {
		projects.forEach(project -> projectService.terminateEndpoint(userInfo, endpointName, project.getName()));
	}

	@Override
	public void checkUrl(UserInfo userInfo, String url) {
		Response response;
		try {
			response = provisioningService.get(url + HEALTH_CHECK, userInfo.getAccessToken(), Response.class);
		} catch (Exception e) {
			log.error("Cannot connect to url '{}'", url);
			throw new DlabException(String.format("Cannot connect to url '%s'", url), e);
		}
		if (response.getStatus() != 200) {
			log.warn("Endpoint url {} is not valid", url);
			throw new ResourceNotFoundException(String.format("Endpoint url '%s' is not valid", url));
		}
	}

	private CloudProvider connectEndpoint(UserInfo userInfo, String url, String name) {
		Response response;
		CloudProvider cloudProvider;
		try {
			response = provisioningService.post(url + ENDPOINT, userInfo.getAccessToken(), name, Response.class);
			cloudProvider = response.readEntity(CloudProvider.class);
		} catch (Exception e) {
			log.error("Cannot connect to url '{}'", url);
			throw new DlabException(String.format("Cannot connect to url '%s'", url), e);
		}
		if (response.getStatus() != 200) {
			log.warn("Endpoint connection failed, url {}", url);
			throw new ResourceNotFoundException(String.format("Endpoint url '%s' is not valid", url));
		}
		return cloudProvider;
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
