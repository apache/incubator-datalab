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

package com.epam.datalab.dto.exploratory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import lombok.Data;

import java.util.Map;

@Data
public class ExploratoryImageDTO extends ExploratoryActionDTO<ExploratoryImageDTO> {

    @JsonProperty("notebook_image_name")
    private String imageName;

    @JsonProperty("tags")
    private Map<String, String> tags;
    @JsonProperty("endpoint_name")
    private String endpoint;
    @JsonProperty("conf_shared_image_enabled")
    private String sharedImageEnabled;

    public ExploratoryImageDTO withImageName(String imageName) {
        this.imageName = imageName;
        return this;
    }

    public ExploratoryImageDTO withTags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public ExploratoryImageDTO withEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ExploratoryImageDTO withSharedImageEnabled(String sharedImageEnabled) {
        this.sharedImageEnabled = sharedImageEnabled;
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("imageName", imageName);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
