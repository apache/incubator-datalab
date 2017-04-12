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
import com.google.common.base.MoreObjects.ToStringHelper;

public class ResourceSysBaseDTO<T extends ResourceSysBaseDTO<?>> extends ResourceBaseDTO<T> {
    @JsonProperty("conf_service_base_name")
    private String serviceBaseName;
    @JsonProperty("conf_tag_resource_id")
    private String confTagResourceId;
    @JsonProperty("conf_os_user")
    private String confOsUser;
    @JsonProperty("conf_os_family")
    private String confOsFamily;

    @SuppressWarnings("unchecked")
	private final T self = (T)this;

    public String getServiceBaseName() {
        return serviceBaseName;
    }

    public void setServiceBaseName(String serviceBaseName) {
        this.serviceBaseName = serviceBaseName;
    }

    public T withServiceBaseName(String serviceBaseName) {
        setServiceBaseName(serviceBaseName);
        return self;
    }
    
    public String getConfTagResourceId() {
        return confTagResourceId;
    }

    public void setConfTagResourceId(String confTagResourceId) {
        this.confTagResourceId = confTagResourceId;
    }

    public T withConfTagResourceId(String confTagResourceId) {
        setConfTagResourceId(confTagResourceId);
        return self;
    }
    
    public String getConfOsUser() {
        return confOsUser;
    }

    public void setConfOsUser(String confOsUser) {
        this.confOsUser = confOsUser;
    }

    public T withConfOsUser(String confOsUser) {
        setConfOsUser(confOsUser);
        return self;
    }
    
    public String getConfOsFamily() {
        return confOsFamily;
    }

    public void setConfOsFamily(String confOsFamily) {
        this.confOsFamily = confOsFamily;
    }

    public T withConfOsFamily(String confOsFamily) {
        setConfOsFamily(confOsFamily);
        return self;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
    	return super.toStringHelper(self)
    	        .add("serviceBaseName", serviceBaseName)
    	        .add("confOsUser", confOsUser)
    	        .add("confOsFamily", confOsFamily);
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
