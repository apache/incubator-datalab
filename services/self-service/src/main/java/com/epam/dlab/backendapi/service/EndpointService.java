package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.domain.EndpointDTO;

import java.util.List;

public interface EndpointService {
	List<EndpointDTO> getEndpoints();
	EndpointDTO get(String name);

	void create(EndpointDTO endpointDTO);

	void remove(String name);
}
