package com.epam.datalab.dto.imagemetadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ComputationalGPU {

    @JsonProperty("masterGPUType")
    private Map<String, List<ComputationalGPUs>> masterGPUType;

    @JsonProperty("slaveGPUType")
    private Map<String, List<ComputationalGPUs>> slaveGPUType;
}
