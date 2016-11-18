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

package com.epam.dlab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

abstract public class ResourceBaseDTO<T extends ResourceBaseDTO<?>> {
    @JsonProperty("conf_service_base_name")
    private String serviceBaseName;
    @JsonProperty("creds_region")
    private String region;
    @JsonProperty("exploratory_name")
    private String exploratoryName;
    @JsonProperty("iam_user_name")
    private String iamUserName;

    public String getServiceBaseName() {
        return serviceBaseName;
    }

    public void setServiceBaseName(String serviceBaseName) {
        this.serviceBaseName = serviceBaseName;
    }

    @SuppressWarnings("unchecked")
    public T withServiceBaseName(String serviceBaseName) {
        setServiceBaseName(serviceBaseName);
        return (T) this;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @SuppressWarnings("unchecked")
    public T withRegion(String region) {
        setRegion(region);
        return (T) this;
    }

    public String getExploratoryName() {
        return exploratoryName;
    }

    public void setExploratoryName(String exploratoryName) {
        this.exploratoryName = exploratoryName;
    }

    @SuppressWarnings("unchecked")
    public T withExploratoryName(String exploratoryName) {
        setExploratoryName(exploratoryName);
        return (T) this;
    }

    public String getIamUserName() {
        return iamUserName;
    }

    public void setIamUserName(String iamUserName) {
        this.iamUserName = iamUserName;
    }

    @SuppressWarnings("unchecked")
    public T withIamUserName(String iamUserName) {
        setIamUserName(iamUserName);
        return (T) this;
    }
}
