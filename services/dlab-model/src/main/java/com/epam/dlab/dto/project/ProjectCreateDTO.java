package com.epam.dlab.dto.project;

import com.epam.dlab.dto.ResourceBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectCreateDTO extends ResourceBaseDTO<ProjectCreateDTO> {
	private final String key;
	@JsonProperty("project_name")
	private final String name;
	@JsonProperty("project_tag")
	private final String tag;
	@JsonProperty("endpoint_name")
	private final String endpoint;
}
