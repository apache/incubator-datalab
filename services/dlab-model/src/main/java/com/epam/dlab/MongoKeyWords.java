/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab;

public abstract class MongoKeyWords {
    public static final String EDGE_COLLECTION = "userCloudCredentials";
    public static final String SETTINGS_COLLECTION = "settings";
    public static final String NOTEBOOK_COLLECTION = "userInstances";
    public static final String SERVICE_BASE_NAME_KEY = "conf_service_base_name";
    public static final String SSN_STORAGE_ACCOUNT_TAG_KEY = "ssn_storage_account_tag_name";
    public static final String SHARED_STORAGE_ACCOUNT_TAG_KEY = "shared_storage_account_tag_name";
    public static final String DATA_LAKE_TAG_NAME = "datalake_tag_name";
    public static final String BILLING_DETAILS = "billing";
    public static final String AZURE_BILLING_SCHEDULER = "billingScheduler";
    public static final String AZURE_BILLING_SCHEDULER_HISTORY = "billingSchedulerHistory";

    /**
     * Mongo DB keywords related to billing functionality
     */
    public static final String MONGO_ID = "_id";
    public static final String DLAB_ID = "dlabId";
    public static final String DLAB_USER = "user";
    public static final String EXPLORATORY_ID = "exploratoryId";
    public static final String EXPLORATORY_ID_OLD = "exploratory_id";
    public static final String RESOURCE_TYPE = "resourceType";
    public static final String RESOURCE_NAME = "resourceName";
    public static final String COMPUTATIONAL_ID = "computationalId";
    public static final String METER_CATEGORY = "meterCategory";
    public static final String COST = "cost";
    public static final String COST_STRING = "costString";
    public static final String USAGE_DAY = "day";
    public static final String USAGE_FROM = "from";
    public static final String USAGE_TO = "to";
    public static final String CURRENCY_CODE = "currencyCode";


    private MongoKeyWords() {
    }

    public static String prepend$(String value) {
        return "$" + value;
    }

    public static String prependId(String value) {
        return MongoKeyWords.MONGO_ID + "." + value;
    }
}
