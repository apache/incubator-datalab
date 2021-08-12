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

package com.epam.datalab.dto.base;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum DataEngineType {
    CLOUD_SERVICE("dataengine-service"),
    SPARK_STANDALONE("dataengine");

    private static final String DOCKER_IMAGE_PREFIX = "docker.datalab-";

    private static final Map<String, DataEngineType> INTERNAL_MAP = new HashMap<>();

    static {
        for (DataEngineType dataEngineType : DataEngineType.values()) {
            INTERNAL_MAP.put(dataEngineType.getName(), dataEngineType);
        }
    }

    private final String name;

    DataEngineType(String name) {
        this.name = name;
    }

    public String getImage() {
        return DOCKER_IMAGE_PREFIX + this.name;
    }

    public static DataEngineType fromString(String name) {
        return INTERNAL_MAP.get(name);
    }

    public static DataEngineType fromDockerImageName(String name) {
        return INTERNAL_MAP.get(name.replace(DOCKER_IMAGE_PREFIX, ""));
    }

    public static String getDockerImageName(DataEngineType dataEngineType) {
        return DOCKER_IMAGE_PREFIX + dataEngineType.getName();
    }

    @JsonValue
    public String getName() {
        return name;
    }
}