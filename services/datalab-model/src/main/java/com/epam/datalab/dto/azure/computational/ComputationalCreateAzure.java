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
import com.epam.datalab.dto.aws.computational.ComputationalCreateAws;
import com.epam.datalab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ComputationalCreateAzure extends ComputationalBase<ComputationalCreateAzure> {
    @JsonProperty("hdinsight_count")
    private String instanceCount;
    @JsonProperty("hdinsight_slave_count")
    private String slaveInstanceCount;
    @JsonProperty("hdinsight_master_instance_type")
    private String masterInstanceType;
    @JsonProperty("hdinsight_slave_instance_type")
    private String slaveInstanceType;
    @JsonProperty("hdinsight_version")
    private String version;
    @JsonProperty("config")
    private List<ClusterConfig> config;
    @JsonProperty("conf_shared_image_enabled")
    private String sharedImageEnabled;

    public String getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(String instanceCount) {
        this.instanceCount = instanceCount;
    }

    public ComputationalCreateAzure withInstanceCount(String instanceCount) {
        setInstanceCount(instanceCount);
        return this;
    }

    public ComputationalCreateAzure withMasterInstanceType(String masterInstanceType) {
        setMasterInstanceType(masterInstanceType);
        return this;
    }

    public ComputationalCreateAzure withSlaveInstanceType(String slaveInstanceType) {
        setSlaveInstanceType(slaveInstanceType);
        return this;
    }

    public ComputationalCreateAzure withVersion(String version) {
        setVersion(version);
        return this;
    }

    public ComputationalCreateAzure withConfig(List<ClusterConfig> config) {
        setConfig(config);
        return this;
    }

    public ComputationalCreateAzure withSharedImageEnabled(String sharedImageEnabled) {
        setSharedImageEnabled(sharedImageEnabled);
        return this;
    }

    public ComputationalCreateAzure withNotebookName(String name) {
        setNotebookInstanceName(name);
        return this;
    }
}
