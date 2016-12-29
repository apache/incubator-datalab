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

import java.util.Date;

public class StatusBaseDTO<T extends StatusBaseDTO<?>> {
    @JsonProperty
    private String user;
    @JsonProperty("exploratory_name")
    private String exploratoryName;
    @JsonProperty
    private String status;
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
}
