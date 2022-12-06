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

import com.epam.datalab.dto.ResourceURL;
import com.epam.datalab.dto.SchedulerJobDTO;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class AzureComputationalResource extends UserComputationalResource {
    @JsonProperty("instance_id")
    private final String instanceId;
    @JsonProperty("master_node_shape")
    private final String masterShape;
    @JsonProperty("slave_node_shape")
    private final String slaveShape;
    @JsonProperty("hdinsight_version")
    private final String version;
    @JsonProperty("hdinsight_slave_count")
    private final String slaveInstanceCount;


    @Builder
    public AzureComputationalResource(String computationalName, String computationalId, String imageName,
                                      String templateName, String status, Date uptime,
                                      SchedulerJobDTO schedulerJobData, boolean reuploadKeyRequired,
                                      String instanceId, String masterShape, String slaveShape, String version,
                                      List<ResourceURL> resourceURL, LocalDateTime lastActivity,
                                      List<ClusterConfig> config, Map<String, String> tags, int totalInstanceCount, String slaveInstanceCount) {
        super(computationalName, computationalId, imageName, templateName, status, uptime, schedulerJobData,
                reuploadKeyRequired, resourceURL, lastActivity, tags, totalInstanceCount);
        this.instanceId = instanceId;
        this.masterShape = masterShape;
        this.slaveShape = slaveShape;
        this.version = version;
        this.slaveInstanceCount = slaveInstanceCount;
        this.config = config;
    }
}
