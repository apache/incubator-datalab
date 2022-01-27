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

package com.epam.datalab.dto.base.edge;

import com.epam.datalab.dto.aws.edge.EdgeInfoAws;
import com.epam.datalab.dto.azure.edge.EdgeInfoAzure;
import com.epam.datalab.dto.gcp.edge.EdgeInfoGcp;
import com.fasterxml.jackson.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
)
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = EdgeInfoAws.class, name = "aws"),
//        @JsonSubTypes.Type(value = EdgeInfoAzure.class, name = "azure"),
//        @JsonSubTypes.Type(value = EdgeInfoGcp.class, name = "gcp")
//})
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

    @JsonProperty("reupload_key_required")
    private boolean reuploadKeyRequired = false;

    @JsonProperty("gpu_types")
    private List<String> gpuList;
}
