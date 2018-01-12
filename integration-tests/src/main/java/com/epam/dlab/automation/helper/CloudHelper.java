package com.epam.dlab.automation.helper;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.epam.dlab.automation.cloud.aws.AmazonHelper;
import com.epam.dlab.automation.cloud.azure.AzureHelper;

public class CloudHelper {

    public static String getInstancePublicIP(String tagNameValue, boolean restrictionMode) throws Exception {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                return AmazonHelper.getInstance(tagNameValue)
                        .getPublicIpAddress();
            case "azure":
                if(AzureHelper.getVirtualMachinesByTag(tagNameValue, restrictionMode) != null){
                    return AzureHelper.getVirtualMachinesByTag(tagNameValue, restrictionMode).get(0)
                            .getPrimaryPublicIPAddress().ipAddress();
                }
                else return null;
            default:
                return null;
        }
    }

    public static String getInstancePrivateIP(String tagNameValue, boolean restrictionMode) throws Exception {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                return AmazonHelper.getInstance(tagNameValue)
                        .getPrivateIpAddress();
            case "azure":
                if(AzureHelper.getVirtualMachinesByTag(tagNameValue, restrictionMode) != null){
                    return AzureHelper.getVirtualMachinesByTag(tagNameValue, restrictionMode).get(0)
                            .getPrimaryNetworkInterface().primaryPrivateIP();
                }
                else return null;
            default:
                return null;
        }
    }

    public static String getInstanceNameByTag(String tagNameValue, boolean restrictionMode) throws Exception {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                Instance instance = AmazonHelper.getInstance(tagNameValue);
                for (Tag tag : instance.getTags()) {
                    if (tag.getKey().equals("Name")) {
                        return tag.getValue();
                    }
                }
                throw new Exception("Could not detect name for instance " + tagNameValue);
            case "azure":
                if(AzureHelper.getVirtualMachinesByTag(tagNameValue, restrictionMode) != null){
                    return AzureHelper.getVirtualMachinesByTag(tagNameValue, restrictionMode).get(0)
                            .name();
                }
                else return null;
            default:
                return null;
        }
    }

    public static String getClusterConfFileLocation(){
        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                return PropertiesResolver.getClusterEC2ConfFileLocation();
            case "azure":
                return PropertiesResolver.getClusterAzureConfFileLocation();
            default:
                return null;
        }
    }

    public static String getPythonTestingScript(){
        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                return "/usr/bin/python %s --storage %s --cloud aws --cluster_name %s --os_user %s";
            case "azure":
                if(ConfigPropertyValue.getAzureDatalakeEnabled().equalsIgnoreCase("true")){
                    return "/usr/bin/python %s --storage %s --cloud azure --cluster_name %s --os_user %s --azure_datalake_account "
                            + ConfigPropertyValue.getAzureDatalakeSharedAccount();
                }
                else return "/usr/bin/python %s --storage %s --cloud azure --cluster_name %s --os_user %s --azure_storage_account "
                        + ConfigPropertyValue.getAzureStorageSharedAccount();
            case "gcp":
                return "/usr/bin/python %s --storage %s --cloud gcp --cluster_name %s --os_user %s";
            default:
                return null;
        }
    }

    public static String getPythonTestingScript2(){
        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                return "/usr/bin/python /home/%s/%s --storage %s --cloud aws";
            case "azure":
                if(ConfigPropertyValue.getAzureDatalakeEnabled().equalsIgnoreCase("true")){
                    return "/usr/bin/python /home/%s/%s --storage %s --cloud azure --azure_datalake_account "
                            + ConfigPropertyValue.getAzureDatalakeSharedAccount();
                }
                else return "/usr/bin/python /home/%s/%s --storage %s --cloud azure --azure_storage_account "
                        + ConfigPropertyValue.getAzureStorageSharedAccount();
            case "gcp":
                return "/usr/bin/python /home/%s/%s --storage %s --cloud gcp";
            default:
                return null;
        }
    }

    public static String getStorageNameAppendix(){
        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                return "bucket";
            case "azure":
                if(ConfigPropertyValue.getAzureDatalakeEnabled().equalsIgnoreCase("true")){
                    return "folder";
                }
                else return "container";
            case "gcp":
                return "bucket";
            default:
                return null;
        }
    }


}
