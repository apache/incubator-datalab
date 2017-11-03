/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.dto.base.edge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdgeInfo {
    @JsonProperty("_id")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String id;
    @JsonProperty("instance_id")
    private String instanceId;
    @JsonProperty
    private String hostname;
    @JsonProperty("public_ip")
    private String publicIp;
    @JsonProperty
    private String ip;
    @JsonProperty("key_name")
    private String keyName;
    @JsonProperty("tunnel_port")
    private String tunnelPort;
    @JsonProperty("socks_port")
    private String socksPort;
    @JsonProperty("notebook_sg")
    private String notebookSg;
    @JsonProperty("edge_sg")
    private String edgeSg;
    @JsonProperty("notebook_subnet")
    private String notebookSubnet;
    @JsonProperty("edge_status")
    private String edgeStatus;
}
