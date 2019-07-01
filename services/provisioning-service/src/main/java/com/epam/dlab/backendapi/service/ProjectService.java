package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.project.ProjectCreateDTO;
import com.epam.dlab.dto.project.ProjectTerminateDTO;

public interface ProjectService {

	String create(UserInfo userInfo, ProjectCreateDTO projectCreateDTO);
	String terminate(UserInfo userInfo, ProjectTerminateDTO dto);
}
