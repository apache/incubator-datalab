package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.dto.base.project.ProjectEdgeInfo;
import com.google.common.collect.Iterables;
import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

public class ProjectDAOImpl extends BaseDAO implements ProjectDAO {

	private static final String PROJECTS_COLLECTION = "Projects";
	private static final String GROUPS = "groups";
	private static final String STATUS_FIELD = "status";
	private static final String EDGE_INFO_FIELD = "edgeInfo";

	@Override
	public List<ProjectDTO> getProjects() {
		return find(PROJECTS_COLLECTION, ProjectDTO.class);
	}

	@Override
	public void create(ProjectDTO projectDTO) {
		insertOne(PROJECTS_COLLECTION, projectDTO);
	}

	@Override
	public void updateStatus(String projectName, ProjectDTO.Status status) {
		updateOne(PROJECTS_COLLECTION, projectCondition(projectName),
				new Document(SET, new Document(STATUS_FIELD, status.toString())));
	}

	@Override
	public void updateEdgeInfoAndStatus(String projectName, ProjectEdgeInfo edgeInfo, ProjectDTO.Status status) {
		BasicDBObject dbObject = new BasicDBObject();
		dbObject.put(STATUS_FIELD, status.toString());
		dbObject.put(EDGE_INFO_FIELD, convertToBson(edgeInfo));
		final UpdateResult updateResult = updateOne(PROJECTS_COLLECTION, projectCondition(projectName),
				new Document(SET, dbObject));
		System.out.println(updateResult);
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
		deleteOne(PROJECTS_COLLECTION, projectCondition(name));
	}

	@Override
	public Optional<Integer> getAllowedBudget(String project) {
		return get(project).map(ProjectDTO::getBudget);
	}

	@Override
	public void updateBudget(String project, Integer budget) {
		updateOne(PROJECTS_COLLECTION, projectCondition(project), new Document(SET, new Document("budget", budget)));
	}

	@Override
	public boolean isAnyProjectAssigned(Set<String> groups) {
		return !Iterables.isEmpty(find(PROJECTS_COLLECTION, in(GROUPS, groups)));
	}

	private Bson projectCondition(String name) {
		return eq("name", name);
	}
}
