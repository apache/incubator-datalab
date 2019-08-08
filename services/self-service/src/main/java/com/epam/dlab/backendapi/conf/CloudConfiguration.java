package com.epam.dlab.backendapi.conf;

import lombok.Data;

@Data
public class CloudConfiguration {

	private final String os;
	private final String serviceBaseName;
	private final String edgeInstanceSize;
	private final String subnetId;
	private final String region;
	private final String confTagResourceId;
	private final String securityGroupIds;
	private final String ssnInstanceSize;
	private final String notebookVpcId;
	private final String notebookSubnetId;
	private final String confKeyDir;
	private final String vpcId;
}
