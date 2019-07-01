package com.epam.dlab.backendapi.dao;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.dto.base.project.ProjectEdgeInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProjectDAO {
	List<ProjectDTO> getProjects();

	List<ProjectDTO> getUserProjectsWithStatus(UserInfo userInfo, ProjectDTO.Status status);

	void create(ProjectDTO projectDTO);

	void updateStatus(String projectName, ProjectDTO.Status status);

	void updateEdgeInfoAndStatus(String projectName, ProjectEdgeInfo edgeInfo, ProjectDTO.Status status);

	Optional<ProjectDTO> get(String name);

	boolean update(ProjectDTO projectDTO);

	void remove(String name);

	Optional<Integer> getAllowedBudget(String project);

	void updateBudget(String project, Integer budget);

	boolean isAnyProjectAssigned(Set<String> groups);
}
