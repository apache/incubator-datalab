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

import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.List;

import org.hibernate.validator.constraints.NotBlank;

/** Stores info about the installation of libraries. 
 * */
public class ExploratoryLibInstallFormDTO {
    @NotBlank
    @JsonProperty("notebook_name")
    private String notebookName;

    @JsonProperty
    private List<LibInstallDTO> libs;

    /** Returns the name of notebook. */
    public String getNotebookName() {
        return notebookName;
    }

    /** Returns the name of libraries. */
    public List<LibInstallDTO> getLibs() {
        return libs;
    }

    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("notebookName", notebookName)
    			.add("libs", libs)
    			.toString();
    }
}
