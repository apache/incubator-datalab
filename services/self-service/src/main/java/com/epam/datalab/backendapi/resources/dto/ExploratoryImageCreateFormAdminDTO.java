package com.epam.datalab.backendapi.resources.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

@Data
@ToString
public class ExploratoryImageCreateFormAdminDTO {
    @NotBlank
    private String user;
    @NotBlank
    private final String name;
    @NotBlank
    @JsonProperty("exploratory_name")
    private String notebookName;
    @NotBlank
    @JsonProperty("project_name")
    private String projectName;
    private final String description;
}
