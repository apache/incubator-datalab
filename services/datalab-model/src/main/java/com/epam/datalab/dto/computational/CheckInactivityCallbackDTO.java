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
package com.epam.datalab.dto.computational;

import com.epam.datalab.dto.ResourceBaseDTO;
import com.epam.datalab.dto.status.EnvResource;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class CheckInactivityCallbackDTO extends ResourceBaseDTO<CheckInactivityCallbackDTO> {

    @JsonProperty
    private List<EnvResource> resources;

    @JsonProperty
    private String id;

    public CheckInactivityCallbackDTO withClusters(List<EnvResource> clusters) {
        setResources(clusters);
        return this;
    }

    public CheckInactivityCallbackDTO withId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("resources", resources)
                .add("id", id)
                .toString();
    }

}
