package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.annotation.Audit;
import com.epam.dlab.backendapi.annotation.ResourceName;
import com.epam.dlab.backendapi.annotation.User;
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
import java.util.Objects;
import java.util.stream.Collectors;

import static com.epam.dlab.backendapi.domain.AuditActionEnum.CREATE;
import static com.epam.dlab.backendapi.domain.AuditActionEnum.DELETE;
import static com.epam.dlab.backendapi.domain.AuditResourceTypeEnum.ENDPOINT;


@Slf4j
public class EndpointServiceImpl implements EndpointService {
	private static final String HEALTH_CHECK = "healthcheck";
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

    /**
     * Create new endpoint object in the System.
     * The Endpoint objects should contain Unique values of the 'url' and 'name' fields,
     * i.e two objects with same URLs should not be created in the system.
     *
     * @param userInfo     user properties
     * @param resourceName name of the endpoint
     * @param endpointDTO  object with endpoint fields
     */
    @Audit(action = CREATE, type = ENDPOINT)
    @Override
    public void create(@User UserInfo userInfo, @ResourceName String resourceName, EndpointDTO endpointDTO) {
        if (endpointDAO.get(endpointDTO.getName()).isPresent()) {
            throw new ResourceConflictException("The Endpoint with this name exists in system");
        }
        if (endpointDAO.getEndpointWithUrl(endpointDTO.getUrl()).isPresent()) {
            throw new ResourceConflictException("The Endpoint URL with this address exists in system");
        }
        CloudProvider cloudProvider = checkUrl(userInfo, endpointDTO.getUrl());
        if (Objects.isNull(cloudProvider)) {
			throw new DlabException("CloudProvider cannot be null");
		}
		endpointDAO.create(new EndpointDTO(endpointDTO.getName(), endpointDTO.getUrl(), endpointDTO.getAccount(),
				endpointDTO.getTag(), EndpointDTO.EndpointStatus.ACTIVE, cloudProvider));
		userRoleDao.updateMissingRoles(cloudProvider);
	}

	@Override
	public void updateEndpointStatus(String name, EndpointDTO.EndpointStatus status) {
		endpointDAO.updateEndpointStatus(name, status.name());
    }

    @Override
    public void remove(UserInfo userInfo, String name) {
        EndpointDTO endpointDTO = endpointDAO.get(name).orElseThrow(() -> new ResourceNotFoundException(String.format("Endpoint %s does not exist", name)));
        List<ProjectDTO> projects = projectService.getProjectsByEndpoint(name);
        checkProjectEndpointResourcesStatuses(projects, name);
        CloudProvider cloudProvider = endpointDTO.getCloudProvider();
        removeEndpoint(userInfo, name, cloudProvider, projects);
    }

    @Audit(action = DELETE, type = ENDPOINT)
    public void removeEndpoint(@User UserInfo userInfo, @ResourceName String name, CloudProvider cloudProvider, List<ProjectDTO> projects) {
        removeEndpointInAllProjects(userInfo, name, projects);
        endpointDAO.remove(name);
        List<CloudProvider> remainingProviders = endpointDAO.getEndpoints()
                .stream()
                .map(EndpointDTO::getCloudProvider)
                .collect(Collectors.toList());
        userRoleDao.removeUnnecessaryRoles(cloudProvider, remainingProviders);
	}

	@Override
	public void removeEndpointInAllProjects(UserInfo userInfo, String endpointName, List<ProjectDTO> projects) {
		projects.forEach(project -> projectService.terminateEndpoint(userInfo, endpointName, project.getName()));
	}

	@Override
	public CloudProvider checkUrl(UserInfo userInfo, String url) {
		Response response;
		CloudProvider cloudProvider;
		try {
			response = provisioningService.get(url + HEALTH_CHECK, userInfo.getAccessToken(), Response.class);
			cloudProvider = response.readEntity(CloudProvider.class);
		} catch (Exception e) {
            log.error("Cannot connect to url '{}'. {}", url, e.getMessage());
            throw new DlabException(String.format("Cannot connect to url '%s'.", url));
        }
		if (response.getStatus() != 200) {
			log.warn("Endpoint url {} is not valid", url);
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
