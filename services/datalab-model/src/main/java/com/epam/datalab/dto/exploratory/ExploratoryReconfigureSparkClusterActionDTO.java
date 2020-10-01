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

import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExploratoryReconfigureSparkClusterActionDTO extends ExploratoryActionDTO<ExploratoryReconfigureSparkClusterActionDTO> {

    @JsonProperty("spark_configurations")
    private List<ClusterConfig> config;
    @JsonProperty("azure_user_refresh_token")
    private String azureUserRefreshToken;

    public ExploratoryReconfigureSparkClusterActionDTO withConfig(List<ClusterConfig> config) {
        this.config = config;
        return this;
    }

    public ExploratoryReconfigureSparkClusterActionDTO withAzureUserRefreshToken(String azureUserRefreshToken) {
        this.azureUserRefreshToken = azureUserRefreshToken;
        return this;
    }
}
