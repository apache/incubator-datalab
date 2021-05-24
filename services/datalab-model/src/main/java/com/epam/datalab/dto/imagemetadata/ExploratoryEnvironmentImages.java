package com.epam.datalab.dto.imagemetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExploratoryEnvironmentImages {

    @JsonProperty("Image family")
    private String imageFamily;
    @JsonProperty("Description")
    private String description;
}
