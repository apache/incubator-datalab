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

package com.epam.datalab.dto.computational;

import com.epam.datalab.dto.ResourceURL;
import com.epam.datalab.dto.SchedulerJobDTO;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SparkStandaloneClusterResource extends UserComputationalResource {

    @NotBlank
    @JsonProperty("master_instance_shape")
    private String masterDataEngineInstanceShape;

    @NotBlank
    @JsonProperty("data_engine_instance_count")
    private String dataEngineInstanceCount;

    @NotBlank
    @JsonProperty("slave_instance_shape")
    private String slaveDataEngineInstanceShape;

    @Builder
    public SparkStandaloneClusterResource(String computationalName, String computationalId, String imageName,
                                          String templateName, String status, Date uptime,
                                          SchedulerJobDTO schedulerJobData, boolean reuploadKeyRequired,
                                          String dataEngineInstanceCount, String masterDataEngineInstanceShape,
                                          String slaveDataEngineInstanceShape,
                                          String masterGpuCount, String masterGpuType,
                                          String slaveGpuCount, String slaveGpuType,
                                          Boolean enabledGPU,
                                          List<ResourceURL> resourceURL, LocalDateTime lastActivity,
                                          List<ClusterConfig> config, Map<String, String> tags, int totalInstanceCount) {
        super(computationalName, computationalId, imageName, templateName, status, uptime, schedulerJobData,
                reuploadKeyRequired, resourceURL, lastActivity, tags, totalInstanceCount);
        super.setMasterGpuCount(masterGpuCount);
        super.setMasterGpuType(masterGpuType);
        super.setSlaveGpuCount(slaveGpuCount);
        super.setSlaveGpuType(slaveGpuType);
        super.setEnabledGPU(enabledGPU);
        super.setMasterNodeShape(masterDataEngineInstanceShape);
        super.setSlaveNodeShape(slaveDataEngineInstanceShape);

        this.masterDataEngineInstanceShape = masterDataEngineInstanceShape;
        this.slaveDataEngineInstanceShape = slaveDataEngineInstanceShape;
        this.dataEngineInstanceCount = dataEngineInstanceCount;

        this.config = config;
    }
}
