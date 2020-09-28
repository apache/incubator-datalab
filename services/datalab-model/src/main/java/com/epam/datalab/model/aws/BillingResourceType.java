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

package com.epam.datalab.model.aws;

/**
 * Billing resource types.
 */
public enum BillingResourceType {
    COMPUTER,
    CLUSTER,
    STORAGE,
    STORAGE_EBS,
    STORAGE_BUCKET,
    IP_ADDRESS,
    OTHER;

    public static BillingResourceType of(String string) {
        if (string != null) {
            for (BillingResourceType value : BillingResourceType.values()) {
                if (string.equalsIgnoreCase(value.toString())) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Return the category of resource.
     *
     * @param resourceType the type of resource.
     */
    public static String category(BillingResourceType resourceType) {
        switch (resourceType) {
            case COMPUTER:
                return "EC2";
            case CLUSTER:
                return "Compute";
            case STORAGE:
                return "Storage";
            case STORAGE_EBS:
                return "EBS";
            case STORAGE_BUCKET:
                return "S3";
            case IP_ADDRESS:
                return "Static";
            default:
                return "Other";
        }
    }

    /**
     * Return the category of resource.
     */
    public String category() {
        return category(this);
    }

    @Override
    public String toString() {
        return super.toString().toUpperCase();
    }
}
