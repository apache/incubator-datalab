package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.UpdateProjectBudgetDTO;
import com.epam.dlab.backendapi.domain.UpdateProjectDTO;

import java.util.List;

public interface ProjectService {
	List<ProjectDTO> getProjects();

	List<ProjectDTO> getProjects(UserInfo user);

	List<ProjectDTO> getUserProjects(UserInfo userInfo, boolean active);

	List<ProjectDTO> getProjectsByEndpoint(String endpointName);

	void create(UserInfo userInfo, ProjectDTO projectDTO, String resourceName);

    ProjectDTO get(String name);

    void terminateEndpoint(UserInfo userInfo, String endpoint, String name);

    void terminateEndpoint(UserInfo userInfo, List<String> endpoints, String name);

    void start(UserInfo userInfo, String endpoint, String name);

    void start(UserInfo userInfo, List<String> endpoints, String name);

    void stop(UserInfo userInfo, String endpoint, String name, List<String> auditInfo);

    void stopWithResources(UserInfo userInfo, List<String> endpoints, String projectName);

    void update(UserInfo userInfo, UpdateProjectDTO projectDTO, String projectName);

    void updateBudget(UserInfo userInfo, List<UpdateProjectBudgetDTO> projects);

    boolean isAnyProjectAssigned(UserInfo userInfo);

    boolean checkExploratoriesAndComputationalProgress(String projectName, List<String> endpoints);
}
