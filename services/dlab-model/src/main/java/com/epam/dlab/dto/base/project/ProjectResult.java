package com.epam.dlab.dto.base.project;

import com.epam.dlab.dto.StatusBaseDTO;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectResult extends StatusBaseDTO<ProjectResult> {
	private EdgeInfo edgeInfo;
	@JsonProperty("project_name")
	private String projectName;
	@JsonProperty("endpoint_name")
	private String endpointName;

}
