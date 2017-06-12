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

package com.epam.dlab.dto.exploratory;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/** Stores info about libraries.
 */
public class LibInstallDTO {
    @JsonProperty
    private String group;

    @JsonProperty
    private String name;
    
    @JsonProperty
    private String version;
    
    @JsonProperty
    private String status;

    @JsonProperty("error_message")
    private String errorMessage;

    /** Returns the group name of library. */
    public String getGroup() {
    	return group;
    }

    /** Sets the group name of library. */
    public void setGroup(String group) {
    	this.group = group;
    }

    public LibInstallDTO withGroup(String group) {
        setGroup(group);
        return this;
    }

    /** Returns name of library. */
    public String getName() {
        return name;
    }

    /** Sets name of library. */
    public void setName(String name) {
        this.name = name;
    }
    
    public LibInstallDTO withName(String name) {
        setName(name);
        return this;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LibInstallDTO withVersion(String version) {
        setVersion(version);
        return this;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LibInstallDTO withStatus(String status) {
        setStatus(status);
        return this;
    }
    
    /** Returns the error message. */
    public String getErrorMessage() {
    	return errorMessage;
    }

    /** Sets the error message. */
    public void setErrorMessage(String errorMessage) {
    	this.errorMessage = errorMessage;
    }

    public LibInstallDTO withErrorMessage(String errorMessage) {
    	setErrorMessage(errorMessage);
        return this;
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (obj == null && !(obj instanceof LibInstallDTO)) {
    		return false;
    	}
    	LibInstallDTO lib = (LibInstallDTO)obj;
    	return (StringUtils.equals(group, lib.group) &&
    			StringUtils.equals(name, lib.name) &&
    			StringUtils.equals(version, lib.version) &&
    			StringUtils.equals(status, lib.status) &&
    			StringUtils.equals(errorMessage, lib.errorMessage));
    }

    
    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("group", group)
    			.add("name", name)
    			.add("version", version)
    			.add("status", status)
    			.add("errorMessage", errorMessage)
    			.toString();
    }
}
