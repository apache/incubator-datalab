package com.epam.datalab.backendapi.resources.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageUserPermissions {
    private final boolean canShare;
    private final boolean canTerminate;
}
