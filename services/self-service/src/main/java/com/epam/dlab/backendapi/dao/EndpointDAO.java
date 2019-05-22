package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.domain.EndpointDTO;

import java.util.Optional;

public interface EndpointDAO {
	Optional<EndpointDTO> get(String name);

	void create(EndpointDTO endpointDTO);

	void remove(String name);
}
