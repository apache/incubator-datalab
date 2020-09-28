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

package com.epam.datalab.dto.exploratory;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Describe GIT credentials.
 */
public class ExploratoryGitCreds implements Comparable<ExploratoryGitCreds> {

    @NotNull
    @JsonProperty
    private String hostname;

    @NotNull
    @JsonProperty
    private String username;

    @NotNull
    @JsonProperty
    private String email;

    @NotNull
    @JsonProperty
    private String login;

    @JsonProperty
    private String password;

    /**
     * Return the name of host.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Set the name of host.
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Set the name of host.
     */
    public ExploratoryGitCreds withHostname(String hostname) {
        setHostname(hostname);
        return this;
    }

    /**
     * Return the name of user.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set the name of user.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Set the name of user.
     */
    public ExploratoryGitCreds withUsername(String username) {
        setUsername(username);
        return this;
    }

    /**
     * Return the email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the email.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Set the email.
     */
    public ExploratoryGitCreds withEmail(String email) {
        setEmail(email);
        return this;
    }

    /**
     * Return the login.
     */
    public String getLogin() {
        return login;
    }

    /**
     * Set the login.
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Set the login.
     */
    public ExploratoryGitCreds withLogin(String login) {
        setLogin(login);
        return this;
    }

    /**
     * Return the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Set the password.
     */
    public ExploratoryGitCreds withPassword(String password) {
        setPassword(password);
        return this;
    }

    @Override
    public int compareTo(@Nullable ExploratoryGitCreds obj) {
        if (obj == null) {
            return 1;
        }
        return this.hostname.compareToIgnoreCase(obj.hostname);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof ExploratoryGitCreds && (this.compareTo((ExploratoryGitCreds) obj) == 0));

    }

    @Override
    public int hashCode() {
        return getHostname() != null ? getHostname().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ExploratoryGitCreds{" +
                "hostname='" + hostname + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", login='" + login + '\'' +
                ", password='" + "***" + '\'' +
                '}';
    }
}
