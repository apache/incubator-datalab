package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.domain.ProjectDTO;

import java.util.List;
import java.util.Optional;

public interface ProjectDAO {
	List<ProjectDTO> getProjects();

	void create(ProjectDTO projectDTO);

	Optional<ProjectDTO> get(String name);

	boolean update(ProjectDTO projectDTO);

	void remove(String name);

	Optional<Integer> getAllowedBudget(String project);

	void updateBudget(String project, Integer budget);
}
