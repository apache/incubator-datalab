/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.mongo;

/** The constants names of collections and fields in Mongo DB.
 */
public interface MongoConstants {
	/** Name of ID field. */
	public static final String FIELD_ID = "_id";
    
	/** Collection environment settings. */
    public static final String COLLECTION_SETTINGS = "settings";
    public static final String FIELD_SERIVICE_BASE_NAME = "conf_service_base_name";

    /** Collection user AWS credentials. */
    public static final String COLLECTION_USER_EDGE = "userCloudCredentials";
    public static final String FIELD_EDGE_BUCKET = "user_own_bicket_name";

    /** Collection user instances. */
    public static final String COLLECTION_USER_INSTANCES = "userInstances";
    public static final String FIELD_EXPLORATORY_NAME = "exploratory_name";
    public static final String FIELD_USER = "user";
	public static final String FIELD_IMAGE = "image";
    public static final String FIELD_EXPLORATORY_ID = "exploratory_id";
    public static final String FIELD_CURRENCY_CODE = "currency_code";
    public static final String FIELD_COMPUTATIONAL_RESOURCES = "computational_resources";
    public static final String FIELD_COMPUTATIONAL_ID = "computational_id";
    public static final String FIELD_COMPUTATIONAL_NAME = "computational_name";
	String FIELD_DATAENGINE_INSTANCE_COUNT = "dataengine_instance_count";

    /** Collection billing. */
    public static final String COLLECTION_BILLING = "billing";
	public static final String FIELD_DLAB_RESOURCE_ID = "dlab_resource_id";
	public static final String FIELD_RESOURCE_NAME = "resource_name";
	public static final String FIELD_DLAB_RESOURCE_TYPE = "dlab_resource_type";

    /** Collection billingTotal. */
	public static final String COLLECTION_BILLING_TOTAL = "billingTotal";
	public static final String FIELD_USAGE_DATE_START = "usage_date_start";
	public static final String FIELD_USAGE_DATE_END = "usage_date_end";
	String BILLING_DATA_COLLECTION = "BillingData";
}
