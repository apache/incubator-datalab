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

package com.epam.dlab.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class UserInfo implements Principal {

    private final String username;
    private final String accessToken;
    private final Set<String> roles = new HashSet<>();

    @JsonProperty
    private String firstName;
    @JsonProperty
    private String lastName;
    @JsonProperty
    private String remoteIp;
    @JsonProperty
    private boolean awsUser = false;

    @JsonCreator
    public UserInfo(@JsonProperty("username") String username, @JsonProperty("access_token") String accessToken) {
        this.username = username;
        this.accessToken = accessToken;
    }

    @Override
    @JsonProperty("username")
    public String getName() {
        return username;
    }

    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    @JsonProperty("roles")
    public Collection<String> getRoles() {
        return roles;
    }

    //@JsonSetter("roles")
    public void addRoles(Collection<String> roles) {
        roles.addAll(roles);
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
        UserInfo newInfo  = new UserInfo(username, token);
        roles.forEach(role -> newInfo.addRole(role));
        newInfo.firstName = this.firstName;
        newInfo.lastName  = this.lastName;
        newInfo.remoteIp  = this.remoteIp;
        newInfo.awsUser   = this.awsUser;
        return newInfo;
    }

    public boolean isAwsUser() {
        return awsUser;
    }

    public void setAwsUser(boolean awsUser) {
        this.awsUser = awsUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserInfo userInfo = (UserInfo) o;

        if (awsUser != userInfo.awsUser) return false;
        if (username != null ? !username.equals(userInfo.username) : userInfo.username != null) return false;
        if (accessToken != null ? !accessToken.equals(userInfo.accessToken) : userInfo.accessToken != null)
            return false;
        if (roles != null ? !roles.equals(userInfo.roles) : userInfo.roles != null) return false;
        if (firstName != null ? !firstName.equals(userInfo.firstName) : userInfo.firstName != null) return false;
        if (lastName != null ? !lastName.equals(userInfo.lastName) : userInfo.lastName != null) return false;
        return remoteIp != null ? remoteIp.equals(userInfo.remoteIp) : userInfo.remoteIp == null;

    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (accessToken != null ? accessToken.hashCode() : 0);
        result = 31 * result + (roles != null ? roles.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (remoteIp != null ? remoteIp.hashCode() : 0);
        result = 31 * result + (awsUser ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "username='" + username + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", roles=" + roles +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", remoteIp='" + remoteIp + '\'' +
                ", awsUser=" + awsUser +
                '}';
    }

}
