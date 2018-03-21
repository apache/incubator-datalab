package com.epam.dlab.backendapi.service.aws;

import com.epam.dlab.dto.aws.edge.EdgeInfoAws;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AwsInfrastructureInfoServiceTest {

	@InjectMocks
	private AwsInfrastructureInfoService awsInfrastructureInfoService;

	@Test
	public void getSharedInfo() {
		EdgeInfoAws edgeInfoAws = new EdgeInfoAws();
		edgeInfoAws.setPublicIp("ip");
		edgeInfoAws.setUserOwnBucketName("userOwnBucketName");
		edgeInfoAws.setSharedBucketName("sharedBucketName");

		Map<String, String> expectedMap = new HashMap<>();
		expectedMap.put("edge_node_ip", "ip");
		expectedMap.put("user_own_bicket_name", "userOwnBucketName");
		expectedMap.put("shared_bucket_name", "sharedBucketName");

		Map<String, String> actualMap = awsInfrastructureInfoService.getSharedInfo(edgeInfoAws);
		assertEquals(expectedMap, actualMap);
	}
}
