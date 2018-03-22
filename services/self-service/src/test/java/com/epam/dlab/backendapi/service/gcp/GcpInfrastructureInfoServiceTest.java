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
