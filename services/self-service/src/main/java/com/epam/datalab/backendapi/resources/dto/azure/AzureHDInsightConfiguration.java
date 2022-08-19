package com.epam.datalab.backendapi.resources.dto.azure;

import com.epam.datalab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
public class AzureHDInsightConfiguration  {
    @NotBlank
    @JsonProperty("min_instance_count")
    private int minHdinsightInstanceCount;
    @NotBlank
    @JsonProperty("max_instance_count")
    private int maxHdinsightInstanceCount;

}
