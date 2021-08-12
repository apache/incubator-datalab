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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Stores limits for creation of the computational resources for EMR cluster
 */
@Data
@Builder
public class AwsEmrConfiguration {
    @NotBlank
    @JsonProperty("min_emr_instance_count")
    private int minEmrInstanceCount;

    @NotBlank
    @JsonProperty("max_emr_instance_count")
    private int maxEmrInstanceCount;

    @NotBlank
    @JsonProperty("min_emr_spot_instance_bid_pct")
    private int minEmrSpotInstanceBidPct;

    @NotBlank
    @JsonProperty("max_emr_spot_instance_bid_pct")
    private int maxEmrSpotInstanceBidPct;
}
