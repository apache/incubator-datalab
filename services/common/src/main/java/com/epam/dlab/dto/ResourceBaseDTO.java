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
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

abstract public class ResourceBaseDTO<T extends ResourceBaseDTO<?>> {
    @JsonProperty("conf_service_base_name")
    private String serviceBaseName;
    @JsonProperty("aws_region")
    private String awsRegion;
    @JsonProperty("exploratory_name")
    private String exploratoryName;
    @JsonProperty("iam_user_name")
    private String iamUserName;
    @JsonProperty("conf_os_user")
    private String confOsUser;
    @JsonProperty("conf_os_family")
    private String confOsFamily;
    @JsonProperty("application")
    private String applicationName;

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

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    @SuppressWarnings("unchecked")
    public T withAwsRegion(String region) {
        setAwsRegion(region);
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

    public String getConfOsUser() {
        return confOsUser;
    }

    public void setConfOsUser(String confOsUser) {
        this.confOsUser = confOsUser;
    }

    @SuppressWarnings("unchecked")
    public T withConfOsUser(String confOsUser) {
        setConfOsUser(confOsUser);
        return (T) this;
    }
    
    public String getConfOsFamily() {
        return confOsFamily;
    }

    public void setConfOsFamily(String confOsFamily) {
        this.confOsFamily = confOsFamily;
    }

    @SuppressWarnings("unchecked")
    public T withConfOsFamily(String confOsFamily) {
        setConfOsFamily(confOsFamily);
        return (T) this;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @SuppressWarnings("unchecked")
    public T withApplicationName(String applicationName) {
        setApplicationName(applicationName);
        return (T) this;
    }
    
    public ToStringHelper toStringHelper(Object self) {
    	return MoreObjects.toStringHelper(self)
    	        .add("serviceBaseName", serviceBaseName)
    	        .add("applicationName", applicationName)
    	        .add("exploratoryName", exploratoryName)
    	        .add("iamUserName", iamUserName)
    	        .add("awsRegion", awsRegion)
    	        .add("confOsUser", confOsUser)
    	        .add("confOsFamily", confOsFamily);
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
