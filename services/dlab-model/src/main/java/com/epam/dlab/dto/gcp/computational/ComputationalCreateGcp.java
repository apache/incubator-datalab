/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.dto.gcp.computational;

import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class ComputationalCreateGcp extends ComputationalBase<ComputationalCreateGcp> {
    @JsonProperty("dataproc_master_count")
    private String masterInstanceCount;
    @JsonProperty("dataproc_slave_count")
    private String slaveInstanceCount;
    @JsonProperty("dataproc_master_instance_type")
    private String masterInstanceType;
    @JsonProperty("dataproc_slave_instance_type")
    private String slaveInstanceType;
    @JsonProperty("dataproc_preemptible_count")
    private String preemptibleCount;
    @JsonProperty("dataproc_version")
    private String version;

    public ComputationalCreateGcp withMasterInstanceCount(String masterInstanceCount) {
        this.masterInstanceCount = masterInstanceCount;
        return this;
    }

    public ComputationalCreateGcp withSlaveInstanceCount(String slaveInstanceCount) {
        this.slaveInstanceCount = slaveInstanceCount;
        return this;
    }

    public ComputationalCreateGcp withMasterInstanceType(String masterInstanceType) {
        this.masterInstanceType = masterInstanceType;
        return this;
    }

    public ComputationalCreateGcp withSlaveInstanceType(String slaveInstanceType) {
        this.slaveInstanceType = slaveInstanceType;
        return this;
    }

    public ComputationalCreateGcp withPreemptibleCount(String preemptibleCount) {
        this.preemptibleCount = preemptibleCount;
        return this;
    }

    public ComputationalCreateGcp withVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("version", version)
                .add("masterInstanceType", masterInstanceType)
                .add("slaveInstanceType", slaveInstanceType)
                .add("masterInstanceCount", masterInstanceCount)
                .add("slaveInstanceCount", slaveInstanceCount)
                .add("preemptibleCount", preemptibleCount);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
