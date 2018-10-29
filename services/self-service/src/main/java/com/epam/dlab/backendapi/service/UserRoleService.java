package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.resources.dto.UserRoleDto;

import java.util.List;

public interface UserRoleService {

	List<UserRoleDto> getUserRoles();

	void createRole(UserRoleDto dto);

	void updateRole(UserRoleDto dto);

	void removeRole(String roleId);
}
