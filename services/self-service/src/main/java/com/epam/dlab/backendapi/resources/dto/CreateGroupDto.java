package com.epam.dlab.backendapi.resources.dto;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Collections;
import java.util.Set;

@Getter
@Setter
public class CreateGroupDto {
	@NotEmpty
	private String name;
	@NotEmpty
	private Set<String> roleIds;
	private Set<String> users = Collections.emptySet();
}
