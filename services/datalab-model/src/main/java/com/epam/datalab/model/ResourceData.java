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

package com.epam.datalab.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class ResourceData {
    private ResourceType resourceType;
    private String resourceId;
    private String exploratoryName;
    private String computationalName;

    public static ResourceData edgeResource(String resourceId) {
        return new ResourceData(ResourceType.EDGE, resourceId, null, null);
    }

    public static ResourceData exploratoryResource(String resourceId, String exploratoryName) {
        return new ResourceData(ResourceType.EXPLORATORY, resourceId, exploratoryName, null);
    }

    public static ResourceData computationalResource(String resourceId, String exploratoryName,
                                                     String computationalName) {
        return new ResourceData(ResourceType.COMPUTATIONAL, resourceId, exploratoryName, computationalName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (resourceType == ResourceType.EDGE) {
            return sb.append(resourceType.toString()).toString();
        } else if (resourceType == ResourceType.EXPLORATORY) {
            return sb.append(resourceType.toString()).append(" ").append(exploratoryName).toString();
        } else if (resourceType == ResourceType.COMPUTATIONAL) {
            return sb.append(resourceType.toString()).append(" ").append(computationalName)
                    .append(" affiliated with exploratory ").append(exploratoryName).toString();
        } else return "";
    }
}
