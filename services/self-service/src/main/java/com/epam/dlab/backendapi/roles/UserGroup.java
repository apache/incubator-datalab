package com.epam.dlab.backendapi.roles;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@AllArgsConstructor
@Getter
public class UserGroup {
	private final String groupName;
	private final Set<String> users;
}
