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

package com.epam.datalab.billing.azure.rate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Meter {
    @JsonProperty("EffectiveDate")
    private String effectiveDate;
    @JsonProperty("IncludedQuantity")
    private long includedQuantity;
    @JsonProperty("MeterCategory")
    private String meterCategory;
    @JsonProperty("MeterId")
    private String meterId;
    @JsonProperty("MeterName")
    private String meterName;
    @JsonProperty("MeterRates")
    private Map<String, Double> meterRates;
    @JsonProperty("MeterRegion")
    private String meterRegion;
    @JsonProperty("MeterStatus")
    private String meterStatus;
    @JsonProperty("MeterSubCategory")
    private String meterSubCategory;
    @JsonProperty("MeterTags")
    private List<Object> meterTags;
    @JsonProperty("Unit")
    private String unit;
}
