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
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.Date;

public abstract class StatusBaseDTO<T extends StatusBaseDTO<?>> {
    @JsonProperty("request_id")
    private String requestId;
    @JsonProperty
    private String user;
    @JsonProperty
    private String status;
    @JsonProperty("error_message")
    private String errorMessage;
    @JsonProperty("up_time")
    private Date uptime;

    @SuppressWarnings("unchecked")
    private final T self = (T) this;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public T withRequestId(String requestId) {
        setRequestId(requestId);
        return self;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public T withUser(String user) {
        setUser(user);
        return self;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public T withStatus(String status) {
        setStatus(status);
        return self;
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

    public T withErrorMessage(String errorMessage) {
        setErrorMessage(errorMessage);
        return self;
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
                .add("requestId", requestId)
                .add("user", user)
                .add("status", status)
                .add("errorMessage", errorMessage)
                .add("uptime", uptime);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
