package org.apache.dlab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ProjectActionDTO {
    @JsonProperty("project_name")
    private final String projectName;
    @JsonProperty("endpoint")
    private final List<String> endpoints;
}
