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

package com.epam.datalab.dto.aws.computational;

import com.epam.datalab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ComputationalCreateAws extends ComputationalBase<ComputationalCreateAws> {
    @JsonProperty("emr_instance_count")
    private String instanceCount;
    @JsonProperty("emr_master_instance_type")
    private String masterInstanceType;
    @JsonProperty("emr_slave_instance_type")
    private String slaveInstanceType;
    @JsonProperty("emr_slave_instance_spot")
    private Boolean slaveInstanceSpot;
    @JsonProperty("emr_slave_instance_spot_pct_price")
    private Integer slaveInstanceSpotPctPrice;
    @JsonProperty("emr_version")
    private String version;
    @JsonProperty("emr_configurations")
    private List<ClusterConfig> config;
    @JsonProperty("conf_shared_image_enabled")
    private String sharedImageEnabled;

    public String getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(String instanceCount) {
        this.instanceCount = instanceCount;
    }

    public ComputationalCreateAws withInstanceCount(String instanceCount) {
        setInstanceCount(instanceCount);
        return this;
    }

    public ComputationalCreateAws withMasterInstanceType(String masterInstanceType) {
        setMasterInstanceType(masterInstanceType);
        return this;
    }

    public ComputationalCreateAws withSlaveInstanceType(String slaveInstanceType) {
        setSlaveInstanceType(slaveInstanceType);
        return this;
    }

    public ComputationalCreateAws withSlaveInstanceSpot(Boolean slaveInstanceSpot) {
        setSlaveInstanceSpot(slaveInstanceSpot);
        return this;
    }

    public void setSlaveInstanceSpotPctPrice(Integer slaveInstanceSpotPctPrice) {
        this.slaveInstanceSpotPctPrice = slaveInstanceSpotPctPrice;
    }

    public ComputationalCreateAws withSlaveInstanceSpotPctPrice(Integer slaveInstanceSpotPctPrice) {
        setSlaveInstanceSpotPctPrice(slaveInstanceSpotPctPrice);
        return this;
    }

    public ComputationalCreateAws withVersion(String version) {
        setVersion(version);
        return this;
    }

    public ComputationalCreateAws withConfig(List<ClusterConfig> config) {
        setConfig(config);
        return this;
    }

    public ComputationalCreateAws withSharedImageEnabled(String sharedImageEnabled) {
        setSharedImageEnabled(sharedImageEnabled);
        return this;
    }

    public ComputationalCreateAws withNotebookName(String name) {
        setNotebookInstanceName(name);
        return this;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("version", version)
                .add("masterInstanceType", masterInstanceType)
                .add("slaveInstanceType", slaveInstanceType)
                .add("slaveInstanceSpot", slaveInstanceSpot)
                .add("slaveInstanceSpotPctPrice", slaveInstanceSpotPctPrice)
                .add("instanceCount", instanceCount);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
