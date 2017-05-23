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

package com.epam.dlab.backendapi.resources.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotBlank;

/** Stores info about libraries.
 */
public class LibInstallFormDTO {
    @NotBlank
    @JsonProperty
    private String group;

    @NotBlank
    @JsonProperty
    private String name;

    /** Returns the group name of library. */
    public String getGroup() {
    	return group;
    }

    /** Sets the group name of library. */
    public void setGroup(String group) {
    	this.group = group;
    }

    /** Returns name of library. */
    public String getName() {
        return name;
    }

    /** Sets name of library. */
    public void setName(String name) {
        this.name = name;
    }
    
    
    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("group", group)
    			.add("name", name)
    			.toString();
    }
}
