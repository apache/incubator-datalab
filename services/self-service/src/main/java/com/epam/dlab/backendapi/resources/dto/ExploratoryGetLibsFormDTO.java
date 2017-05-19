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

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/** Stores info for search the libraries. 
 * */
public class ExploratoryGetLibsFormDTO {
    @NotBlank
    @JsonProperty
    private String image;

    @NotBlank
    @JsonProperty
    private String group;

    @NotBlank
    @JsonProperty("start_with")
    private String startWith;


    /** Returns the name of image. */
    public String getImage() {
        return image;
    }

    /** Returns the name of group. */
    public String getGroup() {
        return group;
    }

    /** Returns the prefix of libarary's name. */
    public String getStartWith() {
        return startWith;
    }


    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("image", image)
    			.add("group", group)
    			.add("startWith", startWith)
    			.toString();
    }
}
