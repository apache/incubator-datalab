package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.EndpointDTO;

import java.util.List;

public interface EndpointService {
	List<EndpointDTO> getEndpoints();

	EndpointDTO get(String name);

	void create(EndpointDTO endpointDTO);

	void remove(UserInfo userInfo, String name, boolean withResources);

	void removeEndpointInAllProjects(UserInfo userInfo, String endpointName);
}
