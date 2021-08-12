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

package com.epam.datalab.dto;

public enum UserInstanceStatus {
    CREATING("creating"),
    CREATED("created"),
    STARTING("starting"),
    CONFIGURING("configuring"),
    RUNNING("running"),
    STOPPING("stopping"),
    STOPPED("stopped"),
    TERMINATING("terminating"),
    TERMINATED("terminated"),
    FAILED("failed"),
    CREATING_IMAGE("creating image"),
    RECONFIGURING("reconfiguring"),
    REUPLOADING_KEY("reuploading key");

    private final String name;

    UserInstanceStatus(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static UserInstanceStatus of(String status) {
        if (status != null) {
            for (UserInstanceStatus uis : UserInstanceStatus.values()) {
                if (status.equalsIgnoreCase(uis.toString())) {
                    return uis;
                }
            }
        }
        return null;
    }

    public boolean in(UserInstanceStatus... statusList) {
        for (UserInstanceStatus status : statusList) {
            if (this.equals(status)) {
                return true;
            }
        }
        return false;
    }
}