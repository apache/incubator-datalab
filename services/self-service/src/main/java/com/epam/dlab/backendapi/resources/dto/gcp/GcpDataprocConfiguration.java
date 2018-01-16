package com.epam.dlab.backendapi.resources.dto.gcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Stores limits for creation of the computational resources for Dataproc cluster
 */
@Data
@Builder
public class GcpDataprocConfiguration {

    @NotBlank
    @JsonProperty("min_dataproc_master_instance_count")
    private int minDataprocMasterInstanceCount;

    @NotBlank
    @JsonProperty("max_dataproc_master_instance_count")
    private int maxDataprocMasterInstanceCount;

    @NotBlank
    @JsonProperty("min_dataproc_slave_instance_count")
    private int minDataprocSlaveInstanceCount;

    @NotBlank
    @JsonProperty("max_dataproc_slave_instance_count")
    private int maxDataprocSlaveInstanceCount;
}
