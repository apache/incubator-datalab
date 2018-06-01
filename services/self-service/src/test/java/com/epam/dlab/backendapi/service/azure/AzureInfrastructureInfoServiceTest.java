/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
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

import com.epam.dlab.dto.azure.edge.EdgeInfoAzure;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AzureInfrastructureInfoServiceTest {

	@Test
	public void getSharedInfo() {
		EdgeInfoAzure edgeInfoAzure = new EdgeInfoAzure();
		edgeInfoAzure.setPublicIp("ip");
		edgeInfoAzure.setUserContainerName("userContainerName");
		edgeInfoAzure.setSharedContainerName("sharedContainerName");
		edgeInfoAzure.setUserStorageAccountName("userStorageAccountName");
		edgeInfoAzure.setSharedStorageAccountName("sharedStorageAccountName");
		edgeInfoAzure.setDataLakeName("datalakeName");
		edgeInfoAzure.setDataLakeDirectoryName("datalakeUserDirectoryName");
		edgeInfoAzure.setDataLakeSharedDirectoryName("datalakeSharedDirectoryName");

		Map<String, String> expectedMap = new HashMap<>();
		expectedMap.put("edge_node_ip", "ip");
		expectedMap.put("user_container_name", "userContainerName");
		expectedMap.put("shared_container_name", "sharedContainerName");
		expectedMap.put("user_storage_account_name", "userStorageAccountName");
		expectedMap.put("shared_storage_account_name", "sharedStorageAccountName");
		expectedMap.put("datalake_name", "datalakeName");
		expectedMap.put("datalake_user_directory_name", "datalakeUserDirectoryName");
		expectedMap.put("datalake_shared_directory_name", "datalakeSharedDirectoryName");

		Map<String, String> actualMap = new AzureInfrastructureInfoService().getSharedInfo(edgeInfoAzure);
		assertEquals(expectedMap, actualMap);
	}
}
