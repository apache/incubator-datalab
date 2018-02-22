package com.epam.dlab.dto.aws.computational;

import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class AwsComputationalTerminateDTO extends ComputationalTerminateDTO {

    @JsonProperty("emr_cluster_name")
    private String clusterName;
}
