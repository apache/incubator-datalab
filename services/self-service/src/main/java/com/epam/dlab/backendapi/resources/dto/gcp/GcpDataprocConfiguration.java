package com.epam.dlab.backendapi.resources.dto.gcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import java.util.List;

/**
 * Stores limits for creation of the computational resources for Dataproc cluster
 */
@Data
@Builder
public class GcpDataprocConfiguration {

    @NotBlank
    @JsonProperty("dataproc_available_master_instance_count")
    private List<Integer> dataprocAvailableMasterInstanceCount;

    @NotBlank
    @JsonProperty("min_dataproc_slave_instance_count")
    private int minDataprocSlaveInstanceCount;

    @NotBlank
    @JsonProperty("max_dataproc_slave_instance_count")
    private int maxDataprocSlaveInstanceCount;
    @NotBlank
    @JsonProperty("min_dataproc_preemptible_instance_count")
    private int minDataprocPreemptibleInstanceCount;
}
