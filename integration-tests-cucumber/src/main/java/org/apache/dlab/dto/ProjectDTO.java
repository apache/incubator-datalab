package org.apache.dlab.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectDTO {
    private String name;
    private Set<String> groups;
    private Set<EndpointStatusDTO> endpoints;
    private boolean sharedImageEnabled;
}
