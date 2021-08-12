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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * Stores the health status for user environment.
 */
public class HealthStatusResource {
    @JsonProperty("type")
    private String type;
    @JsonProperty("resource_id")
    private String resourceId;
    @JsonProperty("status")
    private String status;

    /**
     * Return the type of resource.
     */
    public String getType() {
        return type;
    }

    /**
     * Set the type of resource.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Set the type of resource.
     */
    public HealthStatusResource withType(String type) {
        setType(type);
        return this;
    }

    /**
     * Return the id of resource (ip address, path, etc).
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Set the id of resource (ip address, path, etc).
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    /**
     * Set the id of resource (ip address, path, etc).
     */
    public HealthStatusResource withResourceId(String resourceId) {
        setResourceId(resourceId);
        return this;
    }

    /**
     * Return the status of resource.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Set the status of resource.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Set the status of resource.
     */
    public HealthStatusResource withStatus(String status) {
        setStatus(status);
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("resourceId", resourceId)
                .add("status", status)
                .toString();
    }
}
