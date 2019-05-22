package com.epam.dlab.backendapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectDTO {
	private final String name;
	private final Set<String> endpoints;
	private final Set<String> groups;
}
