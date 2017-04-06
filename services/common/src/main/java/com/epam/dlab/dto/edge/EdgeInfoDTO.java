/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.dto.edge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EdgeInfoDTO {
	public static final String USER_OWN_BUCKET_NAME = "user_own_bicket_name";
	
    @JsonProperty("instance_id")
    private String instanceId;
    @JsonProperty
    private String hostname;
    @JsonProperty("public_ip")
    private String publicIp;
    @JsonProperty("ip")
    private String privateIp;
    @JsonProperty("key_name")
    private String keyName;
    @JsonProperty(USER_OWN_BUCKET_NAME)
    private String userOwnBucketName;
    @JsonProperty("tunnel_port")
    private String tunnelPort;
    @JsonProperty("socks_port")
    private String socksPort;
    @JsonProperty("notebook_sg")
    private String notebookSg;
    @JsonProperty("notebook_profile")
    private String notebookProfile;
    @JsonProperty("notebook_subnet")
    private String notebookSubnet;
    @JsonProperty("edge_sg")
    private String edgeSG;
    @JsonProperty("edge_status")
    private String edgeStatus;

    public String getInstanceId() {
    	return instanceId;
    }
    
    public String getPublicIp() {
        return publicIp;
    }
    
    public String getPrivateIp() {
        return privateIp;
    }
    
    public String getUserOwnBucketName() {
    	return userOwnBucketName;
    }

	public String getEdgeStatus() {
		return edgeStatus;
	}
	
	public EdgeInfoDTO withEdgeStatus(String edgeStatus) {
		this.edgeStatus = edgeStatus;
		return this;
	}

    public ToStringHelper toStringHelper(Object self) {
    	return MoreObjects.toStringHelper(self)
    			.add("instanceId", instanceId)
    			.add("hostname", hostname)
    			.add("publicIp", publicIp)
    			.add("privateIp", privateIp)
    			.add("keyName", keyName)
    			.add("userOwnBucketName", userOwnBucketName)
    			.add("tunnelPort", tunnelPort)
    			.add("socksPort", socksPort)
    			.add("notebookSg", notebookSg)
    			.add("notebookProfile", notebookProfile)
    			.add("notebookSubnet", notebookSubnet)
    			.add("edgeSG", edgeSG)
    			.add("edgeStatus", edgeStatus);
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
