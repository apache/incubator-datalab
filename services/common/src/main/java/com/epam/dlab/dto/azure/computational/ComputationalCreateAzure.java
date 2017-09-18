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

package com.epam.dlab.dto.azure.computational;

import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class ComputationalCreateAzure extends ComputationalBase<ComputationalCreateAzure> {
    @JsonProperty("dataengine_instance_count")
    private String dataEngineInstanceCount;
    @JsonProperty("azure_dataengine_slave_size")
    private String dataEngineSlaveSize;
    @JsonProperty("azure_dataengine_master_size")
    private String dataEngineMasterSize;

    public ComputationalCreateAzure withDataEngineInstanceCount(String dataEngineInstanceCount) {
        this.dataEngineInstanceCount = dataEngineInstanceCount;
        return this;
    }

    public ComputationalCreateAzure withDataEngineSlaveSize(String dataEngineSlaveSize) {
        this.dataEngineSlaveSize = dataEngineSlaveSize;
        return this;
    }

    public ComputationalCreateAzure withDataEngineMasterSize(String dataEngineMasterSize) {
        this.dataEngineMasterSize = dataEngineMasterSize;
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("dataEngineInstanceCount", dataEngineInstanceCount)
                .add("dataEngineSlaveSize", dataEngineSlaveSize)
                .add("dataEngineMasterSize", dataEngineMasterSize);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}

