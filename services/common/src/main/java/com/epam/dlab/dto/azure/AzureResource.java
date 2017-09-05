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

package com.epam.dlab.dto.azure;

import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class AzureResource<T extends AzureResource<?>> extends ResourceSysBaseDTO<T> {
    @SuppressWarnings("unchecked")
    private final T self = (T) this;
    @JsonProperty("azure_region")
    private String azureRegion;
    @JsonProperty("azure_iam_user")
    private String azureIamUser;
    @JsonProperty("azure_vpc_name")
    private String azureVpcName;
    @JsonProperty("azure_subnet_name")
    private String azureSubnetName;
    @JsonProperty("azure_resource_group_name")
    private String azureResourceGroupName;
    @JsonProperty("azure_security_group_name")
    private String azureSecurityGroupName;

    public String getAzureRegion() {
        return azureRegion;
    }

    public T withAzureRegion(String azureRegion) {
        this.azureRegion = azureRegion;
        return self;
    }

    public String getAzureIamUser() {
        return azureIamUser;
    }

    public T withAzureIamUser(String azureIamUser) {
        this.azureIamUser = azureIamUser;
        return self;
    }

    public String getAzureVpcName() {
        return azureVpcName;
    }

    public T withAzureVpcName(String azureVpcName) {
        this.azureVpcName = azureVpcName;
        return self;
    }

    public String getAzureSubnetName() {
        return azureSubnetName;
    }

    public T withAzureSubnetName(String azureSubnetName) {
        this.azureSubnetName = azureSubnetName;
        return self;
    }

    public String getAzureSecurityGroupName() {
        return azureSecurityGroupName;
    }

    public T withAzureSecurityGroupName(String azureSecurityGroupName) {
        this.azureSecurityGroupName = azureSecurityGroupName;
        return self;
    }

    public String getAzureResourceGroupName() {
        return azureResourceGroupName;
    }

    public T withAzureResourceGroupName(String azureResourceGroupName) {
        this.azureResourceGroupName = azureResourceGroupName;
        return self;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("azureRegion", azureRegion)
                .add("azureIamUser", azureIamUser)
                .add("azureVpcName", azureVpcName)
                .add("azureSubnetName", azureSubnetName)
                .add("azureSecurityGroupName", azureSecurityGroupName)
                .add("azureResourceGroupName", azureResourceGroupName);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
