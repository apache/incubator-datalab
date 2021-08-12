/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.backendapi.resources.dto.gcp;

import com.epam.datalab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
@JsonIgnoreProperties
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

    @JsonProperty("gpu_enabled")
    private Boolean enabledGPU;

    @JsonProperty("master_gpu_type")
    private String masterGpuType;

    @JsonProperty("master_gpu_count")
    private String masterGpuCount;

    @JsonProperty("slave_gpu_type")
    private String slaveGpuType;

    @JsonProperty("slave_gpu_count")
    private String slaveGpuCount;
}
