package com.epam.datalab.dto.azure.computational;

import com.epam.datalab.dto.computational.ComputationalTerminateDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class AzureComputationalTerminateDTO extends ComputationalTerminateDTO {

    @JsonProperty("hdinsight_cluster_name")
    private String clusterName;
}
