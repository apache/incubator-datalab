/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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

	private String ldapGroupAttribute;
	private String ldapGroupNameAttribute;
	private String ldapGroupUserAttribute;

	@JsonProperty
	private Request ldapSearchRequest;

	@JsonProperty
	private Request ldapGroupSearchRequest;

	@JsonProperty
	private String awsUserIdentificationEndpoint;
	@JsonProperty
	private String awsUserIdentificationEndpointRegion;

	public SecurityServiceConfiguration() {
		super();
	}

	public String getLdapGroupUserAttribute() {
		return ldapGroupUserAttribute;
	}

	public String getLdapGroupAttribute() {
		return ldapGroupAttribute;
	}

	public String getLdapGroupNameAttribute() {
		return ldapGroupNameAttribute;
	}

	public Request getLdapGroupSearchRequest() {
		return ldapGroupSearchRequest;
	}

	public boolean isUserInfoPersistenceEnabled() {
		return userInfoPersistenceEnabled;
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

	public Request getLdapSearchRequest() {
		return ldapSearchRequest;
	}

	public String getAwsUserIdentificationEndpoint() {
		return awsUserIdentificationEndpoint;
	}

	public String getAwsUserIdentificationEndpointRegion() {
		return awsUserIdentificationEndpointRegion;
	}
}
