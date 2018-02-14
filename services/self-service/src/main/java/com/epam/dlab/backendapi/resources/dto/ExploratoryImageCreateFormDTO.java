package com.epam.dlab.backendapi.resources.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

@Data
@ToString
public class ExploratoryImageCreateFormDTO {

    @NotBlank
    @JsonProperty("exploratory_name")
    private String notebookName;
    @NotBlank
    private final String name;
    private final String description;
}
