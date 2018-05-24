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

package com.epam.dlab.backendapi.service.gcp;

import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GcpInfrastructureInfoServiceTest {

	@Test
	public void getSharedInfo() {
		EdgeInfoGcp edgeInfoGcp = new EdgeInfoGcp();
		edgeInfoGcp.setPublicIp("ip");
		edgeInfoGcp.setUserOwnBucketName("userOwnBucketName");
		edgeInfoGcp.setSharedBucketName("sharedBucketName");

		Map<String, String> expectedMap = new HashMap<>();
		expectedMap.put("edge_node_ip", "ip");
		expectedMap.put("user_own_bucket_name", "userOwnBucketName");
		expectedMap.put("shared_bucket_name", "sharedBucketName");

		Map<String, String> actualMap = new GcpInfrastructureInfoService().getSharedInfo(edgeInfoGcp);
		assertEquals(expectedMap, actualMap);
	}
}
