package com.epam.dlab.backendapi.resources.dto;

import com.epam.dlab.dto.exploratory.ImageStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageInfoRecord {
    private final String name;
    private final String description;
    private final String application;
    private final String fullName;
    private final ImageStatus status;
}
