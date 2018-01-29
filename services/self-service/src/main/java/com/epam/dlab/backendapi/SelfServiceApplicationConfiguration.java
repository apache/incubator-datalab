/***************************************************************************

 Copyright (c) 2016, EPAM SYSTEMS INC

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

package com.epam.dlab.backendapi;

import com.epam.dlab.ServiceConfiguration;
import com.epam.dlab.backendapi.validation.SelfServiceCloudConfigurationSequenceProvider;
import com.epam.dlab.config.azure.AzureLoginConfiguration;
import com.epam.dlab.validation.AwsValidation;
import com.epam.dlab.validation.AzureValidation;
import com.epam.dlab.validation.GcpValidation;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * Configuration for Self Service.
 */
@GroupSequenceProvider(SelfServiceCloudConfigurationSequenceProvider.class)
public class SelfServiceApplicationConfiguration extends ServiceConfiguration {

    @Min(value = 2, groups = AwsValidation.class)
    @JsonProperty
    private int minEmrInstanceCount;

    @Max(value = 1000, groups = AwsValidation.class)
    @JsonProperty
    private int maxEmrInstanceCount;

    @Min(value = 10, groups = AwsValidation.class)
    @JsonProperty
    private int minEmrSpotInstanceBidPct;

    @Max(value = 95, groups = AwsValidation.class)
    @JsonProperty
    private int maxEmrSpotInstanceBidPct;

    @Min(value = 2, groups = {AzureValidation.class, AwsValidation.class, GcpValidation.class})
    @JsonProperty
    private int minSparkInstanceCount;

    @Max(value = 1000, groups = {AzureValidation.class, AwsValidation.class, GcpValidation.class})
    @JsonProperty
    private int maxSparkInstanceCount;

    @JsonProperty
    private AzureLoginConfiguration azureLoginConfiguration;

    @JsonProperty
    private boolean rolePolicyEnabled = false;

    @JsonProperty
    private boolean roleDefaultAccess = false;

    @JsonProperty
    private Duration checkEnvStatusTimeout = Duration.minutes(10);

    @JsonProperty
    private boolean billingSchedulerEnabled = false;

    @NotEmpty(groups = AwsValidation.class)
    @JsonProperty
    private String billingConfFile;

    @JsonProperty
    private List<Integer> dataprocAvailableMasterInstanceCount;
    @JsonProperty
    private int minDataprocSlaveInstanceCount;
    @JsonProperty
    private int maxDataprocSlaveInstanceCount;
    @JsonProperty
    private int minDataprocPreemptibleCount;

    /**
     * Returns the minimum number of slave EMR instances than could be created.
     */
    public int getMinEmrInstanceCount() {
        return minEmrInstanceCount;
    }

    /**
     * Returns the maximum number of slave EMR instances than could be created.
     */
    public int getMaxEmrInstanceCount() {
        return maxEmrInstanceCount;
    }

    /**
     * Returns the timeout for check the status of environment via provisioning service.
     */
    public Duration getCheckEnvStatusTimeout() {
        return checkEnvStatusTimeout;
    }

    public int getMinEmrSpotInstanceBidPct() {
        return minEmrSpotInstanceBidPct;
    }

    public int getMaxEmrSpotInstanceBidPct() {
        return maxEmrSpotInstanceBidPct;
    }

    public int getMinSparkInstanceCount() {
        return minSparkInstanceCount;
    }

    public int getMaxSparkInstanceCount() {
        return maxSparkInstanceCount;
    }

    /**
     * Return the <b>true</b> if using roles policy to DLab features.
     */
    public boolean isRolePolicyEnabled() {
        return rolePolicyEnabled;
    }

    /**
     * Return the default access to DLab features using roles policy.
     */
    public boolean getRoleDefaultAccess() {
        return roleDefaultAccess;
    }


    /**
     * Return the <b>true</b> if the billing scheduler is enabled.
     */
    public boolean isBillingSchedulerEnabled() {
        return billingSchedulerEnabled;
    }

    /**
     * Return the default access to DLab features using roles policy.
     */
    public String getBillingConfFile() {
        return billingConfFile;
    }

    /**
     * Return the Azure login configuration
     */
    public AzureLoginConfiguration getAzureLoginConfiguration() {
        return azureLoginConfiguration;
    }


    public int getMinDataprocSlaveInstanceCount() {
        return minDataprocSlaveInstanceCount;
    }

    public List<Integer> getDataprocAvailableMasterInstanceCount() {
        return dataprocAvailableMasterInstanceCount;
    }

    public int getMaxDataprocSlaveInstanceCount() {
        return maxDataprocSlaveInstanceCount;
    }

    public int getMinDataprocPreemptibleCount() {
        return minDataprocPreemptibleCount;
    }
}
