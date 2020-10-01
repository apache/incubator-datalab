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

package com.epam.datalab.dto.reuploadkey;

import com.epam.datalab.dto.ResourceSysBaseDTO;
import com.epam.datalab.model.ResourceData;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import lombok.Getter;

@Getter
public class ReuploadKeyCallbackDTO extends ResourceSysBaseDTO<ReuploadKeyCallbackDTO> {

    @JsonProperty
    private ResourceData resource;

    @JsonProperty("resource_id")
    private String resourceId;

    @JsonProperty
    private String id;


    public ReuploadKeyCallbackDTO withResource(ResourceData resource) {
        this.resource = resource;
        return this;
    }

    public ReuploadKeyCallbackDTO withId(String id) {
        this.id = id;
        return this;
    }

    public ReuploadKeyCallbackDTO withResourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("resource", resource)
                .add("resource_id", resourceId)
                .add("id", id);
    }
}

