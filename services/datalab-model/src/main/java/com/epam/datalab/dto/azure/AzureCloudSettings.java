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

package com.epam.datalab.dto.azure;

import com.epam.datalab.dto.base.CloudSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AzureCloudSettings extends CloudSettings {

    @JsonProperty("azure_region")
    private String azureRegion;
    @JsonProperty("azure_iam_user")
    private String azureIamUser;
    @JsonProperty("conf_service_base_name")
    protected String sbn;
    @JsonProperty("conf_os_family")
    protected String os;
    @JsonProperty("conf_cloud_provider")
    protected String cloud;
    @JsonProperty("azure_vpc_name")
    private String azureVpcName;
    @JsonProperty("azure_subnet_name")
    private String azureSubnetName;
    @JsonProperty("azure_resource_group_name")
    private String azureResourceGroupName;
    @JsonProperty("azure_security_group_name")
    private String azureSecurityGroupName;
    @JsonProperty("ldap_hostname")
    protected String ldapHost;
    @JsonProperty("ldap_dn")
    protected String ldapDn;
    @JsonProperty("ldap_ou")
    protected String ldapOu;
    @JsonProperty("ldap_service_username")
    protected String ldapUser;
    @JsonProperty("ldap_service_password")
    protected String ldapPassword;
    @JsonProperty("conf_key_dir")
    protected String confKeyDir;
    @JsonProperty("conf_image_enabled")
    private String imageEnabled;
    @JsonProperty("conf_stepcerts_enabled")
    private String stepCertsEnabled;
    @JsonProperty("conf_stepcerts_root_ca")
    private String stepCertsRootCA;
    @JsonProperty("conf_stepcerts_kid")
    private String stepCertsKid;
    @JsonProperty("conf_stepcerts_kid_password")
    private String stepCertsKidPassword;
    @JsonProperty("conf_stepcerts_ca_url")
    private String stepCertsCAURL;
    @JsonProperty("keycloak_auth_server_url")
    private String keycloakAuthServerUrl;
    @JsonProperty("keycloak_realm_name")
    private String keycloakRealmName;
    @JsonProperty("keycloak_user")
    private String keycloakUser;
    @JsonProperty("keycloak_user_password")
    private String keycloakUserPassword;

    @Override
    @JsonIgnore
    public String getIamUser() {
        return azureIamUser;
    }
}
