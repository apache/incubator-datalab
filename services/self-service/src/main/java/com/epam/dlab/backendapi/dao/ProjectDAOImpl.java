package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.domain.ProjectDTO;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class ProjectDAOImpl extends BaseDAO implements ProjectDAO {

	private static final String PROJECTS_COLLECTION = "Projects";

	@Override
	public List<ProjectDTO> getProjects() {
		return find(PROJECTS_COLLECTION, ProjectDTO.class);
	}

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

	@Override
	public Optional<Integer> getAllowedBudget(String project) {
		return get(project).map(ProjectDTO::getBudget);
	}

	@Override
	public void updateBudget(String project, Integer budget) {
		updateOne(PROJECTS_COLLECTION, projectCondition(project), new Document(SET, new Document("budget", budget)));
	}

	private Bson projectCondition(String name) {
		return eq("name", name);
	}
}
