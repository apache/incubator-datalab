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

package com.epam.dlab.dto.keyload;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadFileDTO {
    @JsonProperty
    private String user;
    @JsonProperty
    private String content;
    @JsonProperty("conf_service_base_name")
    private String serviceBaseName;
    @JsonProperty("security_group")
    private String securityGroup;

    public UploadFileDTO() {
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public UploadFileDTO withUser(String user) {
        setUser(user);
        return this;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UploadFileDTO withContent(String content) {
        setContent(content);
        return this;
    }

    public String getServiceBaseName() {
        return serviceBaseName;
    }

    public void setServiceBaseName(String serviceBaseName) {
        this.serviceBaseName = serviceBaseName;
    }

    public UploadFileDTO withServiceBaseName(String serviceBaseName) {
        setServiceBaseName(serviceBaseName);
        return this;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }

    public UploadFileDTO withSecurityGroup(String securityGroup) {
        setSecurityGroup(securityGroup);
        return this;
    }
}
