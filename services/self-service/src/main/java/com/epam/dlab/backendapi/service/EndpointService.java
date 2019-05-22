package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.domain.EndpointDTO;

public interface EndpointService {
	EndpointDTO get(String name);

	void create(EndpointDTO endpointDTO);

	void remove(String name);
}
