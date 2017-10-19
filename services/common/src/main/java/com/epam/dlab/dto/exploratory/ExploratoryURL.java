package com.epam.dlab.dto.exploratory;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describe URL of exploratory.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExploratoryURL {
    @JsonProperty("description")
    private String description;
    @JsonProperty("url")
    private String url;
}
