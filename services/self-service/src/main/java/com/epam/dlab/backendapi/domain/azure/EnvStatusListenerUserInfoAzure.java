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

package com.epam.dlab.backendapi.domain.azure;

import com.epam.dlab.backendapi.domain.EnvStatusListenerUserInfo;
import com.epam.dlab.dto.azure.status.AzureEnvResource;
import com.epam.dlab.utils.UsernameUtils;

public class EnvStatusListenerUserInfoAzure extends EnvStatusListenerUserInfo {
    private AzureEnvResource dto;

    public EnvStatusListenerUserInfoAzure(String username, String accessToken, String azureRegion, String azureResourceGroupName) {
        super(username, accessToken);
        this.dto = new AzureEnvResource()
                .withAzureRegion(azureRegion)
                .withAzureResourceGroupName(azureResourceGroupName)
                .withEdgeUserName(UsernameUtils.removeDomain(username))
                .withAzureIamUser(username);
    }

    public AzureEnvResource getDto() {
        return dto;
    }
}
