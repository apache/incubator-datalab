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

package com.epam.datalab.backendapi.resources.dto.aws;

import com.epam.datalab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AwsComputationalCreateForm extends ComputationalCreateFormDTO {

    @NotBlank
    @JsonProperty("emr_instance_count")
    private String instanceCount;

    @NotBlank
    @JsonProperty("emr_master_instance_type")
    private String masterInstanceType;

    @NotBlank
    @JsonProperty("emr_slave_instance_type")
    private String slaveInstanceType;

    @JsonProperty("emr_slave_instance_spot")
    private Boolean slaveInstanceSpot = false;

    @JsonProperty("emr_slave_instance_spot_pct_price")
    private Integer slaveInstanceSpotPctPrice;

    @NotBlank
    @JsonProperty("emr_version")
    private String version;
}
