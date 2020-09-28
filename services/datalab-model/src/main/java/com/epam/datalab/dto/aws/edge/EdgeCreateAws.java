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

package com.epam.datalab.dto.aws.edge;

import com.epam.datalab.dto.ResourceSysBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

public class EdgeCreateAws extends ResourceSysBaseDTO<EdgeCreateAws> {
    @JsonProperty("edge_elastic_ip")
    private String edgeElasticIp;

    public String getEdgeElasticIp() {
        return edgeElasticIp;
    }

    public void setEdgeElasticIp(String edgeElasticIp) {
        this.edgeElasticIp = edgeElasticIp;
    }

    public EdgeCreateAws withEdgeElasticIp(String edgeElasticIp) {
        setEdgeElasticIp(edgeElasticIp);
        return this;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("edgeElasticIp", edgeElasticIp);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
