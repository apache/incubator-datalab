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

package com.epam.datalab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

public class ResourceEnvBaseDTO<T extends ResourceEnvBaseDTO<?>> extends ResourceSysBaseDTO<T> {
    @JsonProperty("exploratory_name")
    private String exploratoryName;
    @JsonProperty("application")
    private String applicationName;

    @SuppressWarnings("unchecked")
    private final T self = (T) this;

    public String getExploratoryName() {
        return exploratoryName;
    }

    public void setExploratoryName(String exploratoryName) {
        this.exploratoryName = exploratoryName;
    }

    public T withExploratoryName(String exploratoryName) {
        setExploratoryName(exploratoryName);
        return self;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public T withApplicationName(String applicationName) {
        setApplicationName(applicationName);
        return self;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("applicationName", applicationName)
                .add("exploratoryName", exploratoryName);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
