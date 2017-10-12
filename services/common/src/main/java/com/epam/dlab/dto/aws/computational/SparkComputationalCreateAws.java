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

package com.epam.dlab.dto.aws.computational;

import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class SparkComputationalCreateAws extends ComputationalBase<SparkComputationalCreateAws> {

    @JsonProperty("dataengine_instance_count")
    private String dataEngineInstanceCount;
    @JsonProperty("aws_dataengine_slave_shape")
    private String dataEngineSlaveShape;
    @JsonProperty("aws_dataengine_master_shape")
    private String dataEngineMasterShape;

    public SparkComputationalCreateAws withDataEngineInstanceCount(String dataEngineInstanceCount) {
        this.dataEngineInstanceCount = dataEngineInstanceCount;
        return this;
    }

    public SparkComputationalCreateAws withDataEngineSlaveShape(String dataEngineSlaveSize) {
        this.dataEngineSlaveShape = dataEngineSlaveSize;
        return this;
    }

    public SparkComputationalCreateAws withDataEngineMasterShape(String dataEngineMasterSize) {
        this.dataEngineMasterShape = dataEngineMasterSize;
        return this;
    }

    public String getDataEngineInstanceCount() {
        return dataEngineInstanceCount;
    }

    public String getDataEngineSlaveShape() {
        return dataEngineSlaveShape;
    }

    public String getDataEngineMasterShape() {
        return dataEngineMasterShape;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("dataEngineInstanceCount", dataEngineInstanceCount)
                .add("dataEngineSlaveShape", dataEngineSlaveShape)
                .add("dataEngineMasterShape", dataEngineMasterShape);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
