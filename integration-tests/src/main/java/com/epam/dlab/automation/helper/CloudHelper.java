package com.epam.dlab.automation.helper;

import com.epam.dlab.automation.cloud.aws.AmazonHelper;
import com.epam.dlab.automation.cloud.azure.AzureHelper;

public class CloudHelper {

    public static String getInstancePublicIP(String tagNameValue) throws Exception {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                return AmazonHelper.getInstance(tagNameValue)
                        .getPublicIpAddress();
            case "azure":
                if(AzureHelper.getVirtualMachinesByTag(tagNameValue) != null){
                    return AzureHelper.getVirtualMachinesByTag(tagNameValue).get(0)
                            .getPrimaryPublicIPAddress().toString();
                }
                else return null;
            default:
                return null;
        }
    }

    public static String getInstancePrivateIP(String tagNameValue) throws Exception {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                return AmazonHelper.getInstance(tagNameValue)
                        .getPrivateIpAddress();
            case "azure":
                if(AzureHelper.getVirtualMachinesByTag(tagNameValue) != null){
                    return AzureHelper.getVirtualMachinesByTag(tagNameValue).get(0)
                            .getPrimaryNetworkInterface().primaryPrivateIP();
                }
                else return null;
            default:
                return null;
        }
    }
}
