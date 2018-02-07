package com.epam.dlab.backendapi.resources.dto.gcp;

import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

@Data
@ToString(callSuper = true)
public class GcpComputationalCreateForm extends ComputationalCreateFormDTO {

    @NotBlank
    @JsonProperty("dataproc_master_count")
    private String masterInstanceCount;

    @NotBlank
    @JsonProperty("dataproc_slave_count")
    private String slaveInstanceCount;

    @NotBlank
    @JsonProperty("dataproc_preemptible_count")
    private String preemptibleCount;

    @JsonProperty("dataproc_master_instance_type")
    private String masterInstanceType;

    @JsonProperty("dataproc_slave_instance_type")
    private String slaveInstanceType;

    @NotBlank
    @JsonProperty("dataproc_version")
    private String version;
}
