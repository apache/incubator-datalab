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

package com.epam.dlab.backendapi.service.azure;

import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.dto.azure.edge.EdgeInfoAzure;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class AzureInfrastructureInfoService implements InfrastructureInfoService {
    @Override
    public Map<String, String> getSharedInfo(EdgeInfo edgeInfo) {
        return getSharedInfo((EdgeInfoAzure) edgeInfo);
    }

    private Map<String, String> getSharedInfo(EdgeInfoAzure edgeInfo) {
        Map<String, String> shared = new HashMap<>();
        shared.put("edge_node_ip", edgeInfo.getPublicIp());
        shared.put("user_container_name", edgeInfo.getUserContainerName());
        shared.put("shared_container_name", edgeInfo.getSharedContainerName());
        shared.put("user_storage_account_name", edgeInfo.getUserStorageAccountName());
        shared.put("shared_storage_account_name", edgeInfo.getSharedStorageAccountName());
        shared.put("datalake_name", edgeInfo.getDataLakeName());
        shared.put("datalake_user_directory_name", edgeInfo.getDataLakeDirectoryName());
        shared.put("datalake_shared_directory_name", edgeInfo.getDataLakeSharedDirectoryName());

        return shared;
    }
}
