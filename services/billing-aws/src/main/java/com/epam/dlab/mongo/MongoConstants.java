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

package com.epam.dlab.mongo;

/** The constants names of collections and fields in Mongo DB.
 */
public interface MongoConstants {
	/** Name of ID field. */
	String FIELD_ID = "_id";
    
	/** Collection environment settings. */
    String COLLECTION_SETTINGS = "settings";
    String FIELD_SERIVICE_BASE_NAME = "conf_service_base_name";

    /** Collection user AWS credentials. */
    String COLLECTION_USER_EDGE = "userCloudCredentials";

    /** Collection user instances. */
    String COLLECTION_USER_INSTANCES = "userInstances";
    String FIELD_EXPLORATORY_NAME = "exploratory_name";
    String FIELD_USER = "user";
	String FIELD_IMAGE = "image";
    String FIELD_EXPLORATORY_ID = "exploratory_id";
    String FIELD_CURRENCY_CODE = "currency_code";
    String FIELD_COMPUTATIONAL_RESOURCES = "computational_resources";
    String FIELD_COMPUTATIONAL_ID = "computational_id";
    String FIELD_COMPUTATIONAL_NAME = "computational_name";
	String FIELD_DATAENGINE_INSTANCE_COUNT = "dataengine_instance_count";

    /** Collection billing. */
    String COLLECTION_BILLING = "billing";
	String FIELD_DLAB_RESOURCE_ID = "dlab_resource_id";
	String FIELD_RESOURCE_NAME = "resource_name";
	String FIELD_PROJECT = "project";
	String FIELD_DLAB_RESOURCE_TYPE = "dlab_resource_type";

    /** Collection billingTotal. */
	String COLLECTION_BILLING_TOTAL = "billingTotal";
	String FIELD_USAGE_DATE_START = "usage_date_start";
	String FIELD_USAGE_DATE_END = "usage_date_end";
	String BILLING_DATA_COLLECTION = "BillingData";
}
