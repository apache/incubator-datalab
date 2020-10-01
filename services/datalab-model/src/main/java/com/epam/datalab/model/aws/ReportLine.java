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

import com.epam.datalab.exceptions.ParseException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The line of billing report.
 */
public class ReportLine {
    /**
     * Patterns to calculate the type of resource in report line.
     */
    private static final Pattern pInstancceId = Pattern.compile("^i-[a-z0-9]{17}$");
    private static final Pattern pVolumeId = Pattern.compile("^vol-[a-z0-9]{17}$");
    private static final Pattern pIpAddress = Pattern.compile("^[1-9][0-9]{0,2}\\.[1-9][0-9]{0,2}\\.[1-9][0-9]{0,2}\\.[1-9][0-9]{0,2}$");
    private static final Pattern pClusterId = Pattern.compile("j-[A-Z0-9]{12,13}$");

    public static final String FIELD_DATALAB_ID = "datalab_id";
    public static final String FIELD_USER_ID = "user";
    public static final String FIELD_USAGE_DATE = "usage_date";
    public static final String FIELD_PRODUCT = "product";
    public static final String FIELD_USAGE_TYPE = "usage_type";
    public static final String FIELD_USAGE = "usage";
    public static final String FIELD_COST = "cost";
    public static final String FIELD_CURRENCY_CODE = "currency_code";
    public static final String FIELD_RESOURCE_TYPE = "resource_type";
    public static final String FIELD_RESOURCE_ID = "resource_id";
    public static final String FIELD_TAGS = "tags";

    @JsonProperty
    private String datalabId;

    @JsonProperty
    private String user;

    @JsonProperty
    private String usageDate;

    @JsonProperty
    private String usageIntervalEnd;

    @JsonProperty
    private String product;

    @JsonProperty
    private String usageType;

    @JsonProperty
    private double usage;

    @JsonProperty
    private double cost;

    @JsonProperty
    private String currencyCode;

    @JsonProperty
    private BillingResourceType resourceType;

    @JsonProperty
    private String resourceId;

    @JsonProperty
    private LinkedHashMap<String, String> tags;


    public String getDatalabId() {
        return datalabId;
    }

    public void setDatalabId(String datalabId) {
        this.datalabId = datalabId;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(String usageDate) {
        this.usageDate = usageDate;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public double getUsage() {
        return usage;
    }

    public void setUsage(double usage) {
        this.usage = usage;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getResourceId() {
        return resourceId;
    }

    public BillingResourceType getResourceType() {
        return resourceType;
    }

    public LinkedHashMap<String, String> getTags() {
        return tags;
    }

    public void setTags(LinkedHashMap<String, String> tags) {
        this.tags = tags;
    }

    /**
     * Calculate and set the type of resource and resource id.
     *
     * @throws ParseException
     */
    public void setResourceTypeId(String resourceTypeId) throws ParseException {
        if (product == null) {
            throw new ParseException("Property product is not set");
        }

        if ("Amazon Elastic MapReduce".equals(product) ||
                "Amazon Simple Queue Service".equals(product)) {
            resourceType = BillingResourceType.CLUSTER;
            Matcher m = pClusterId.matcher(resourceTypeId);
            resourceId = (m.find() ? m.group() : null);
        } else {
            if ("Amazon Elastic Compute Cloud".equals(product)) {
                if (pInstancceId.matcher(resourceTypeId).find()) {
                    resourceType = BillingResourceType.COMPUTER;
                } else if (pVolumeId.matcher(resourceTypeId).find()) {
                    resourceType = BillingResourceType.STORAGE_EBS;
                } else if (pIpAddress.matcher(resourceTypeId).find()) {
                    resourceType = BillingResourceType.IP_ADDRESS;
                } else {
                    resourceType = BillingResourceType.COMPUTER;
                }
            } else if ("Amazon Simple Storage Service".equals(product)) {
                resourceType = BillingResourceType.STORAGE_BUCKET;
            } else {
                resourceType = BillingResourceType.OTHER;
            }
            resourceId = resourceTypeId;
        }
    }


    /**
     * Returns a string representation of the object.
     *
     * @param self the object to generate the string for (typically this), used only for its class name.
     */
    public ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add(FIELD_DATALAB_ID, datalabId)
                .add(FIELD_USER_ID, user)
                .add(FIELD_USAGE_DATE, usageDate)
                .add(FIELD_PRODUCT, product)
                .add(FIELD_USAGE_TYPE, usageType)
                .add(FIELD_USAGE, usage)
                .add(FIELD_COST, cost)
                .add(FIELD_CURRENCY_CODE, currencyCode)
                .add(FIELD_RESOURCE_TYPE, resourceType)
                .add(FIELD_RESOURCE_ID, resourceId)
                .add(FIELD_TAGS, tags);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .toString();
    }
}
