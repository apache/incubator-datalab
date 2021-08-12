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
import com.epam.datalab.dto.StatusEnvBaseDTO;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.Date;
import java.util.List;

public class ComputationalStatusDTO extends StatusEnvBaseDTO<ComputationalStatusDTO> {
    @JsonProperty("computational_url")
    private List<ResourceURL> resourceUrl;
    @JsonProperty("computational_id")
    private String computationalId;
    @JsonProperty("computational_name")
    private String computationalName;
    @JsonProperty("last_activity")
    private Date lastActivity;
    @JsonProperty
    private List<ClusterConfig> config;

    public String getComputationalId() {
        return computationalId;
    }

    public void setComputationalId(String computationalId) {
        this.computationalId = computationalId;
    }

    public ComputationalStatusDTO withComputationalId(String computationalId) {
        setComputationalId(computationalId);
        return this;
    }

    public List<ResourceURL> getResourceUrl() {
        return resourceUrl;
    }

    public String getComputationalName() {
        return computationalName;
    }

    public void setComputationalName(String computationalName) {
        this.computationalName = computationalName;
    }

    public void setResourceUrl(List<ResourceURL> resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public void setLastActivity(Date lastActivity) {
        this.lastActivity = lastActivity;
    }

    public ComputationalStatusDTO withComputationalUrl(List<ResourceURL> resourceUrl) {
        setResourceUrl(resourceUrl);
        return this;
    }

    public ComputationalStatusDTO withComputationalName(String computationalName) {
        setComputationalName(computationalName);
        return this;
    }

    public ComputationalStatusDTO withConfig(List<ClusterConfig> config) {
        this.config = config;
        return this;
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    public ComputationalStatusDTO withLastActivity(Date lastActivity) {
        setLastActivity(lastActivity);
        return this;
    }

    public List<ClusterConfig> getConfig() {
        return config;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("computationalUrl", resourceUrl)
                .add("computationalId", computationalId)
                .add("computationalName", computationalName)
                .add("lastActivity", lastActivity)
                .add("config", config);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
