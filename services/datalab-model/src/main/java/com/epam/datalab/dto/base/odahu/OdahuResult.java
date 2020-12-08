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

package com.epam.datalab.dto.base.odahu;

import com.epam.datalab.dto.ResourceURL;
import com.epam.datalab.dto.StatusBaseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OdahuResult extends StatusBaseDTO<OdahuResult> {
    private String name;
    @JsonProperty("project_name")
    private String projectName;
    @JsonProperty("endpoint_name")
    private String endpointName;
    @JsonProperty("grafana_admin")
    private String grafanaAdmin;
    @JsonProperty("grafana_pass")
    private String grafanaPassword;
    @JsonProperty("oauth_cookie_secret")
    private String oauthCookieSecret;
    @JsonProperty("odahuflow_connection_decrypt_token")
    private String decryptToken;
    @JsonProperty("odahu_urls")
    private List<ResourceURL> resourceUrls;
}
