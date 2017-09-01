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

package com.epam.dlab.dto.azure.edge;

import com.epam.dlab.dto.azure.AzureResource;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class EdgeCreateAzure extends AzureResource<EdgeCreateAzure> {
    @JsonProperty("azure_vpc_name")
    private String azureVpcName;
    @JsonProperty("azure_subnet_name")
    private String azureSubnetName;
    @JsonProperty("azure_resource_group_name")
    private String azureSecurityGroupName;

    public String getAzureVpcName() {
        return azureVpcName;
    }

    public EdgeCreateAzure withAzureVpcName(String awsVpcName) {
        this.azureVpcName = awsVpcName;
        return this;
    }

    public String getAzureSubnetName() {
        return azureSubnetName;
    }

    public EdgeCreateAzure withAzureSubnetName(String awsSubnetName) {
        this.azureSubnetName = awsSubnetName;
        return this;
    }

    public String getAzureSecurityGroupName() {
        return azureSecurityGroupName;
    }

    public EdgeCreateAzure withAzureSecurityGroupName(String awsSecurityGroupName) {
        this.azureSecurityGroupName = awsSecurityGroupName;
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("azureVpcName", azureVpcName)
                .add("azureSubnetName", azureSubnetName)
                .add("azureSecurityGroupName", azureSecurityGroupName);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
