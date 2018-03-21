package com.epam.dlab.backendapi.service.gcp;

import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class GcpInfrastructureInfoServiceTest {

	@InjectMocks
	private GcpInfrastructureInfoService gcpInfrastructureInfoService;

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

		Map<String, String> actualMap = gcpInfrastructureInfoService.getSharedInfo(edgeInfoGcp);
		assertEquals(expectedMap, actualMap);
	}
}
