package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.UpdateProjectDTO;

import java.util.List;

public interface ProjectService {
	List<ProjectDTO> getProjects();

	List<ProjectDTO> getUserActiveProjects(UserInfo userInfo);

	List<ProjectDTO> getProjectsWithStatus(ProjectDTO.Status status);

	void create(UserInfo userInfo, ProjectDTO projectDTO);

	ProjectDTO get(String name);

	void terminateEndpoint(UserInfo userInfo, String endpoint, String name);

	void terminateProject(UserInfo userInfo, String name);

	void start(UserInfo userInfo, String endpoint, String name);

	void stop(UserInfo userInfo, String endpoint, String name);

	void update(UserInfo userInfo, UpdateProjectDTO projectDTO);

	void updateBudget(String project, Integer budget);

	void updateBudget(List<ProjectDTO> projects);

	boolean isAnyProjectAssigned(UserInfo userInfo);
}
