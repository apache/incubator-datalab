package com.epam.dlab.automation.cloud;

import com.epam.dlab.automation.cloud.aws.AmazonHelper;
import com.epam.dlab.automation.cloud.aws.AmazonInstanceState;
import com.epam.dlab.automation.cloud.azure.AzureHelper;
import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.microsoft.azure.management.compute.PowerState;
import org.testng.Assert;

public class VirtualMachineStatusChecker {

    public static void checkIfRunning(String tagNameValue) throws Exception {

        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                AmazonHelper.checkAmazonStatus(tagNameValue, AmazonInstanceState.RUNNING);
                break;
            case "azure":
                AzureHelper.checkAzureStatus(tagNameValue, PowerState.RUNNING);
                break;
            default:
                Assert.fail("Unknown cloud provider");
        }

    }

    public static void checkIfTerminated(String tagNameValue) throws Exception {

        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                AmazonHelper.checkAmazonStatus(tagNameValue, AmazonInstanceState.TERMINATED);
                break;
            case "azure":
                AzureHelper.checkAzureStatus(tagNameValue, PowerState.STOPPED);
                break;
            default:
                Assert.fail("Unknown cloud provider");
        }

    }

    public static String getRunningStatus(){
        switch (ConfigPropertyValue.getCloudProvider()) {
            case "aws":
                return AmazonInstanceState.RUNNING.toString();
            case "azure":
                return PowerState.RUNNING.toString();
            default:
                return null;
        }

    }

}
