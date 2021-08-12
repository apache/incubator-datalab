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

package com.epam.datalab.dto.azure.computational;

import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.List;

public class SparkComputationalCreateAzure extends ComputationalBase<SparkComputationalCreateAzure> {
    @JsonProperty("dataengine_instance_count")
    private String dataEngineInstanceCount;
    @JsonProperty("azure_dataengine_slave_size")
    private String dataEngineSlaveSize;
    @JsonProperty("azure_dataengine_master_size")
    private String dataEngineMasterSize;
    @JsonProperty("azure_datalake_enable")
    private String azureDataLakeEnabled;
    @JsonProperty("azure_user_refresh_token")
    private String azureUserRefreshToken;
    @JsonProperty("spark_configurations")
    private List<ClusterConfig> config;
    @JsonProperty("conf_shared_image_enabled")
    private String sharedImageEnabled;

    public SparkComputationalCreateAzure withDataEngineInstanceCount(String dataEngineInstanceCount) {
        this.dataEngineInstanceCount = dataEngineInstanceCount;
        return this;
    }

    public SparkComputationalCreateAzure withDataEngineSlaveSize(String dataEngineSlaveSize) {
        this.dataEngineSlaveSize = dataEngineSlaveSize;
        return this;
    }

    public SparkComputationalCreateAzure withDataEngineMasterSize(String dataEngineMasterSize) {
        this.dataEngineMasterSize = dataEngineMasterSize;
        return this;
    }

    public SparkComputationalCreateAzure withAzureDataLakeEnabled(String azureDataLakeEnabled) {
        this.azureDataLakeEnabled = azureDataLakeEnabled;
        return this;
    }

    public SparkComputationalCreateAzure withAzureUserRefreshToken(String azureUserRefreshToken) {
        this.azureUserRefreshToken = azureUserRefreshToken;
        return this;
    }

    public SparkComputationalCreateAzure withConfig(List<ClusterConfig> config) {
        this.config = config;
        return this;
    }

    public SparkComputationalCreateAzure withSharedImageEnabled(String sharedImageEnabled) {
        this.sharedImageEnabled = sharedImageEnabled;
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("dataEngineInstanceCount", dataEngineInstanceCount)
                .add("dataEngineSlaveSize", dataEngineSlaveSize)
                .add("dataEngineMasterSize", dataEngineMasterSize)
                .add("azureDataLakeEnabled", azureDataLakeEnabled)
                .add("azureUserRefreshToken", azureUserRefreshToken != null ? "***" : null);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
