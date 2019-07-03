package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.ProjectDTO;

import java.util.List;

public interface ProjectService {
	List<ProjectDTO> getProjects();

	List<ProjectDTO> getUserProjects(UserInfo userInfo);

	void create(UserInfo userInfo, ProjectDTO projectDTO);

	ProjectDTO get(String name);

	void terminate(UserInfo userInfo, String name);
	void start(UserInfo userInfo, String name);
	void stop(UserInfo userInfo, String name);

	void update(ProjectDTO projectDTO);

	void updateBudget(String project, Integer budget);

	boolean isAnyProjectAssigned(UserInfo userInfo);
}
