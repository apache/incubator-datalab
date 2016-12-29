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

package com.epam.dlab.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.dao.Request;
import com.epam.dlab.ServiceConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SecurityServiceConfiguration extends ServiceConfiguration {

	protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

	public SecurityServiceConfiguration() {
		super();
	}
	
	private boolean userInfoPersistenceEnabled = false;

	@JsonProperty
	private boolean awsUserIdentificationEnabled = false;
	
	@JsonProperty
	private long inactiveUserTimeoutMillSec;
	
	public long getInactiveUserTimeoutMillSec() {
		return inactiveUserTimeoutMillSec;
	}

	@JsonProperty
	public boolean isUserInfoPersistenceEnabled() {
		return userInfoPersistenceEnabled;
	}

	@JsonProperty
	private List<Request> ldapSearch;
	
	public List<Request> getLdapSearch() {
		return ldapSearch;
	}

	@JsonProperty
	private String ldapBindTemplate;
	
	@JsonProperty
	private Map<String,String> ldapConnectionConfig = new HashMap<String, String>();
	private LdapConnectionConfig _ldapConnectionConfig;
	
	public LdapConnectionConfig getLdapConnectionConfig() {
		if(_ldapConnectionConfig == null) {
			_ldapConnectionConfig = new LdapConnectionConfig();
			_ldapConnectionConfig.setLdapHost(ldapConnectionConfig.get("ldapHost"));
			_ldapConnectionConfig.setLdapPort(Integer.parseInt(ldapConnectionConfig.get("ldapPort")));
			_ldapConnectionConfig.setName(ldapConnectionConfig.get("name"));
			_ldapConnectionConfig.setCredentials(ldapConnectionConfig.get("credentials"));
			//TODO: add all configurable properties
			//      from the LdapConnectionConfig class
		}
		return _ldapConnectionConfig;
		
	}

	public String getLdapBindTemplate() {
		return ldapBindTemplate;
	}
	
	public boolean isAwsUserIdentificationEnabled() {
		return awsUserIdentificationEnabled;
	}
}
