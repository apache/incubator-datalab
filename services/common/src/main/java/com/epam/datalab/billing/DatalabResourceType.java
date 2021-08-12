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

package com.epam.datalab.billing;

public enum DatalabResourceType {
    SSN,
    SSN_BUCKET,
    SSN_CONTAINER,
    SSN_STORAGE_ACCOUNT,
    DATA_LAKE_STORE,
    COLLABORATION_BUCKET,
    COLLABORATION_CONTAINER,
    COLLABORATION_STORAGE_ACCOUNT,
    EDGE,
    EDGE_BUCKET,
    EDGE_CONTAINER,
    EDGE_STORAGE_ACCOUNT,
    EXPLORATORY,
    COMPUTATIONAL,
    VOLUME;

    public static DatalabResourceType of(String string) {
        if (string != null) {
            for (DatalabResourceType value : DatalabResourceType.values()) {
                if (string.equalsIgnoreCase(value.toString())) {
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString().toUpperCase();
    }
}
