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

package com.epam.datalab.backendapi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.Date;

/**
 * Store request id info.
 *
 * @author Usein_Faradzhev
 */
public class RequestIdDTO {
    @JsonProperty("_id")
    private String id;

    @JsonProperty
    private String user;

    @JsonProperty
    private Date requestTime;

    @JsonProperty
    private Date expirationTime;

    /**
     * Return request id.
     */
    public String getId() {
        return id;
    }

    /**
     * Set request id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Set request id.
     */
    public RequestIdDTO withId(String id) {
        setId(id);
        return this;
    }

    /**
     * Return user name.
     */
    public String getUser() {
        return user;
    }

    /**
     * Set user name.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Set user name.
     */
    public RequestIdDTO withUser(String user) {
        setUser(user);
        return this;
    }

    /**
     * Return request time.
     */
    public Date getRequestTime() {
        return requestTime;
    }

    /**
     * Set request time.
     */
    public void setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
    }

    /**
     * Set request time.
     */
    public RequestIdDTO withRequestTime(Date requestTime) {
        setRequestTime(requestTime);
        return this;
    }

    /**
     * Return expiration time.
     */
    public Date getExpirationTime() {
        return expirationTime;
    }

    /**
     * Set expiration time.
     */
    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    /**
     * Set expiration time.
     */
    public RequestIdDTO withExpirationTime(Date expirationTime) {
        setExpirationTime(expirationTime);
        return this;
    }

    public ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("id", id)
                .add("user", user)
                .add("requestTime", requestTime)
                .add("expirationTime", expirationTime);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
