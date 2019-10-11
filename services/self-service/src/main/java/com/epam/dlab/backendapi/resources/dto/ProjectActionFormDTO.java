package com.epam.dlab.backendapi.resources.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProjectActionFormDTO {
	@JsonProperty("project_name")
	private final String projectName;
	@JsonProperty("endpoint")
	private final String endpoint;
}
