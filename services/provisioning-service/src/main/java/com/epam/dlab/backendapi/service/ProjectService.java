package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.ProjectCreateDTO;

public interface ProjectService {

	String create(UserInfo userInfo, ProjectCreateDTO projectCreateDTO);
}
