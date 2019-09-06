package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.project.ProjectActionDTO;
import com.epam.dlab.dto.project.ProjectCreateDTO;

public interface ProjectService {

	String create(UserInfo userInfo, ProjectCreateDTO projectCreateDTO);

	String terminate(UserInfo userInfo, ProjectActionDTO dto);

	String start(UserInfo userInfo, ProjectActionDTO dto);

	String stop(UserInfo userInfo, ProjectActionDTO dto);
}
