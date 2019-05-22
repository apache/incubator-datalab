package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.domain.EndpointDTO;
import org.bson.conversions.Bson;

import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class EndpointDAOImpl extends BaseDAO implements EndpointDAO {

	private static final String ENDPOINTS_COLLECTION = "endpoints";

	@Override
	public Optional<EndpointDTO> get(String name) {
		return findOne(ENDPOINTS_COLLECTION, endpointCondition(name), EndpointDTO.class);
	}

	@Override
	public void create(EndpointDTO endpointDTO) {
		insertOne(ENDPOINTS_COLLECTION, endpointDTO);
	}

	@Override
	public void remove(String name) {
		deleteOne(ENDPOINTS_COLLECTION, endpointCondition(name));
	}

	private Bson endpointCondition(String name) {
		return eq("name", name);
	}
}
