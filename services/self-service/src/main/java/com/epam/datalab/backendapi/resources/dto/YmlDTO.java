package com.epam.datalab.backendapi.resources.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
public class YmlDTO {

    @JsonIgnoreProperties
    private String endpointName;
    private String ymlString;
}