package com.epam.dlab.backendapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectDTO {
	@NotNull
	private final String name;
	@NotNull
	private final Set<String> endpoints;
	@NotNull
	private final Set<String> groups;
	private final Integer budget;
}
