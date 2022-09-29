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
package com.epam.datalab.backendapi.resources.dto;

import com.epam.datalab.cloud.CloudProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRoleDTO {
    @JsonProperty("_id")
    private String id;
    private String description;
    private Type type;
    private CloudProvider cloud;
    private Set<String> pages;
    private Set<String> computationals;
    private Set<String> exploratories;
    private Set<String> images;
    @JsonProperty("exploratory_shapes")
    private Set<String> exploratoryShapes;
    @JsonProperty("computational_shapes")
    private Set<String> computationalShapes;
    private Set<String> groups;

    private enum Type {
        NOTEBOOK,
        COMPUTATIONAL,
        IMAGE,
        NOTEBOOK_SHAPE,
        COMPUTATIONAL_SHAPE,
        CONNECTED_PLATFORMS,
        BILLING,
        BUCKET_BROWSER,
        ADMINISTRATION,
    }

    public static List<Type> cloudSpecificTypes() {
        return Arrays.asList(Type.NOTEBOOK, Type.COMPUTATIONAL, Type.IMAGE, Type.NOTEBOOK_SHAPE, Type.COMPUTATIONAL_SHAPE);
    }
}
