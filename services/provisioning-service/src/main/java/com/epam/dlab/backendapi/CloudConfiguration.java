package com.epam.dlab.backendapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CloudConfiguration {

	private final String os;
	private final String serviceBaseName;
	private final String edgeInstanceSize;
	private final String subnetId;
	private final String region;
	private final String zone;
	private final String confTagResourceId;
	private final String securityGroupIds;
	private final String ssnInstanceSize;
	private final String notebookVpcId;
	private final String notebookSubnetId;
	private final String confKeyDir;
	private final String vpcId;
	private final String azureResourceGroupName;
	private final String ssnStorageAccountTagName;
	private final String sharedStorageAccountTagName;
	private final String datalakeTagName;
	private final String azureClientId;
	private final String peeringId;
	private final String gcpProjectId;
	private final boolean imageEnabled;
	private final boolean sharedImageEnabled;
	@JsonProperty("ldap")
	private final LdapConfig ldapConfig;
	private final StepCerts stepCerts;

	@Data
	public static class LdapConfig {
		private final String host;
		private final String dn;
		private final String ou;
		private final String user;
		private final String password;
	}

	@Data
	public static class StepCerts {
		private final boolean enabled;
		private final String rootCA;
		private final String kid;
		private final String kidPassword;
		private final String caURL;
	}
}
