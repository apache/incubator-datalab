package com.epam.datalab.backendapi.resources.dto.azure;

import com.epam.datalab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties
public class AzureComputationalCreateForm extends ComputationalCreateFormDTO {

    @NotBlank
    @JsonProperty("hdinsight_master_instance_type")
    private String masterInstanceType;
    @NotBlank
    @JsonProperty("hdinsight_slave_instance_type")
    private String slaveInstanceType;
    @NotBlank
    @JsonProperty("hdinsight_version")
    private String version;

    @NotBlank
    @JsonProperty("hdinsight_slave_instance_count")
    private String slaveInstanceCount;


}
