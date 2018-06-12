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

package com.epam.dlab.automation.cloud;

import com.epam.dlab.automation.cloud.aws.AmazonHelper;
import com.epam.dlab.automation.cloud.aws.AmazonInstanceState;
import com.epam.dlab.automation.cloud.azure.AzureHelper;
import com.epam.dlab.automation.cloud.gcp.GcpHelper;
import com.epam.dlab.automation.cloud.gcp.GcpInstanceState;
import com.epam.dlab.automation.helper.CloudProvider;
import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.microsoft.azure.management.compute.PowerState;
import org.testng.Assert;

import java.io.IOException;

public class VirtualMachineStatusChecker {

	private static final String UNKNOWN_CLOUD_PROVIDER = "Unknown cloud provider";

	private VirtualMachineStatusChecker(){}

    public static void checkIfRunning(String tagNameValue, boolean restrictionMode)
			throws InterruptedException, IOException {

        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                AmazonHelper.checkAmazonStatus(tagNameValue, AmazonInstanceState.RUNNING);
                break;
            case CloudProvider.AZURE_PROVIDER:
                AzureHelper.checkAzureStatus(tagNameValue, PowerState.RUNNING, restrictionMode);
                break;
            case CloudProvider.GCP_PROVIDER:
                GcpHelper.checkGcpStatus(tagNameValue, ConfigPropertyValue.getGcpDlabProjectId(),
                        GcpInstanceState.RUNNING, restrictionMode,
                        GcpHelper.getAvailableZonesForProject(ConfigPropertyValue.getGcpDlabProjectId()));
                break;
            default:
                Assert.fail(UNKNOWN_CLOUD_PROVIDER);
        }

    }

    public static void checkIfTerminated(String tagNameValue, boolean restrictionMode)
			throws InterruptedException, IOException {

        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                AmazonHelper.checkAmazonStatus(tagNameValue, AmazonInstanceState.TERMINATED);
                break;
            case CloudProvider.AZURE_PROVIDER:
                AzureHelper.checkAzureStatus(tagNameValue, PowerState.STOPPED, restrictionMode);
                break;
            case CloudProvider.GCP_PROVIDER:
                GcpHelper.checkGcpStatus(tagNameValue, ConfigPropertyValue.getGcpDlabProjectId(),
                        GcpInstanceState.TERMINATED, restrictionMode,
                        GcpHelper.getAvailableZonesForProject(ConfigPropertyValue.getGcpDlabProjectId()));
                break;
            default:
                Assert.fail(UNKNOWN_CLOUD_PROVIDER);
        }

    }

    public static void checkIfStopped(String tagNameValue, boolean restrictionMode)
			throws InterruptedException, IOException {

        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                AmazonHelper.checkAmazonStatus(tagNameValue, AmazonInstanceState.STOPPED);
                break;
            case CloudProvider.AZURE_PROVIDER:
                AzureHelper.checkAzureStatus(tagNameValue, PowerState.DEALLOCATED, restrictionMode);
                break;
            case CloudProvider.GCP_PROVIDER:
                GcpHelper.checkGcpStatus(tagNameValue, ConfigPropertyValue.getGcpDlabProjectId(),
                        GcpInstanceState.TERMINATED, restrictionMode,
                        GcpHelper.getAvailableZonesForProject(ConfigPropertyValue.getGcpDlabProjectId()));
                break;
            default:
                Assert.fail(UNKNOWN_CLOUD_PROVIDER);
        }

    }

    public static String getStartingStatus() {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                return AmazonInstanceState.STARTING.toString();
            case CloudProvider.AZURE_PROVIDER:
                return PowerState.STARTING.toString();
            case CloudProvider.GCP_PROVIDER:
				return GcpInstanceState.STARTING.toString();
            default:
                return "";
        }

    }

    public static String getRunningStatus(){
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
                return AmazonInstanceState.RUNNING.toString();
            case CloudProvider.AZURE_PROVIDER:
                return PowerState.RUNNING.toString();
            case CloudProvider.GCP_PROVIDER:
                return GcpInstanceState.RUNNING.toString();
            default:
                return "";
        }

    }

}
