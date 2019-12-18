package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.EndpointResourcesDTO;

import java.util.List;

public interface EndpointService {
	List<EndpointDTO> getEndpoints();

	EndpointResourcesDTO getEndpointResources(String endpoint);

	EndpointDTO get(String name);

	void create(EndpointDTO endpointDTO);

	void remove(UserInfo userInfo, String name, boolean withResources);

	void removeEndpointInAllProjects(UserInfo userInfo, String endpointName);
}
