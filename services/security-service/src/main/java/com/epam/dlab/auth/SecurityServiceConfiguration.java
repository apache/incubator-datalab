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
import com.epam.dlab.auth.dao.Request;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;

import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SecurityServiceConfiguration extends ServiceConfiguration {
    @JsonProperty
    private boolean userInfoPersistenceEnabled = false;
    @JsonProperty
    private boolean awsUserIdentificationEnabled = false;
    @JsonProperty
    @Min(5)
    private int loginAuthenticationTimeout = 10;
    @JsonProperty
    private long inactiveUserTimeoutMillSec;
    @JsonProperty
    private List<Request> ldapSearch;
    @JsonProperty
    private String ldapBindTemplate;
    @JsonProperty
    private String ldapBindAttribute;
    @JsonProperty
    private String ldapSearchAttribute;
    @JsonProperty
    private Map<String, String> ldapConnectionConfig = new HashMap<>();
    private LdapConnectionConfig _ldapConnectionConfig;

    public SecurityServiceConfiguration() {
        super();
    }

    public long getInactiveUserTimeoutMillSec() {
        return inactiveUserTimeoutMillSec;
    }

    public boolean isUserInfoPersistenceEnabled() {
        return userInfoPersistenceEnabled;
    }

    public List<Request> getLdapSearch() {
        return ldapSearch;
    }

    public LdapConnectionConfig getLdapConnectionConfig() {
        if (_ldapConnectionConfig == null) {
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
}
