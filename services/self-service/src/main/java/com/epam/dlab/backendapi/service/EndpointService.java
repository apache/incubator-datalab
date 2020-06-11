package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.EndpointResourcesDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.cloud.CloudProvider;

import java.util.List;

public interface EndpointService {
	List<EndpointDTO> getEndpoints();

	List<EndpointDTO> getEndpointsWithStatus(EndpointDTO.EndpointStatus status);

	EndpointResourcesDTO getEndpointResources(String endpoint);

	EndpointDTO get(String name);

	void create(UserInfo userInfo, String resourceName, EndpointDTO endpointDTO);

	void updateEndpointStatus(String name, EndpointDTO.EndpointStatus status);

	void remove(UserInfo userInfo, String name);

	void removeEndpointInAllProjects(UserInfo userInfo, String endpointName, List<ProjectDTO> projects);

	CloudProvider checkUrl(UserInfo userInfo, String url);
}
