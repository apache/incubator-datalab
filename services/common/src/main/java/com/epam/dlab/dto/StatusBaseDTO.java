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

import com.epam.dlab.UserInstanceStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.Date;

public class StatusBaseDTO<T extends StatusBaseDTO<?>> {
    @JsonProperty
    private String user;
    @JsonProperty("instance_id")
    private String instanceId;
    @JsonProperty("exploratory_name")
    private String exploratoryName;
    @JsonProperty("exploratory_template_name")
    private String exploratoryTemplateName;
    @JsonProperty
    private String status;
    @JsonProperty("error_message")
    private String errorMessage;
    @JsonProperty("up_time")
    private Date uptime;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @SuppressWarnings("unchecked")
    public T withUser(String user) {
        setUser(user);
        return (T) this;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @SuppressWarnings("unchecked")
    public T withInstanceId(String instanceId) {
    	setInstanceId(instanceId);
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

    public String getExploratoryTemplateName() {
        return exploratoryTemplateName;
    }

    public void setExploratoryTemplateName(String exploratoryTemplateName) {
        this.exploratoryTemplateName = exploratoryTemplateName;
    }

    @SuppressWarnings("unchecked")
    public T withExploratoryTemplateName(String exploratoryTemplateName) {
        setExploratoryTemplateName(exploratoryTemplateName);
        return (T) this;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @SuppressWarnings("unchecked")
    public T withStatus(String status) {
        setStatus(status);
        return (T) this;
    }

    public T withStatus(UserInstanceStatus status) {
        return withStatus(status.toString());
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @SuppressWarnings("unchecked")
    public T withErrorMessage(String errorMessage) {
        setErrorMessage(errorMessage);
        return (T) this;
    }

    public Date getUptime() {
        return uptime;
    }

    public void setUptime(Date uptime) {
        this.uptime = uptime;
    }

    @SuppressWarnings("unchecked")
    public T withUptime(Date uptime) {
        setUptime(uptime);
        return (T) this;
    }
    
    public ToStringHelper toStringHelper(Object self) {
    	return MoreObjects.toStringHelper(self)
    	        .add("instanceId", instanceId)
    	        .add("exploratoryName", exploratoryName)
    	        .add("exploratoryTemplateName", exploratoryTemplateName)
    	        .add("status", status)
    	        .add("errorMessage", errorMessage)
    	        .add("uptime", uptime);
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
