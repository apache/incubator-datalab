package com.epam.dlab.backendapi.service.azure;

import com.epam.dlab.dto.azure.edge.EdgeInfoAzure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class AzureInfrastructureInfoServiceTest {

	@InjectMocks
	private AzureInfrastructureInfoService azureInfrastructureInfoService;

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

		Map<String, String> actualMap = azureInfrastructureInfoService.getSharedInfo(edgeInfoAzure);
		assertEquals(expectedMap, actualMap);
	}
}
