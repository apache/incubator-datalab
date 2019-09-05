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

package com.epam.dlab.backendapi.dao;

/**
 * Name of fields in the Mongo collection {@link MongoCollections#SETTINGS}.
 */
public enum MongoSetting {

	// General properties
	/**
	 * Base name of service.
	 */
	SERIVICE_BASE_NAME("conf_service_base_name"),
	/**
	 * Name of directory for user key.
	 */
	CONF_KEY_DIRECTORY("conf_key_dir"),
	/**
	 * Name of resource id.
	 */
	CONF_TAG_RESOURCE_ID("conf_tag_resource_id"),
	/**
	 * Name of OS family.
	 */
	CONF_OS_FAMILY("conf_os_family"),

	CONF_MAX_BUDGET("conf_max_budget"),
	SSN_STORAGE_ACCOUNT_TAG_NAME("ssn_storage_account_tag_name"),
	SHARED_STORAGE_ACCOUNT_TAG_NAME("shared_storage_account_tag_name"),

	LDAP_HOSTNAME("ldap_hostname"),
	LDAP_DN("ldap_dn"),
	LDAP_OU("ldap_ou"),
	LDAP_USER("ldap_service_username"),
	LDAP_PASSWORD("ldap_service_password"),

	PEERING_ID("peering_id"),


	// AWS Related properties
	/**
	 * Name of AWS region.
	 */
	AWS_REGION("aws_region"),
	/**
	 * Id of security group.
	 */
	AWS_SECURITY_GROUPS("aws_security_groups_ids"),
	/**
	 * Id of virtual private cloud for AWS account.
	 */
	AWS_VPC_ID("aws_vpc_id"),
	/**
	 * Id of virtual private cloud subnet for AWS account.
	 */
	AWS_SUBNET_ID("aws_subnet_id"),
	AWS_NOTEBOOK_VPC_ID("aws_notebook_vpc_id"),
	AWS_NOTEBOOK_SUBNET_ID("aws_notebook_subnet_id"),
	AWS_ZONE("aws_zone"),


	// Azure related properties
	AZURE_REGION("azure_region"),
	AZURE_RESOURCE_GROUP_NAME("azure_resource_group_name"),
	AZURE_SUBNET_NAME("azure_subnet_name"),
	AZURE_VPC_NAME("azure_vpc_name"),
	AZURE_SECURITY_GROUP_NAME("azure_security_group_name"),
	AZURE_EDGE_INSTANCE_SIZE("edge_instance_size"),
	AZURE_SSN_INSTANCE_SIZE("ssn_instance_size"),
	AZURE_DATA_LAKE_NAME_TAG("datalake_tag_name"),
	AZURE_DATA_LAKE_CLIENT_ID("azure_client_id"),

	// GCP related properties
	GCP_REGION("gcp_region"),
	GCP_ZONE("gcp_zone"),
	GCP_SUBNET_NAME("gcp_subnet_name"),
	GCP_PROJECT_ID("gcp_project_id"),
	GCP_VPC_NAME("gcp_vpc_name");

	private String id;

	MongoSetting(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}
