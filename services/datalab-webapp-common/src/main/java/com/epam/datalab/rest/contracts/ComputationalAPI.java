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

public interface ComputationalAPI {
    String AUDIT_MESSAGE = "Notebook name %s";
    String AUDIT_COMPUTATIONAL_RECONFIGURE_MESSAGE = "Reconfigure compute %s, requested for notebook %s";
    String LIBRARY = "library/";
    String COMPUTATIONAL = "computational";
    String COMPUTATIONAL_CREATE = COMPUTATIONAL + "/create";
    String COMPUTATIONAL_STOP = COMPUTATIONAL + "/stop";
    String COMPUTATIONAL_START = COMPUTATIONAL + "/start";
    String SPARK = "/spark";
    String COMPUTATIONAL_CREATE_SPARK = COMPUTATIONAL_CREATE + SPARK;
    String COMPUTATIONAL_RECONFIGURE_SPARK = COMPUTATIONAL + SPARK + "/reconfigure";
    String COMPUTATIONAL_CREATE_CLOUD_SPECIFIC = COMPUTATIONAL_CREATE + "/cloud";
    String COMPUTATIONAL_TERMINATE = COMPUTATIONAL + "/terminate";
    String COMPUTATIONAL_TERMINATE_SPARK = COMPUTATIONAL_TERMINATE + SPARK;
    String COMPUTATIONAL_STOP_SPARK = COMPUTATIONAL_STOP + SPARK;
    String COMPUTATIONAL_START_SPARK = COMPUTATIONAL_START + SPARK;
    String COMPUTATIONAL_TERMINATE_CLOUD_SPECIFIC = COMPUTATIONAL_TERMINATE + "/cloud";
    String COMPUTATIONAL_LIB_INSTALL = LIBRARY + COMPUTATIONAL + "/lib_install";
    String COMPUTATIONAL_LIB_LIST = LIBRARY + COMPUTATIONAL + "/lib_list";
}
