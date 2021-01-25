package com.epam.datalab.dto.imagemetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ComputationalGPUs {

    @JsonProperty("Type")
    private String type;
    @JsonProperty("Description")
    private String description;
    @JsonProperty("Gpu type")
    private String gpuType;
}
