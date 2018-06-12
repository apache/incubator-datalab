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

import com.epam.dlab.backendapi.service.impl.InfrastructureInfoServiceBase;
import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class GcpInfrastructureInfoService extends InfrastructureInfoServiceBase<EdgeInfoGcp> {

	@Override
	protected Map<String, String> getSharedInfo(EdgeInfoGcp edgeInfo) {
		Map<String, String> shared = new HashMap<>();
		shared.put("edge_node_ip", edgeInfo.getPublicIp());
		shared.put("user_own_bucket_name", edgeInfo.getUserOwnBucketName());
		shared.put("shared_bucket_name", edgeInfo.getSharedBucketName());
		return shared;
	}
}
