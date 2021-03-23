package com.epam.datalab.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
public class YmlDTO {

    @JsonIgnoreProperties
    private String endpointName;
    private String ymlString;
}