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

import com.epam.dlab.dto.base.CloudSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AzureCloudSettings extends CloudSettings {

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

    @Builder
    public AzureCloudSettings(String azureRegion, String azureIamUser, String azureVpcName, String azureSubnetName,
                              String azureResourceGroupName, String azureSecurityGroupName) {

        this.azureRegion = azureRegion;
        this.azureIamUser = azureIamUser;
        this.azureVpcName = azureVpcName;
        this.azureSubnetName = azureSubnetName;
        this.azureResourceGroupName = azureResourceGroupName;
        this.azureSecurityGroupName = azureSecurityGroupName;
    }

    @Override
    @JsonIgnore
    public String getIamUser() {
        return azureIamUser;
    }
}
