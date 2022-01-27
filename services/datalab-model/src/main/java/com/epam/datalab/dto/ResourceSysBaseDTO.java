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

public class ResourceSysBaseDTO<T extends ResourceSysBaseDTO<?>> extends ResourceBaseDTO<T> {
    @JsonProperty("conf_service_base_name")
    private String serviceBaseName;
    @JsonProperty("conf_os_family")
    private String confOsFamily;
    @JsonProperty("conf_key_dir")
    private String confKeyDir;

    public String getServiceBaseName() {
        return serviceBaseName;
    }

    public void setServiceBaseName(String serviceBaseName) {
        this.serviceBaseName = serviceBaseName;
    }

    public T withServiceBaseName(String serviceBaseName) {
        setServiceBaseName(serviceBaseName);
        return (T) this;
    }

    public String getConfOsFamily() {
        return confOsFamily;
    }

    public void setConfOsFamily(String confOsFamily) {
        this.confOsFamily = confOsFamily;
    }

    public T withConfOsFamily(String confOsFamily) {
        setConfOsFamily(confOsFamily);
        return (T) this;
    }


    public String getConfKeyDir() {
        return confKeyDir;
    }

    public void setConfKeyDir(String confKeyDir) {
        this.confKeyDir = confKeyDir;
    }

    public T withConfKeyDir(String confKeyDir) {
        setConfKeyDir(confKeyDir);
        return (T) this;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("serviceBaseName", serviceBaseName)
                .add("confKeyDir", confKeyDir)
                .add("confOsFamily", confOsFamily);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
