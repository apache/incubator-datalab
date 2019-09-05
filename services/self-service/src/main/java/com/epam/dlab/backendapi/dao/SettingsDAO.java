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

import com.epam.dlab.exceptions.DlabException;
import com.mongodb.client.model.UpdateOptions;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.dlab.backendapi.dao.MongoCollections.SETTINGS;
import static com.epam.dlab.backendapi.dao.MongoSetting.*;
import static com.mongodb.client.model.Filters.eq;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Stores the environment settings.
 */
public class SettingsDAO extends BaseDAO {
	private static final String VALUE = "value";

	/**
	 * Returns the base name of service.
	 */
	public String getServiceBaseName() {
		return getSetting(SERIVICE_BASE_NAME);
	}

	public void setServiceBaseName(String sbn) {
		setSetting(SERIVICE_BASE_NAME, sbn);
	}

	/**
	 * Returns the name of OS family.
	 */
	public String getConfOsFamily() {
		return getSetting(CONF_OS_FAMILY);
	}

	public void setConfOsFamily(String osFamily) {
		setSetting(CONF_OS_FAMILY, osFamily);
	}

	/**
	 * Returns the name of directory for user key.
	 */
	public String getConfKeyDir() {
		return getSetting(CONF_KEY_DIRECTORY);
	}

	public void setConfKeyDir(String confKeyDir) {
		setSetting(CONF_KEY_DIRECTORY, confKeyDir);
	}

	/**
	 * Returns the name of tag for resource id.
	 */
	public String getConfTagResourceId() {
		return getSetting(CONF_TAG_RESOURCE_ID);
	}

	public void setConfTagResourceId(String confTagResourceId) {
		setSetting(CONF_TAG_RESOURCE_ID, confTagResourceId);
	}

	public Optional<Integer> getMaxBudget() {
		return getOptionalSetting(CONF_MAX_BUDGET)
				.map(Integer::valueOf);

	}

	public String getAwsZone() {
		return getSetting(AWS_ZONE);
	}

	public void setAwsZone(String awsZone) {
		setSetting(AWS_ZONE, awsZone);
	}

	public String getLdapHost() {
		return getSetting(LDAP_HOSTNAME);
	}

	public void setLdapHost(String ldapHost) {
		setSetting(LDAP_HOSTNAME, ldapHost);
	}

	public String getLdapOu() {
		return getSetting(LDAP_OU);
	}

	public void setLdapOu(String ldapOu) {
		setSetting(LDAP_OU, ldapOu);
	}

	public String getLdapDn() {
		return getSetting(LDAP_DN);
	}

	public void setLdapDn(String ldapDn) {
		setSetting(LDAP_DN, ldapDn);
	}

	public String getLdapUser() {
		return getSetting(LDAP_USER);
	}

	public void setLdapUser(String user) {
		setSetting(LDAP_USER, user);
	}

	public String getLdapPassword() {
		return getSetting(LDAP_PASSWORD);
	}

	public void setLdapPassword(String ldapPassword) {
		setSetting(LDAP_PASSWORD, ldapPassword);
	}

	/**
	 * Returns the name of AWS region.
	 */
	public String getAwsRegion() {
		return getSetting(AWS_REGION);
	}

	public void setAwsRegion(String awsRegion) {
		setSetting(AWS_REGION, awsRegion);
	}

	/**
	 * Returns the id of security group.
	 */
	public String getAwsSecurityGroups() {
		return getSetting(AWS_SECURITY_GROUPS);
	}

	public void setAwsSecurityGroups(String awsSecurityGroups) {
		setSetting(AWS_SECURITY_GROUPS, awsSecurityGroups);
	}

	/**
	 * Returns the id of virtual private cloud for AWS account.
	 */
	public String getAwsVpcId() {
		return getSetting(AWS_VPC_ID);
	}

	public void setAwsVpcId(String awsVpcId) {
		setSetting(AWS_VPC_ID, awsVpcId);
	}

	/**
	 * Returns the id of virtual private cloud subnet for AWS account.
	 */
	public void setAwsSubnetId(String awsSubnetId) {
		setSetting(AWS_SUBNET_ID, awsSubnetId);
	}

	public String getAwsSubnetId() {
		return getSetting(AWS_SUBNET_ID);
	}

	public String getAwsNotebookVpcId() {
		return getSetting(AWS_NOTEBOOK_VPC_ID);
	}

	public void setSsnStorageAccountTagName(String ssnStorageAccountTagName) {
		setSetting(SSN_STORAGE_ACCOUNT_TAG_NAME, ssnStorageAccountTagName);
	}

	public String getSsnStorageAccountTagName() {
		return getSetting(SSN_STORAGE_ACCOUNT_TAG_NAME);
	}

	public void setSharedStorageAccountTagName(String sharedStorageAccountTagName) {
		setSetting(SHARED_STORAGE_ACCOUNT_TAG_NAME, sharedStorageAccountTagName);
	}

	public String getSharedStorageAccountTagName() {
		return getSetting(SHARED_STORAGE_ACCOUNT_TAG_NAME);
	}

	public void setPeeringId(String peeringId) {
		setSetting(PEERING_ID, peeringId);
	}

	public void setAwsNotebookVpcId(String awsNotebookVpcId) {
		setSetting(AWS_NOTEBOOK_VPC_ID, awsNotebookVpcId);
	}

