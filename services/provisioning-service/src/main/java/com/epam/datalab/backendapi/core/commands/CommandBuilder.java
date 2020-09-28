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

package com.epam.datalab.backendapi.core.commands;

import com.epam.datalab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.datalab.backendapi.conf.CloudConfiguration;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.ResourceBaseDTO;
import com.epam.datalab.dto.aws.AwsCloudSettings;
import com.epam.datalab.dto.azure.AzureCloudSettings;
import com.epam.datalab.dto.base.CloudSettings;
import com.epam.datalab.dto.gcp.GcpCloudSettings;
import com.epam.datalab.util.JsonGenerator;
import com.epam.datalab.util.SecurityUtils;
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
        final CloudConfiguration.StepCerts stepCerts = cloudConfiguration.getStepCerts();
        final CloudConfiguration.Keycloak keycloak = cloudConfiguration.getKeycloak();
        if (cloudProvider == CloudProvider.AWS) {
            return awsCloudSettings(settings, cloudConfiguration, ldapConfig, stepCerts, keycloak);
        } else if (cloudProvider == CloudProvider.GCP) {
            return gcpCloudSettings(settings, cloudConfiguration, ldapConfig, stepCerts, keycloak);
        } else if (cloudProvider == CloudProvider.AZURE) {
            return azureCloudSettings(settings, cloudConfiguration, ldapConfig, stepCerts, keycloak);
        } else {
            throw new UnsupportedOperationException("Unsupported cloud provider " + cloudProvider.getName());
        }
    }

    private AzureCloudSettings azureCloudSettings(CloudSettings settings, CloudConfiguration cloudConfiguration,
                                                  CloudConfiguration.LdapConfig ldapConfig,
                                                  CloudConfiguration.StepCerts stepCerts,
                                                  CloudConfiguration.Keycloak keycloak) {
        return AzureCloudSettings.builder()
                .azureRegion(cloudConfiguration.getRegion())
                .azureResourceGroupName(cloudConfiguration.getAzureResourceGroupName())
                .azureSecurityGroupName(cloudConfiguration.getSecurityGroupIds())
                .ldapDn(ldapConfig.getDn())
                .ldapHost(ldapConfig.getHost())
                .ldapOu(ldapConfig.getOu())
                .ldapUser(ldapConfig.getUser())
                .ldapPassword(ldapConfig.getPassword())
                .azureSubnetName(cloudConfiguration.getSubnetId())
                .azureVpcName(cloudConfiguration.getVpcId())
                .confKeyDir(cloudConfiguration.getConfKeyDir())
                .azureIamUser(settings.getIamUser())
                .sbn(cloudConfiguration.getServiceBaseName())
                .os(cloudConfiguration.getOs())
                .cloud(conf.getCloudProvider().getName())
                .imageEnabled(String.valueOf(cloudConfiguration.isImageEnabled()))
                .stepCertsEnabled(String.valueOf(stepCerts.isEnabled()))
                .stepCertsRootCA(stepCerts.getRootCA())
                .stepCertsKid(stepCerts.getKid())
                .stepCertsKidPassword(stepCerts.getKidPassword())
                .stepCertsCAURL(stepCerts.getCaURL())
                .keycloakAuthServerUrl(keycloak.getAuthServerUrl())
                .keycloakRealmName(keycloak.getRealmName())
                .keycloakUser(keycloak.getUser())
                .keycloakUserPassword(keycloak.getUserPassword())
                .build();
    }

    private GcpCloudSettings gcpCloudSettings(CloudSettings settings, CloudConfiguration cloudConfiguration,
                                              CloudConfiguration.LdapConfig ldapConfig,
                                              CloudConfiguration.StepCerts stepCerts,
                                              CloudConfiguration.Keycloak keycloak) {
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
                .gcpIamUser(settings.getIamUser())
                .imageEnabled(String.valueOf(cloudConfiguration.isImageEnabled()))
                .stepCertsEnabled(String.valueOf(stepCerts.isEnabled()))
                .stepCertsRootCA(stepCerts.getRootCA())
                .stepCertsKid(stepCerts.getKid())
                .stepCertsKidPassword(stepCerts.getKidPassword())
                .stepCertsCAURL(stepCerts.getCaURL())
                .keycloakAuthServerUrl(keycloak.getAuthServerUrl())
                .keycloakRealmName(keycloak.getRealmName())
                .keycloakUser(keycloak.getUser())
                .keycloakUserPassword(keycloak.getUserPassword())
                .build();
    }

    private AwsCloudSettings awsCloudSettings(CloudSettings settings, CloudConfiguration cloudConfiguration,
                                              CloudConfiguration.LdapConfig ldapConfig,
                                              CloudConfiguration.StepCerts stepCerts,
                                              CloudConfiguration.Keycloak keycloak) {
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
                .imageEnabled(String.valueOf(cloudConfiguration.isImageEnabled()))
                .stepCertsEnabled(String.valueOf(stepCerts.isEnabled()))
                .stepCertsRootCA(stepCerts.getRootCA())
                .stepCertsKid(stepCerts.getKid())
                .stepCertsKidPassword(stepCerts.getKidPassword())
                .stepCertsCAURL(stepCerts.getCaURL())
                .keycloakAuthServerUrl(keycloak.getAuthServerUrl())
                .keycloakRealmName(keycloak.getRealmName())
                .keycloakUser(keycloak.getUser())
                .keycloakUserPassword(keycloak.getUserPassword())
                .build();
    }
}
