/***************************************************************************

 Copyright (c) 2018, EPAM SYSTEMS INC

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

package com.epam.dlab.automation.helper;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.epam.dlab.automation.cloud.CloudException;
import com.epam.dlab.automation.cloud.aws.AmazonHelper;
import com.epam.dlab.automation.cloud.azure.AzureHelper;

public class CloudHelper {

    private CloudHelper(){}

    public static String getInstancePublicIP(String name, boolean restrictionMode) throws CloudException {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                return AmazonHelper.getInstance(name)
                        .getPublicIpAddress();
            case CloudProvider.AZURE_PROVIDER:
                if(AzureHelper.getVirtualMachinesByName(name, restrictionMode) != null){
                    return AzureHelper.getVirtualMachinesByName(name, restrictionMode).get(0)
                            .getPrimaryPublicIPAddress().ipAddress();
                }
                else return null;
            default:
                return null;
        }
    }

    public static String getInstancePrivateIP(String name, boolean restrictionMode) throws CloudException {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                return AmazonHelper.getInstance(name)
                        .getPrivateIpAddress();
            case CloudProvider.AZURE_PROVIDER:
                if(AzureHelper.getVirtualMachinesByName(name, restrictionMode) != null){
                    return AzureHelper.getVirtualMachinesByName(name, restrictionMode).get(0)
                            .getPrimaryNetworkInterface().primaryPrivateIP();
                }
                else return null;
            default:
                return null;
        }
    }

    public static String getInstanceNameByCondition(String name, boolean restrictionMode) throws CloudException {
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
                if(AzureHelper.getVirtualMachinesByName(name, restrictionMode) != null){
                    return AzureHelper.getVirtualMachinesByName(name, restrictionMode).get(0)
                            .name();
                }
                else return null;
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
                return "/usr/bin/python /home/%s/%s --storage %s --cloud aws";
            case CloudProvider.AZURE_PROVIDER:
                if(ConfigPropertyValue.getAzureDatalakeEnabled().equalsIgnoreCase("true")){
                    return "/usr/bin/python /home/%s/%s --storage %s --cloud azure --azure_datalake_account "
                            + ConfigPropertyValue.getAzureDatalakeSharedAccount();
                }
                else return "/usr/bin/python /home/%s/%s --storage %s --cloud azure --azure_storage_account "
                        + ConfigPropertyValue.getAzureStorageSharedAccount();
            case CloudProvider.GCP_PROVIDER:
                return "/usr/bin/python /home/%s/%s --storage %s --cloud gcp";
            default:
                return null;
        }
    }

    public static String getStorageNameAppendix(){
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


}
