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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;

import javax.validation.Valid;

/** Configuration for Self Service.
 */
public class SelfServiceApplicationConfiguration extends ServiceConfiguration {

    @Valid
    @JsonProperty
    private boolean mocked;
    
    @Valid
    @JsonProperty
    private int minEmrInstanceCount;

    @Valid
    @JsonProperty
    private int maxEmrInstanceCount;


    @Valid
    @JsonProperty
    private int minEmrSpotInstanceBidPct;

    @Valid
    @JsonProperty
    private int maxEmrSpotInstanceBidPct;
    
    @Valid
    @JsonProperty
    private boolean rolePolicyEnabled = false;
    
    @Valid
    @JsonProperty
    private boolean roleDefaultAccess = false;
    
    @Valid
    @JsonProperty
    private Duration checkEnvStatusTimeout = Duration.minutes(10);
    
    @Valid
    @JsonProperty
    private boolean billingSchedulerEnabled = false;
    
    @Valid
    @JsonProperty
    private String billingConfFile = null;


    /** Returns <b>true</b> if service is a mock. */
    public boolean isMocked() {
        return mocked;
    }
    
    /** Returns the minimum number of slave EMR instances than could be created. */
    public int getMinEmrInstanceCount() {
    	return minEmrInstanceCount;
    }

    /** Returns the maximum number of slave EMR instances than could be created. */
    public int getMaxEmrInstanceCount() {
    	return maxEmrInstanceCount;
    }
    
    /** Returns the timeout for check the status of environment via provisioning service. */
    public Duration getCheckEnvStatusTimeout() {
    	return checkEnvStatusTimeout;
    }

    public int getMinEmrSpotInstanceBidPct() {
        return minEmrSpotInstanceBidPct;
    }

    public int getMaxEmrSpotInstanceBidPct() {
        return maxEmrSpotInstanceBidPct;
    }

    /** Return the <b>true</b> if using roles policy to DLab features. */
    public boolean isRolePolicyEnabled() {
        return rolePolicyEnabled;
    }
    
    /** Return the default access to DLab features using roles policy. */
    public boolean getRoleDefaultAccess() {
    	return roleDefaultAccess;
    }

    public SelfServiceApplicationConfiguration withCheckEnvStatusTimeout(Duration checkEnvStatusTimeout) {
    	this.checkEnvStatusTimeout = checkEnvStatusTimeout;
    	return this;
    }
    
    /** Return the <b>true</b> if the billing scheduler is enabled. */
    public boolean isBillingSchedulerEnabled() {
    	return billingSchedulerEnabled;
    }
    
    /** Return the default access to DLab features using roles policy. */
    public String getBillingConfFile() {
    	return billingConfFile;
    }
}
