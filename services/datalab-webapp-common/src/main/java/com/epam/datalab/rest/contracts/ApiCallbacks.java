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

package com.epam.datalab.rest.contracts;

public class ApiCallbacks {
    public static final String API = "/api";
    public static final String KEY_LOADER = API + "/user/access_key/callback";
    public static final String INFRASTRUCTURE_PROVISION = API + "/infrastructure_provision";
    public static final String COMPUTATIONAL = INFRASTRUCTURE_PROVISION + "/computational_resources";
    public static final String EXPLORATORY = INFRASTRUCTURE_PROVISION + "/exploratory_environment";
    public static final String LIBRARY = INFRASTRUCTURE_PROVISION + "/library";
    public static final String UPDATE_LIBS_URI = LIBRARY + "/update_lib_list";
    public static final String INFRASTRUCTURE = API + "/infrastructure";
    public static final String EDGE = INFRASTRUCTURE + "/edge";
    public static final String STATUS_URI = "/status";
    public static final String LIB_STATUS_URI = LIBRARY + "/lib_status";
    public static final String GIT_CREDS = API + "/user/git_creds" + STATUS_URI;
    public static final String IMAGE = INFRASTRUCTURE_PROVISION + "/image";
    public static final String IMAGE_STATUS_URI = IMAGE + "/image_status";
    public static final String BACKUP_URI = API + "/infrastructure/backup" + STATUS_URI;
    public static final String REUPLOAD_KEY_URI = API + "/infrastructure/reupload_key/callback";
    public static final String CHECK_INACTIVITY_EXPLORATORY_URI = API + "/infrastructure/inactivity/callback/exploratory";
    public static final String CHECK_INACTIVITY_COMPUTATIONAL_URI = API + "/infrastructure/inactivity/callback" +
            "/computational";

    private ApiCallbacks() {
    }
}
