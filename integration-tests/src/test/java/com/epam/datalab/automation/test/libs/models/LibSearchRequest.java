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

package com.epam.datalab.automation.test.libs.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/**
 * Created by yu on 7/3/17.
 */
public class LibSearchRequest {
    @JsonProperty("exploratory_name")
    private String notebookName;
    @JsonProperty
    private String group;
    @JsonProperty("start_with")
    private String startWith;

    public LibSearchRequest() {
    }

    public LibSearchRequest(String notebookName, String group, String startWith) {
        this.notebookName = notebookName;
        this.group = group;
        this.startWith = startWith;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("notebookName", notebookName)
                .add("group", group)
                .add("startWith", startWith)
                .toString();
    }
}
