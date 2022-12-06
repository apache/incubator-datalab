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

package com.epam.datalab.backendapi.conf;

import com.epam.datalab.ServiceConfiguration;
import com.epam.datalab.backendapi.domain.SchedulerConfigurationData;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.rest.client.RESTServiceFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Configuration for Self Service.
 */
public class SelfServiceApplicationConfiguration extends ServiceConfiguration {

    @Min(value = 2)
    @JsonProperty
    private int minEmrInstanceCount;

    @Max(value = 1000)
    @JsonProperty
    private int maxEmrInstanceCount;

    @Min(value = 10)
    @JsonProperty
    private int minEmrSpotInstanceBidPct;

    @Max(value = 95)
    @JsonProperty
    private int maxEmrSpotInstanceBidPct;


    @Min(value = 2)
    @JsonProperty
    private int minHDInsightInstanceCount;

    @Max(value = 1000)
    @JsonProperty
    private int maxHDInsightInstanceCount;

    @Min(value = 2)
    @JsonProperty
    private int minSparkInstanceCount;

    @Max(value = 1000)
    @JsonProperty
    private int maxSparkInstanceCount;

    @JsonProperty
    private String ssnInstanceSize;

    @JsonProperty
    private boolean rolePolicyEnabled = false;

    @JsonProperty
    private boolean roleDefaultAccess = false;

    @JsonProperty
    private Duration checkEnvStatusTimeout = Duration.minutes(10);

    @JsonProperty
    private boolean billingSchedulerEnabled = false;

    @JsonProperty
    private int billingPort;

    @JsonProperty
    private boolean auditEnabled = false;

    @NotEmpty
    @JsonProperty
    private String billingConfFile;
    @JsonProperty
    private int minInstanceCount;
    @JsonProperty
    private int maxInstanceCount;
    @JsonProperty
    private int minDataprocPreemptibleCount;
    @JsonProperty
    private int maxUserNameLength;
    @JsonProperty
    private boolean gcpOuauth2AuthenticationEnabled;
    @JsonProperty
    private boolean mongoMigrationEnabled;
    @JsonProperty
    private int privateKeySize = 2048;
    @Valid
    @NotNull
    private Map<String, SchedulerConfigurationData> schedulers;


    @Valid
    @NotNull
    @JsonProperty("jerseyClient")
    private JerseyClientConfiguration jerseyClient = new JerseyClientConfiguration();

    @Valid
    @NotNull
    @JsonProperty(ServiceConsts.MAVEN_SEARCH_API)
    private RESTServiceFactory mavenApiFactory;

    @Valid
    @NotNull
    private Map<String, String> guacamole;

    private String serviceBaseName;
    private String os;

    private KeycloakConfiguration keycloakConfiguration = new KeycloakConfiguration();

    public Map<String, String> getGuacamole() {
        return guacamole;
    }

    public String getGuacamoleHost() {
        return guacamole.get("serverHost");
    }

    public Integer getGuacamolePort() {
        return Integer.valueOf(guacamole.get("serverPort"));
    }

    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return jerseyClient;
    }

    public Map<String, SchedulerConfigurationData> getSchedulers() {
        return schedulers;
    }

    public boolean isGcpOuauth2AuthenticationEnabled() {
        return gcpOuauth2AuthenticationEnabled;
    }

    public String deployed;

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

    public int getMinHDInsightInstanceCount() {
        return minHDInsightInstanceCount;
    }

    public int getMaxHDInsightInstanceCount() {
        return maxHDInsightInstanceCount;
    }

    public int getMinSparkInstanceCount() {
        return minSparkInstanceCount;
    }

    public int getMaxSparkInstanceCount() {
        return maxSparkInstanceCount;
    }

    /**
     * Return the <b>true</b> if using roles policy to DataLab features.
     */
    public boolean isRolePolicyEnabled() {
        return rolePolicyEnabled;
    }

    /**
     * Return the default access to DataLab features using roles policy.
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

    public int getBillingPort() {
        return billingPort;
    }

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    /**
     * Return the default access to DataLab features using roles policy.
     */
    public String getBillingConfFile() {
        return billingConfFile;
    }


    public int getMinInstanceCount() {
        return minInstanceCount;
    }

    public int getMaxInstanceCount() {
        return maxInstanceCount;
    }

    public int getMinDataprocPreemptibleCount() {
        return minDataprocPreemptibleCount;
    }

    public int getMaxUserNameLength() {
        return maxUserNameLength;
    }

    public int getPrivateKeySize() {
        return privateKeySize;
    }

    public String getSsnInstanceSize() {
        return ssnInstanceSize;
    }

    public boolean isMongoMigrationEnabled() {
        return mongoMigrationEnabled;
    }

    @NotNull
    public RESTServiceFactory getMavenApiFactory() {
        return mavenApiFactory;
    }

    public KeycloakConfiguration getKeycloakConfiguration() {
        return keycloakConfiguration;
    }

    public String getServiceBaseName() {
        return serviceBaseName;
    }

    public String getOs() {
        return os;
    }

    public String getDeployed() {
        return deployed;
    }

}
