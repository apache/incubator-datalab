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

package com.epam.datalab.dto.exploratory;

import com.epam.datalab.dto.ResourceURL;
import com.epam.datalab.dto.StatusEnvBaseDTO;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.Date;
import java.util.List;

public class ExploratoryStatusDTO extends StatusEnvBaseDTO<ExploratoryStatusDTO> {
    @JsonProperty("exploratory_url")
    private List<ResourceURL> resourceUrl;
    @JsonProperty("exploratory_user")
    private String exploratoryUser;
    @JsonProperty("exploratory_pass")
    private String exploratoryPassword;
    @JsonProperty("private_ip")
    private String privateIp;
    @JsonProperty("last_activity")
    private Date lastActivity;
    @JsonProperty
    private List<ClusterConfig> config;

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public ExploratoryStatusDTO withPrivateIp(String privateIp) {
        setPrivateIp(privateIp);
        return this;
    }

    public List<ResourceURL> getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(List<ResourceURL> resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public ExploratoryStatusDTO withExploratoryUrl(List<ResourceURL> resourceUrl) {
        setResourceUrl(resourceUrl);
        return this;
    }

    public String getExploratoryUser() {
        return exploratoryUser;
    }

    public void setExploratoryUser(String exploratoryUser) {
        this.exploratoryUser = exploratoryUser;
    }

    public ExploratoryStatusDTO withExploratoryUser(String exploratoryUser) {
        setExploratoryUser(exploratoryUser);
        return this;
    }

    public String getExploratoryPassword() {
        return exploratoryPassword;
    }

    public void setExploratoryPassword(String exploratoryPassword) {
        this.exploratoryPassword = exploratoryPassword;
    }

    public ExploratoryStatusDTO withExploratoryPassword(String exploratoryPassword) {
        setExploratoryPassword(exploratoryPassword);
        return this;
    }

    public ExploratoryStatusDTO withConfig(List<ClusterConfig> config) {
        this.config = config;
        return this;
    }

    public List<ClusterConfig> getConfig() {
        return config;
    }

    public ExploratoryStatusDTO withLastActivity(Date lastActivity) {
        this.lastActivity = lastActivity;
        return this;
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("exploratoryUrl", resourceUrl)
                .add("exploratoryUser", exploratoryUser)
                .add("exploratoryPassword", exploratoryPassword)
                .add("config", config);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
