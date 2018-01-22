package com.epam.dlab.dto.gcp.computational;

import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class GcpComputationalTerminateDTO extends ComputationalTerminateDTO {

    @JsonProperty("dataproc_cluster_name")
    private String clusterName;
}
