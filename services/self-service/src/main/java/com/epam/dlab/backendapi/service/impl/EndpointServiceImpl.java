package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.dao.EndpointDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.google.inject.Inject;

import java.util.List;

public class EndpointServiceImpl implements EndpointService {
	private final EndpointDAO endpointDAO;

	@Inject
	public EndpointServiceImpl(EndpointDAO endpointDAO) {
		this.endpointDAO = endpointDAO;
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
	public void remove(String name) {
		endpointDAO.remove(name);
	}
}
