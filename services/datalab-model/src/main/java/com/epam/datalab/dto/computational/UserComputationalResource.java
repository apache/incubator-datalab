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
import com.epam.datalab.dto.base.DataEngineType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserComputationalResource {
    @JsonProperty("computational_name")
    private String computationalName;
    @JsonProperty("computational_id")
    private String computationalId;
    @JsonProperty("image")
    private String imageName;
    @JsonProperty("template_name")
    private String templateName;
    @JsonProperty
    private String status;
    @JsonProperty("up_time")
    private Date uptime;
    @JsonProperty("scheduler_data")
    private SchedulerJobDTO schedulerData;
    @JsonProperty("reupload_key_required")
    private boolean reuploadKeyRequired = false;
    @JsonProperty("computational_url")
    private List<ResourceURL> resourceUrl;
    @JsonProperty("last_activity")
    private LocalDateTime lastActivity;
    @JsonProperty("master_node_shape")
    private String masterNodeShape;
    @JsonProperty("slave_node_shape")
    private String slaveNodeShape;
    @JsonProperty("dataengine_instance_shape")
    private String dataengineShape;
    @JsonProperty("dataengine_instance_count")
    private int dataengineInstanceCount;
    @JsonProperty("instance_id")
    private String instanceId;
    @JsonProperty("dataproc_version")
    private String gcpClusterVersion;
    @JsonProperty("emr_version")
    private String awsClusterVersion;
    @JsonProperty("hdinsight_version")
    private String azureClusterVersion;
    private int totalInstanceCount;
    protected List<ClusterConfig> config;
    private Map<String, String> tags;
    @JsonProperty("master_gpu_type")
    private String masterGpuType;
    @JsonProperty("master_gpu_count")
    private String masterGpuCount;
    @JsonProperty("slave_gpu_type")
    private String slaveGpuType;
    @JsonProperty("slave_gpu_count")
    private String slaveGpuCount;
    private boolean enabledGPU;

    public UserComputationalResource(String computationalName, String computationalId, String imageName,
                                     String templateName, String status, Date uptime, SchedulerJobDTO schedulerData,
                                     boolean reuploadKeyRequired, List<ResourceURL> resourceUrl,
                                     LocalDateTime lastActivity, Map<String, String> tags, int totalInstanceCount) {
        this.computationalName = computationalName;
        this.computationalId = computationalId;
        this.imageName = imageName;
        this.templateName = templateName;
        this.status = status;
        this.uptime = uptime;
        this.schedulerData = schedulerData;
        this.reuploadKeyRequired = reuploadKeyRequired;
        this.resourceUrl = resourceUrl;
        this.lastActivity = lastActivity;
        this.tags = tags;
        this.totalInstanceCount = totalInstanceCount;
    }

    public DataEngineType getDataEngineType() {
        return DataEngineType.fromDockerImageName(imageName);
    }
}
