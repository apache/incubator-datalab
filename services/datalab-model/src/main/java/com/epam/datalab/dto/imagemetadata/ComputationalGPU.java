package com.epam.datalab.dto.imagemetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ComputationalGPU {

    @JsonProperty("masterGPUType")
    private List<String> masterGPUType;
    @JsonProperty("slaveGPUType")
    private List<String> slaveGPUType;
    @JsonProperty("masterGPUCount")
    private Long masterGPUCount;
    @JsonProperty("slaveGPUCount")
    private Long slaveGPUCount;
}