	public String getAwsNotebookSubnetId() {
		return getSetting(AWS_NOTEBOOK_SUBNET_ID);
	}

	public void setAwsNotebookSubnetId(String awsNotebookSubnetId) {
		setSetting(AWS_NOTEBOOK_SUBNET_ID, awsNotebookSubnetId);
	}

	public String getAzureRegion() {
		return getSetting(AZURE_REGION);
	}

	public String getAzureResourceGroupName() {
		return getSetting(AZURE_RESOURCE_GROUP_NAME);
	}

	public String getAzureSubnetName() {
		return getSetting(AZURE_SUBNET_NAME);
	}

	public String getAzureVpcName() {
		return getSetting(AZURE_VPC_NAME);
	}

	public String getAzureSecurityGroupName() {
		return getSetting(AZURE_SECURITY_GROUP_NAME);
	}

	public String getAzureEdgeInstanceSize() {
		return getSetting(AZURE_EDGE_INSTANCE_SIZE);
	}

	public String getAzureSsnInstanceSize() {
		return getSetting(AZURE_SSN_INSTANCE_SIZE);
	}

	public String getAzureDataLakeNameTag() {
		return getSetting(AZURE_DATA_LAKE_NAME_TAG, "");
	}

	public boolean isAzureDataLakeEnabled() {
		String dataLakeTagName = getAzureDataLakeNameTag();
		return dataLakeTagName != null && !dataLakeTagName.isEmpty();
	}

	public String getAzureDataLakeClientId() {
		return getSetting(AZURE_DATA_LAKE_CLIENT_ID);
	}

	public void setAzureRegion(String region) {
		setSetting(AZURE_REGION, region);
	}

	public void setAzureResourceGroupName(String resourceGroupName) {
		setSetting(AZURE_RESOURCE_GROUP_NAME, resourceGroupName);
	}

	public void setAzureSubnetName(String subnetName) {
		setSetting(AZURE_SUBNET_NAME, subnetName);
	}

	public void setAzureVpcName(String vpcName) {
		setSetting(AZURE_VPC_NAME, vpcName);
	}

	public void setAzureSecurityGroupName(String securityGroupName) {
		setSetting(AZURE_SECURITY_GROUP_NAME, securityGroupName);
	}

	public void setAzureEdgeInstanceSize(String azureEdgeInstanceSize) {
		setSetting(AZURE_EDGE_INSTANCE_SIZE, azureEdgeInstanceSize);
	}

	public void setAzureSsnInstanceSize(String ssnInstanceSize) {
		setSetting(AZURE_SSN_INSTANCE_SIZE, ssnInstanceSize);
	}

	public void setAzureDataLakeNameTag(String dataLakeNameTag) {
		setSetting(AZURE_DATA_LAKE_NAME_TAG, dataLakeNameTag);
	}

	public void setAzureDataLakeClientId(String dataLakeClientId) {
		setSetting(AZURE_DATA_LAKE_CLIENT_ID, dataLakeClientId);
	}

	public String getGcpRegion() {
		return getSetting(GCP_REGION);
	}

	public String getGcpZone() {
		return getSetting(GCP_ZONE);
	}

	public String getGcpSubnetName() {
		return getSetting(GCP_SUBNET_NAME);
	}

	public String getGcpProjectId() {
		return getSetting(GCP_PROJECT_ID);
	}

	public String getGcpVpcName() {
		return getSetting(GCP_VPC_NAME);
	}

	public void setMaxBudget(Long budget) {
		setSetting(CONF_MAX_BUDGET, budget.toString());
	}

	public void removeSetting(MongoSetting setting) {
		getCollection(SETTINGS).deleteOne(eq(ID, setting.getId()));
	}

	public Map<String, Object> getSettings() {
		return stream(getCollection(SETTINGS).find())
				.collect(Collectors.toMap(d -> d.getString(ID), d -> d.get(VALUE)));
	}

	/**
	 * Returns the value of property from Mongo database.
	 *
	 * @param setting the name of property.
	 */
	private String getSetting(MongoSetting setting) {
		Document d = settingDocument(setting);
		if (d == null) {
			throw new DlabException("Setting property " + setting + " not found");
		}
		return d.getOrDefault(VALUE, EMPTY).toString();
	}

	private Optional<String> getOptionalSetting(MongoSetting setting) {
		Document d = settingDocument(setting);
		return Optional.ofNullable(d).map(doc -> doc.getString(VALUE));
	}

	private Document settingDocument(MongoSetting setting) {
		return mongoService
				.getCollection(SETTINGS)
				.find(eq(ID, setting.getId()))
				.first();
	}

	private void setSetting(MongoSetting mongoSetting, String value) {
		if (StringUtils.isNotEmpty(value)) {
			mongoService.getCollection(SETTINGS)
					.updateOne(eq(ID, mongoSetting.getId()), new Document("$set", new Document(VALUE, value)),
							new UpdateOptions().upsert(true));
		}
	}


	private String getSetting(MongoSetting setting, String defaultValue) {
		Document d = settingDocument(setting);
		if (d == null) {
			return defaultValue;
		}
		return d.getOrDefault(VALUE, defaultValue).toString();
	}
}
