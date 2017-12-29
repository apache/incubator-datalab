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

package com.epam.dlab.dto.azure.exploratory;

import com.epam.dlab.dto.exploratory.ExploratoryCreateDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class ExploratoryCreateAzure extends ExploratoryCreateDTO<ExploratoryCreateAzure> {
    @JsonProperty("azure_notebook_instance_size")
    private String notebookInstanceType;
    @JsonProperty("azure_client_id")
    private String azureClientId;
    @JsonProperty("azure_datalake_enable")
    private String azureDataLakeEnabled;
    @JsonProperty("azure_user_refresh_token")
    private String azureUserRefreshToken;

    public String getNotebookInstanceType() {
        return notebookInstanceType;
    }

    public void setNotebookInstanceType(String notebookInstanceType) {
        this.notebookInstanceType = notebookInstanceType;
    }

    public String getAzureClientId() {
        return azureClientId;
    }

    public void setAzureClientId(String azureClientId) {
        this.azureClientId = azureClientId;
    }

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

    public ExploratoryCreateAzure withNotebookInstanceSize(String notebookInstanceType) {
        setNotebookInstanceType(notebookInstanceType);
        return this;
    }

    public ExploratoryCreateAzure withAzureClientId(String azureClientId) {
        setAzureClientId(azureClientId);
        return this;
    }

    public ExploratoryCreateAzure withAzureDataLakeEnabled(String azureDataLakeEnabled) {
        setAzureDataLakeEnabled(azureDataLakeEnabled);
        return this;
    }

    public ExploratoryCreateAzure withAzureUserRefreshToken(String azureUserRefreshToken) {
        setAzureUserRefreshToken(azureUserRefreshToken);
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("notebookInstanceType", notebookInstanceType)
                .add("azureClientId", azureClientId != null ? "***" : null)
                .add("azureDataLakeEnabled", azureDataLakeEnabled)
                .add("azureUserRefreshToken", azureUserRefreshToken != null ? "***" : null);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
