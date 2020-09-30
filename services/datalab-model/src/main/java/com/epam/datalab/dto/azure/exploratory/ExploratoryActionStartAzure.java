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

package com.epam.datalab.dto.azure.exploratory;

import com.epam.datalab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class ExploratoryActionStartAzure extends ExploratoryGitCredsUpdateDTO {
    @JsonProperty("azure_datalake_enable")
    private String azureDataLakeEnabled;
    @JsonProperty("azure_user_refresh_token")
    private String azureUserRefreshToken;

    public String getAzureDataLakeEnabled() {
        return azureDataLakeEnabled;
    }

    public void setAzureDataLakeEnabled(String azureDataLakeEnabled) {
        this.azureDataLakeEnabled = azureDataLakeEnabled;
    }

    public String getAzureUserRefreshToken() {
        return azureUserRefreshToken;
    }

    public void setAzureUserRefreshToken(String azureUserRefreshToken) {
        this.azureUserRefreshToken = azureUserRefreshToken;
    }

    public ExploratoryActionStartAzure withAzureDataLakeEnabled(String azureDataLakeEnabled) {
        setAzureDataLakeEnabled(azureDataLakeEnabled);
        return this;
    }

    public ExploratoryActionStartAzure withAzureUserRefreshToken(String azureUserRefreshToken) {
        setAzureUserRefreshToken(azureUserRefreshToken);
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("azureDataLakeEnabled", azureDataLakeEnabled)
                .add("azureUserRefreshToken", azureUserRefreshToken != null ? "***" : null);
    }
}
