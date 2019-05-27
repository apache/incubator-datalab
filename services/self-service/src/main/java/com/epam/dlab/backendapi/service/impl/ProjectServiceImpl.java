package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.google.inject.Inject;

import java.util.List;
import java.util.function.Supplier;

public class ProjectServiceImpl implements ProjectService {

	private final ProjectDAO projectDAO;
	private final EnvironmentService environmentService;

	@Inject
	public ProjectServiceImpl(ProjectDAO projectDAO, EnvironmentService environmentService) {
		this.projectDAO = projectDAO;
		this.environmentService = environmentService;
	}

	@Override
	public List<ProjectDTO> getProjects() {
		return projectDAO.getProjects();
	}

	@Override
	public void create(ProjectDTO projectDTO) {
		if (!projectDAO.get(projectDTO.getName()).isPresent()) {
			projectDAO.create(projectDTO);
		} else {
			throw new ResourceConflictException("Project with passed name already exist in system");
		}
	}

	@Override
	public ProjectDTO get(String name) {
		return projectDAO.get(name)
				.orElseThrow(projectNotFound());
	}

	@Override
	public void remove(String name) {
		environmentService.terminateProjectEnvironment(name);
		projectDAO.remove(name);
	}

	@Override
	public void update(ProjectDTO projectDTO) {
		if (!projectDAO.update(projectDTO)) {
			throw projectNotFound().get();
		}
	}

	@Override
	public void updateBudget(String project, Integer budget) {
		projectDAO.updateBudget(project, budget);
	}

	private Supplier<ResourceNotFoundException> projectNotFound() {
		return () -> new ResourceNotFoundException("Project with passed name not found");
	}
}
