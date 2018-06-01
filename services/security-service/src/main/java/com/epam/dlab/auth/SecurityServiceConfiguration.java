/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
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


package com.epam.dlab.auth;

import com.epam.dlab.ServiceConfiguration;
import com.epam.dlab.auth.conf.AzureLoginConfiguration;
import com.epam.dlab.auth.dao.Request;
import com.epam.dlab.config.gcp.GcpLoginConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;

import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecurityServiceConfiguration extends ServiceConfiguration {
	@JsonProperty
	private boolean userInfoPersistenceEnabled = false;
	@JsonProperty
	private boolean awsUserIdentificationEnabled = false;
	@JsonProperty
	private boolean ldapUseConnectionPool = false;
	@JsonProperty
	@Min(5)
	private int loginAuthenticationTimeout = 10;
	@JsonProperty
	private List<Request> ldapSearch;
	@JsonProperty
	private String ldapBindTemplate;
	@JsonProperty
	private String ldapBindAttribute;
	@JsonProperty
	private String ldapSearchAttribute;
	@JsonProperty
	private boolean useLdapBindTemplate;
	@JsonProperty
	private Map<String, String> ldapConnectionConfig = new HashMap<>();
	@JsonProperty
	private AzureLoginConfiguration azureLoginConfiguration;
	@JsonProperty
	private GcpLoginConfiguration gcpLoginConfiguration;

	private LdapConnectionConfig ldapConfiguration;

	public SecurityServiceConfiguration() {
		super();
	}

	public boolean isUserInfoPersistenceEnabled() {
		return userInfoPersistenceEnabled;
	}

	public List<Request> getLdapSearch() {
		return ldapSearch;
	}

	public LdapConnectionConfig getLdapConnectionConfig() {
		if (ldapConfiguration == null) {
			ldapConfiguration = new LdapConnectionConfig();
			ldapConfiguration.setLdapHost(ldapConnectionConfig.get("ldapHost"));
			ldapConfiguration.setLdapPort(Integer.parseInt(ldapConnectionConfig.get("ldapPort")));
			ldapConfiguration.setName(ldapConnectionConfig.get("name"));
			ldapConfiguration.setCredentials(ldapConnectionConfig.get("credentials"));
		}
		return ldapConfiguration;

	}

	public String getLdapBindTemplate() {
		return ldapBindTemplate;
	}

	public String getLdapBindAttribute() {
		return ldapBindAttribute;
	}

	public String getLdapSearchAttribute() {
		return ldapSearchAttribute;
	}

	public boolean isAwsUserIdentificationEnabled() {
		return awsUserIdentificationEnabled;
	}

	public int getLoginAuthenticationTimeout() {
		return loginAuthenticationTimeout;
	}

	public boolean isLdapUseConnectionPool() {
		return ldapUseConnectionPool;
	}

	public AzureLoginConfiguration getAzureLoginConfiguration() {
		return azureLoginConfiguration;
	}

	public boolean isUseLdapBindTemplate() {
		return useLdapBindTemplate;
	}

	public GcpLoginConfiguration getGcpLoginConfiguration() {
		return gcpLoginConfiguration;
	}
}
