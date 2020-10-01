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

package com.epam.datalab.dto.azure.auth;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import javax.ws.rs.FormParam;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(exclude = "code")
public class AuthorizationCodeFlowResponse {

    @JsonProperty
    @FormParam("state")
    private String state;
    @JsonProperty
    @FormParam("code")
    private String code;
    @JsonProperty
    @FormParam("error")
    private String error;
    @JsonProperty("error_description")
    @FormParam("error_description")
    private String errorDescription;

    @JsonIgnore
    public boolean isSuccessful() {
        return state != null && !state.isEmpty() && code != null && !code.isEmpty();
    }

    @JsonIgnore
    public boolean isFailed() {
        return error != null && !error.isEmpty();
    }

    @JsonIgnore
    public boolean isValid() {
        return isSuccessful() || isFailed();
    }
}
