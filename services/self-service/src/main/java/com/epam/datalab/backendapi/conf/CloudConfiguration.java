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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CloudConfiguration {

    private final String os;
    private final String serviceBaseName;
    private final String edgeInstanceSize;
    private final String subnetId;
    private final String region;
    private final String zone;
    private final String confTagResourceId;
    private final String securityGroupIds;
    private final String ssnInstanceSize;
    private final String notebookVpcId;
    private final String notebookSubnetId;
    private final String confKeyDir;
    private final String vpcId;
    private final String azureResourceGroupName;
    private final String ssnStorageAccountTagName;
    private final String sharedStorageAccountTagName;
    private final String datalakeTagName;
    private final String azureClientId;
    private final String peeringId;
    private final String gcpProjectId;
    @JsonProperty("ldap")
    private final LdapConfig ldapConfig;

    @Data
    public static class LdapConfig {
        private final String host;
        private final String dn;
        private final String ou;
        private final String user;
        private final String password;
    }
}
