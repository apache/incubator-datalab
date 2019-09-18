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

package com.epam.dlab.dto.gcp;

import com.epam.dlab.dto.base.CloudSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class GcpCloudSettings extends CloudSettings {

	@JsonProperty("gcp_iam_user")
	private String gcpIamUser;
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
	@JsonProperty("conf_os_family")
	protected String os;
	@JsonProperty("conf_cloud_provider")
	protected String cloud;
	@JsonProperty("conf_service_base_name")
	protected String sbn;
	@JsonProperty("conf_key_dir")
	protected String confKeyDir;
	@JsonProperty("gcp_project_id")
	protected String projectId;
	@JsonProperty("gcp_vpc_name")
	protected String vpcName;
	@JsonProperty("gcp_subnet_name")
	protected String subnetName;
	@JsonProperty("gcp_zone")
	protected String zone;
	@JsonProperty("gcp_region")
	protected String region;

	@Override
	@JsonIgnore
	public String getIamUser() {
		return gcpIamUser;
	}
}