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

package com.epam.dlab.backendapi.core.commands;

import com.epam.dlab.backendapi.CloudConfiguration;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.dto.aws.AwsCloudSettings;
import com.epam.dlab.dto.azure.AzureCloudSettings;
import com.epam.dlab.dto.base.CloudSettings;
import com.epam.dlab.dto.gcp.GcpCloudSettings;
import com.epam.dlab.util.JsonGenerator;
import com.epam.dlab.util.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CommandBuilder {

	private final ProvisioningServiceApplicationConfiguration conf;

	@Inject
	public CommandBuilder(ProvisioningServiceApplicationConfiguration conf) {
		this.conf = conf;
	}

	public String buildCommand(RunDockerCommand runDockerCommand, ResourceBaseDTO<?> resourceBaseDTO) throws JsonProcessingException {
		StringBuilder builder = new StringBuilder();
		if (resourceBaseDTO != null) {
			builder.append("echo -e '");
			try {
				resourceBaseDTO.setCloudSettings(getCloudSettings(resourceBaseDTO.getCloudSettings()));
				String str = JsonGenerator.generateJson(resourceBaseDTO);
				log.info("Serialized DTO to: {}", SecurityUtils.hideCreds(str));
				builder.append(str);
			} catch (JsonProcessingException e) {
				log.error("ERROR generating json from dockerRunParameters: {}", e.getMessage());
				throw e;
			}
			builder.append('\'');
			builder.append(" | ");
		}
		builder.append(runDockerCommand.toCMD());
		return builder.toString();
	}

	private CloudSettings getCloudSettings(CloudSettings settings) {
		final CloudProvider cloudProvider = conf.getCloudProvider();
		final CloudConfiguration cloudConfiguration = conf.getCloudConfiguration();
		final CloudConfiguration.LdapConfig ldapConfig = cloudConfiguration.getLdapConfig();
		if (cloudProvider == CloudProvider.AWS) {
			return awsCloudSettings(settings, cloudConfiguration, ldapConfig);
		} else if (cloudProvider == CloudProvider.GCP) {
			return gcpCloudSettings(settings, cloudConfiguration, ldapConfig);
		} else if (cloudProvider == CloudProvider.AZURE) {
			return azureCloudSettings(settings, cloudConfiguration);
		} else {
			throw new UnsupportedOperationException("Unsupported cloud provider " + cloudProvider.getName());
		}
	}

	private AzureCloudSettings azureCloudSettings(CloudSettings settings, CloudConfiguration cloudConfiguration) {
		return AzureCloudSettings.builder()
				.azureRegion(cloudConfiguration.getRegion())
				.azureResourceGroupName(cloudConfiguration.getAzureResourceGroupName())
				.azureSecurityGroupName(cloudConfiguration.getSecurityGroupIds())
				.azureSubnetName(cloudConfiguration.getSubnetId())
				.azureVpcName(cloudConfiguration.getVpcId())
				.confKeyDir(cloudConfiguration.getConfKeyDir())
				.azureIamUser(settings.getIamUser()).build();
	}

	private GcpCloudSettings gcpCloudSettings(CloudSettings settings, CloudConfiguration cloudConfiguration,
											  CloudConfiguration.LdapConfig ldapConfig) {
		return GcpCloudSettings.builder()
				.projectId(cloudConfiguration.getGcpProjectId())
				.vpcName(cloudConfiguration.getVpcId())
				.subnetName(cloudConfiguration.getSubnetId())
				.zone(cloudConfiguration.getZone())
				.region(cloudConfiguration.getRegion())
				.ldapDn(ldapConfig.getDn())
				.ldapHost(ldapConfig.getHost())
				.ldapOu(ldapConfig.getOu())
				.ldapUser(ldapConfig.getUser())
				.ldapPassword(ldapConfig.getPassword())
				.sbn(cloudConfiguration.getServiceBaseName())
				.cloud(conf.getCloudProvider().getName())
				.os(cloudConfiguration.getOs())
				.confKeyDir(cloudConfiguration.getConfKeyDir())
				.gcpIamUser(settings.getIamUser()).build();
	}

	private AwsCloudSettings awsCloudSettings(CloudSettings settings, CloudConfiguration cloudConfiguration,
											  CloudConfiguration.LdapConfig ldapConfig) {
		return AwsCloudSettings.builder()
				.awsRegion(cloudConfiguration.getRegion())
				.awsSecurityGroupIds(cloudConfiguration.getSecurityGroupIds())
				.awsSubnetId(cloudConfiguration.getSubnetId())
				.awsVpcId(cloudConfiguration.getVpcId())
				.confTagResourceId(cloudConfiguration.getConfTagResourceId())
				.awsNotebookSubnetId(cloudConfiguration.getNotebookSubnetId())
				.awsNotebookVpcId(cloudConfiguration.getNotebookVpcId())
				.awsIamUser(settings.getIamUser())
				.zone(cloudConfiguration.getZone())
				.ldapDn(ldapConfig.getDn())
				.ldapHost(ldapConfig.getHost())
				.ldapOu(ldapConfig.getOu())
				.ldapUser(ldapConfig.getUser())
				.ldapPassword(ldapConfig.getPassword())
				.sbn(cloudConfiguration.getServiceBaseName())
				.cloud(conf.getCloudProvider().getName())
				.os(cloudConfiguration.getOs())
				.confKeyDir(cloudConfiguration.getConfKeyDir())
				.build();
	}
}
