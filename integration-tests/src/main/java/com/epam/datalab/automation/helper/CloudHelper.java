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

package com.epam.datalab.automation.helper;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.epam.datalab.automation.cloud.aws.AmazonHelper;
import com.epam.datalab.automation.cloud.azure.AzureHelper;
import com.epam.datalab.automation.cloud.gcp.GcpHelper;
import com.epam.datalab.automation.exceptions.CloudException;
import com.epam.datalab.automation.model.DeployClusterDto;
import com.epam.datalab.automation.model.DeployDataProcDto;
import com.epam.datalab.automation.model.DeployEMRDto;
import com.epam.datalab.automation.model.NotebookConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

public class CloudHelper {

    private CloudHelper(){}

	public static String getInstancePublicIP(String name, boolean restrictionMode) throws IOException {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                return AmazonHelper.getInstance(name).getPublicIpAddress();
            case CloudProvider.AZURE_PROVIDER:
                if(AzureHelper.getVirtualMachinesByName(name, restrictionMode) != null){
                    return AzureHelper.getVirtualMachinesByName(name, restrictionMode).get(0)
                            .getPrimaryPublicIPAddress().ipAddress();
                } else return null;
            case CloudProvider.GCP_PROVIDER:
                List<com.google.api.services.compute.model.Instance> instanceList =
                        GcpHelper.getInstancesByName(name, ConfigPropertyValue.getGcpDataLabProjectId(),
                                restrictionMode,
                                GcpHelper.getAvailableZonesForProject(ConfigPropertyValue.getGcpDataLabProjectId()));
                if (instanceList != null && !GcpHelper.getInstancePublicIps(instanceList.get(0)).isEmpty()) {
                    return GcpHelper.getInstancePublicIps(instanceList.get(0)).get(0);
                } else return null;
            default:
                return null;
        }
    }

	public static String getInstancePrivateIP(String name, boolean restrictionMode) throws IOException {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                return AmazonHelper.getInstance(name).getPrivateIpAddress();
            case CloudProvider.AZURE_PROVIDER:
                if(AzureHelper.getVirtualMachinesByName(name, restrictionMode) != null){
                    return AzureHelper.getVirtualMachinesByName(name, restrictionMode).get(0)
                            .getPrimaryNetworkInterface().primaryPrivateIP();
                } else return null;
            case CloudProvider.GCP_PROVIDER:
                List<com.google.api.services.compute.model.Instance> instanceList =
                        GcpHelper.getInstancesByName(name, ConfigPropertyValue.getGcpDataLabProjectId(), restrictionMode,
                                GcpHelper.getAvailableZonesForProject(ConfigPropertyValue.getGcpDataLabProjectId()));
                if (instanceList != null && !GcpHelper.getInstancePrivateIps(instanceList.get(0)).isEmpty()) {
                    return GcpHelper.getInstancePrivateIps(instanceList.get(0)).get(0);
                } else return null;
            default:
                return null;
        }
    }

	static String getInstanceNameByCondition(String name, boolean restrictionMode) throws IOException {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                Instance instance = AmazonHelper.getInstance(name);
                for (Tag tag : instance.getTags()) {
                    if (tag.getKey().equals("Name")) {
                        return tag.getValue();
                    }
                }
                throw new CloudException("Could not detect name for instance " + name);
            case CloudProvider.AZURE_PROVIDER:
                if (AzureHelper.getVirtualMachinesByName(name, restrictionMode) != null) {
                    return AzureHelper.getVirtualMachinesByName(name, restrictionMode).get(0).name();
                } else return null;
            case CloudProvider.GCP_PROVIDER:
                if (GcpHelper.getInstancesByName(name, ConfigPropertyValue.getGcpDataLabProjectId(), restrictionMode,
                        GcpHelper.getAvailableZonesForProject(ConfigPropertyValue.getGcpDataLabProjectId())) != null) {
                    return GcpHelper.getInstancesByName(name, ConfigPropertyValue.getGcpDataLabProjectId(),
                            restrictionMode,
                            GcpHelper.getAvailableZonesForProject(ConfigPropertyValue.getGcpDataLabProjectId()))
                            .get(0).getName();
                } else return null;
            default:
                return null;
        }
    }

    public static String getClusterConfFileLocation(){
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                return PropertiesResolver.getClusterEC2ConfFileLocation();
            case CloudProvider.AZURE_PROVIDER:
                return PropertiesResolver.getClusterAzureConfFileLocation();
            case CloudProvider.GCP_PROVIDER:
                return PropertiesResolver.getClusterGcpConfFileLocation();
            default:
                return null;
        }
    }


    public static String getPythonTestingScript(){
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                return "/usr/bin/python %s --storage %s --cloud aws --cluster_name %s --os_user %s";
            case CloudProvider.AZURE_PROVIDER:
                if(ConfigPropertyValue.getAzureDatalakeEnabled().equalsIgnoreCase("true")){
                    return "/usr/bin/python %s --storage %s --cloud azure --cluster_name %s --os_user %s --azure_datalake_account "
                            + ConfigPropertyValue.getAzureDatalakeSharedAccount();
                }
                else return "/usr/bin/python %s --storage %s --cloud azure --cluster_name %s --os_user %s --azure_storage_account "
                        + ConfigPropertyValue.getAzureStorageSharedAccount();
            case CloudProvider.GCP_PROVIDER:
                return "/usr/bin/python %s --storage %s --cloud gcp --cluster_name %s --os_user %s";
            default:
                return null;
        }
    }

    public static String getPythonTestingScript2(){
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
				return "/usr/bin/python /home/%s/%s --storage %s --notebook %s --cloud aws";
            case CloudProvider.AZURE_PROVIDER:
                if(ConfigPropertyValue.getAzureDatalakeEnabled().equalsIgnoreCase("true")){
					return "/usr/bin/python /home/%s/%s --storage %s --notebook %s --cloud azure " +
							"--azure_datalake_account " + ConfigPropertyValue.getAzureDatalakeSharedAccount();
                } else return "/usr/bin/python /home/%s/%s --storage %s --notebook %s --cloud azure " +
						"--azure_storage_account " + ConfigPropertyValue.getAzureStorageSharedAccount();
            case CloudProvider.GCP_PROVIDER:
				return "/usr/bin/python /home/%s/%s --storage %s --notebook %s --cloud gcp";
            default:
                return null;
        }
    }

	static String getStorageNameAppendix() {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                return "bucket";
            case CloudProvider.AZURE_PROVIDER:
                if(ConfigPropertyValue.getAzureDatalakeEnabled().equalsIgnoreCase("true")){
                    return "folder";
                }
                else return "container";
            case CloudProvider.GCP_PROVIDER:
                return "bucket";
            default:
                return null;
        }
    }

	public static String getDockerTemplateFileForDES(boolean isSpotRequired) {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
				return isSpotRequired ? "EMR_spot.json" : "EMR.json";
            case CloudProvider.GCP_PROVIDER:
                return "dataproc.json";
            default:
                return null;
        }
    }

    public static Class<? extends DeployClusterDto> getDeployClusterClass() {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                return DeployEMRDto.class;
            case CloudProvider.GCP_PROVIDER:
                return DeployDataProcDto.class;
            default:
                return null;
        }
    }

	public static DeployClusterDto populateDeployClusterDto(DeployClusterDto deployClusterDto,
															NotebookConfig nbConfig) {
		if (nbConfig.getDataEngineType().equals(NamingHelper.DATA_ENGINE_SERVICE) &&
				ConfigPropertyValue.getCloudProvider().equals(CloudProvider.AWS_PROVIDER)) {
			DeployEMRDto emrDto = (DeployEMRDto) deployClusterDto;
			if (!StringUtils.isEmpty(nbConfig.getDesVersion())) {
				emrDto.setEmrVersion(nbConfig.getDesVersion());
			}
			if (nbConfig.isDesSpotRequired() && nbConfig.getDesSpotPrice() > 0) {
				emrDto.setEmrSlaveInstanceSpot(nbConfig.isDesSpotRequired());
				emrDto.setEmrSlaveInstanceSpotPctPrice(nbConfig.getDesSpotPrice());
			}
			return emrDto;
		} else return deployClusterDto;
	}

	static String getGcpDataprocClusterName(String gcpDataprocMasterNodeName) {
        return gcpDataprocMasterNodeName != null ?
                gcpDataprocMasterNodeName.substring(0, gcpDataprocMasterNodeName.lastIndexOf('-')) : null;
	}

}
