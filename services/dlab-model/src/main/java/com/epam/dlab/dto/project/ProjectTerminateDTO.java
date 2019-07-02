package com.epam.dlab.dto.project;

import com.epam.dlab.dto.ResourceBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectTerminateDTO extends ResourceBaseDTO<ProjectTerminateDTO> {
	@JsonProperty("project_name")
	private final String name;
}
