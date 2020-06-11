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

package com.epam.dlab.backendapi.domain;

public enum AuditActionEnum {
    CREATE_PROJECT, START_PROJECT, STOP_PROJECT, TERMINATE_PROJECT, UPDATE_PROJECT,
    CREATE_NOTEBOOK, START_NOTEBOOK, STOP_NOTEBOOK, TERMINATE_NOTEBOOK, UPDATE_CLUSTER_CONFIG,
    CREATE_DATA_ENGINE, CREATE_DATA_ENGINE_SERVICE, START_COMPUTATIONAL, STOP_COMPUTATIONAL, TERMINATE_COMPUTATIONAL, UPDATE_DATA_ENGINE_CONFIG,
    CREATE_ENDPOINT, DELETE_ENDPOINT,
    FOLLOW_NOTEBOOK_LINK
}
