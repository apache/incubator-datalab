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

package com.epam.datalab.dto.gcp.computational;

import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.util.List;


@ToString
public class SparkComputationalCreateGcp extends ComputationalBase<SparkComputationalCreateGcp> {

    @JsonProperty("dataengine_instance_count")
    private String dataengineInstanceCount;
    @JsonProperty("gcp_dataengine_master_size")
    private String masterDataEngineInstanceShape;
    @JsonProperty("gcp_dataengine_slave_size")
    private String slaveDtaEngineInstanceShape;

    @JsonProperty("master_gpu_type")
    private String masterGPUType;
    @JsonProperty("slave_gpu_type")
    private String slaveGPUType;
    @JsonProperty("master_gpu_count")
    private String masterGPUCount;
    @JsonProperty("slave_gpu_count")
    private String slaveGPUCount;


    @JsonProperty("spark_configurations")
    private List<ClusterConfig> config;
    @JsonProperty("conf_shared_image_enabled")
    private String sharedImageEnabled;

    public SparkComputationalCreateGcp withDataengineInstanceCount(String dataengineInstanceCount) {
        this.dataengineInstanceCount = dataengineInstanceCount;
        return this;
    }

    public SparkComputationalCreateGcp withDataEngineSlaveSize(String dataEngineSlaveSize) {
        this.slaveDtaEngineInstanceShape = dataEngineSlaveSize;
        return this;
    }

    public SparkComputationalCreateGcp withDataEngineMasterSize(String dataEngineMasterSize) {
        this.masterDataEngineInstanceShape = dataEngineMasterSize;
        return this;
    }

    public SparkComputationalCreateGcp withMasterGPUType(String masterGPUType) {
        this.masterGPUType = masterGPUType;
        return this;
    }

    public SparkComputationalCreateGcp withSlaveGPUType(String slaveGPUType) {
        this.slaveGPUType = slaveGPUType;
        return this;
    }

    public SparkComputationalCreateGcp withMasterGPUCount(String masterGPUCount) {
        this.masterGPUCount = masterGPUCount;
        return this;
    }

    public SparkComputationalCreateGcp withSlaveGPUCount(String slaveGPUCount) {
        this.slaveGPUCount = slaveGPUCount;
        return this;
    }

    public SparkComputationalCreateGcp withConfig(List<ClusterConfig> config) {
        this.config = config;
        return this;
    }

    public SparkComputationalCreateGcp withSharedImageEnabled(String sharedImageEnabled) {
        this.sharedImageEnabled = sharedImageEnabled;
        return this;
    }
}
