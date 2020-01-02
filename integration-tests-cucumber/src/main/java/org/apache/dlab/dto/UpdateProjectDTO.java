package org.apache.dlab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class UpdateProjectDTO {
    private String name;
    private Set<String> groups;
    private Set<String> endpoints;
    @JsonProperty("shared_image_enabled")
    private boolean sharedImageEnabled;
}
