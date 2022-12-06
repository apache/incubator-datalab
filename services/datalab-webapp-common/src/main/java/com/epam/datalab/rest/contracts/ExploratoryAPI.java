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

public interface ExploratoryAPI {
    String LIBRARY = "library/";
    String EXPLORATORY = "exploratory";
    String EXPLORATORY_CREATE = EXPLORATORY + "/create";
    String EXPLORATORY_RECONFIGURE_SPARK = EXPLORATORY + "/reconfigure_spark";
    String EXPLORATORY_START = EXPLORATORY + "/start";
    String EXPLORATORY_TERMINATE = EXPLORATORY + "/terminate";
    String EXPLORATORY_STOP = EXPLORATORY + "/stop";
    String EXPLORATORY_LIB_INSTALL = LIBRARY + EXPLORATORY + "/lib_install";
    String EXPLORATORY_LIB_LIST = LIBRARY + EXPLORATORY + "/lib_list";
    String EXPLORATORY_GIT_CREDS = EXPLORATORY + "/git_creds";
    String EXPLORATORY_IMAGE = EXPLORATORY + "/image";
    String EXPLORATORY_IMAGE_TERMINATE = EXPLORATORY + "/image" + "/terminate";
}
