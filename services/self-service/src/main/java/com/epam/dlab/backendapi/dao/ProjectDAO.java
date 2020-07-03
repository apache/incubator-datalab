package com.epam.dlab.backendapi.dao;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProjectDAO {
	List<ProjectDTO> getProjects();

	List<ProjectDTO> getProjectsWithEndpointStatusNotIn(UserInstanceStatus... statuses);

	List<ProjectDTO> getUserProjects(UserInfo userInfo, boolean active);

	void create(ProjectDTO projectDTO);

	void updateStatus(String projectName, ProjectDTO.Status status);

	void updateEdgeStatus(String projectName, String endpoint, UserInstanceStatus status);

	void updateEdgeInfo(String projectName, String endpointName, EdgeInfo edgeInfo);

	Optional<ProjectDTO> get(String name);

	List<ProjectDTO> getProjectsByEndpoint(String endpointName);

	boolean update(ProjectDTO projectDTO);

	void remove(String name);

	Optional<Integer> getAllowedBudget(String project);

	void updateBudget(String project, Integer budget, boolean monthlyBudget);

	boolean isAnyProjectAssigned(Set<String> groups);
}
