package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.domain.ProjectDTO;

import java.util.Optional;

public interface ProjectDAO {
	void create(ProjectDTO projectDTO);

	Optional<ProjectDTO> get(String name);

	boolean update(ProjectDTO projectDTO);

	void remove(String name);
}
