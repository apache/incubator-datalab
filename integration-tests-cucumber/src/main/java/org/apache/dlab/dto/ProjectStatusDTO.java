package org.apache.dlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectStatusDTO {
    private String name;
    private Set<EndpointStatusDTO> endpoints;
}
