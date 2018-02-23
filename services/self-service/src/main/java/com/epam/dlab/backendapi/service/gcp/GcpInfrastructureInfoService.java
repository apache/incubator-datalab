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
