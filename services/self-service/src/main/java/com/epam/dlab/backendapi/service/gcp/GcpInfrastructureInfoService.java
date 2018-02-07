package com.epam.dlab.backendapi.service.gcp;

import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class GcpInfrastructureInfoService implements InfrastructureInfoService {
    @Override
    public Map<String, String> getSharedInfo(EdgeInfo edgeInfo) {
        return getSharedInfo((EdgeInfoGcp) edgeInfo);
    }

    private Map<String, String> getSharedInfo(EdgeInfoGcp edgeInfo) {
        Map<String, String> shared = new HashMap<>();
        shared.put("edge_node_ip", edgeInfo.getPublicIp());
        shared.put("user_own_bucket_name", edgeInfo.getUserOwnBucketName());
        shared.put("shared_bucket_name", edgeInfo.getSharedBucketName());

        return shared;
    }
}
