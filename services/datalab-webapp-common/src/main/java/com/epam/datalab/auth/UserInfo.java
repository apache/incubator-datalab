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

package com.epam.datalab.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo implements Principal {

    private final String username;
    private final String accessToken;
    private String refreshToken;
    private final Set<String> roles = new HashSet<>();
    private final Map<String, String> keys = new HashMap<>();

    @JsonProperty
    private String firstName;
    @JsonProperty
    private String lastName;
    @JsonProperty
    private String remoteIp;
    @JsonProperty
    private boolean awsUser = false;

    @JsonCreator
    public UserInfo(@JsonProperty("username") String username,
                    @JsonProperty("access_token") String accessToken) {
        this.username = (username == null ? null : username.toLowerCase());
        this.accessToken = accessToken;
    }

    @Override
    @JsonProperty("username")
    public String getName() {
        return username;
    }

    public String getSimpleName() {
        return (username == null ? null : username.replaceAll("@.*", ""));
    }

    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    @JsonProperty("refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @JsonProperty("roles")
    public Set<String> getRoles() {
        return roles;
    }

    //@JsonSetter("roles")
    public void addRoles(Collection<String> r) {
        roles.addAll(r);
    }

    @JsonSetter("roles")
    public void addRoles(String[] r) {
        roles.addAll(Arrays.asList(r));
    }

    public void addRole(String role) {
        roles.add(role);
    }


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public UserInfo withToken(String token) {
        UserInfo newInfo = new UserInfo(username, token);
        roles.forEach(newInfo::addRole);
        newInfo.firstName = this.firstName;
        newInfo.lastName = this.lastName;
        newInfo.remoteIp = this.remoteIp;
        newInfo.awsUser = this.awsUser;
        newInfo.setKeys(this.getKeys());
        return newInfo;
    }

    public boolean isAwsUser() {
        return awsUser;
    }

    public void setAwsUser(boolean awsUser) {
        this.awsUser = awsUser;
    }

    public Map<String, String> getKeys() {
        return keys;
    }

    public void addKey(String id, String status) {
        keys.put(id, status);
    }

    @JsonSetter("keys")
    public void setKeys(Map<String, String> awsKeys) {
        awsKeys.forEach(keys::put);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserInfo userInfo = (UserInfo) o;

        if (awsUser != userInfo.awsUser) {
            return false;
        }
        if (username != null ? !username.equals(userInfo.username) : userInfo.username != null) {
            return false;
        }
        if (accessToken != null ? !accessToken.equals(userInfo.accessToken) : userInfo.accessToken != null)
            return false;
        if (!roles.equals(userInfo.roles)) {
            return false;
        }
        if (!keys.equals(userInfo.keys)) {
            return false;
        }
        if (firstName != null ? !firstName.equals(userInfo.firstName) : userInfo.firstName != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(userInfo.lastName) : userInfo.lastName != null) {
            return false;
        }
        return remoteIp != null ? remoteIp.equals(userInfo.remoteIp) : userInfo.remoteIp == null;
    }


    @Override
    public int hashCode() {
        return Objects.hash(username,
                accessToken,
                roles,
                keys,
                firstName,
                lastName,
                remoteIp,
                awsUser);
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "username='" + username + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", roles=" + roles +
                ", keys=" + keys.keySet() +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", remoteIp='" + remoteIp + '\'' +
                ", awsUser=" + awsUser +
                '}';
    }
}
