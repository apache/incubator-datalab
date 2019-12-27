package com.epam.dlab.backendapi.resources.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ProjectActionFormDTO {
	@NotNull
	@JsonProperty("project_name")
	private final String projectName;
	@NotNull
	@JsonProperty("endpoint")
	private final List<String> endpoints;
}
