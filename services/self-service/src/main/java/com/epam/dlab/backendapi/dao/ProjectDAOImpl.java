package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.domain.ProjectDTO;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class ProjectDAOImpl extends BaseDAO implements ProjectDAO {

	private static final String PROJECTS_COLLECTION = "Projects";

	@Override
	public void create(ProjectDTO projectDTO) {
		insertOne(PROJECTS_COLLECTION, projectDTO);
	}

	@Override
	public Optional<ProjectDTO> get(String name) {
		return findOne(PROJECTS_COLLECTION, projectCondition(name), ProjectDTO.class);
	}

	@Override
	public boolean update(ProjectDTO projectDTO) {
		return updateOne(PROJECTS_COLLECTION, projectCondition(projectDTO.getName()),
				new Document(SET, convertToBson(projectDTO))).getMatchedCount() > 0L;
	}

	@Override
	public void remove(String name) {

	}

	private Bson projectCondition(String name) {
		return eq("name", name);
	}
}
