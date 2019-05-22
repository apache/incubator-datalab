package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.domain.ProjectDTO;

public interface ProjectService {
	void create(ProjectDTO projectDTO);

	ProjectDTO get(String name);

	void remove(String name);

	void update(ProjectDTO projectDTO);
}
