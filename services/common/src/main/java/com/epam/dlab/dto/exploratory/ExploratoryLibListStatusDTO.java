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

import com.epam.dlab.dto.StatusBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

/** Stores the info about image libraries.
 */
public class ExploratoryLibListStatusDTO extends StatusBaseDTO<ExploratoryLibListStatusDTO> {

    @JsonProperty
    private String libs;

    @JsonProperty
    private String imageName;

    
    /** Return the list of libraries.
     */
    public String getLibs() {
        return libs;
    }

    /** Set the list of libraries.
     */
    public void setLibs(String libs) {
        this.libs = libs;
    }

    /** Set the list of libraries and return this object.
     */
    public ExploratoryLibListStatusDTO withLibs(String libs) {
        setLibs(libs);
        return this;
    }

    /** Return the name of image.
     */
    public String getImageName() {
        return imageName;
    }

    /** Set the name of image.
     */
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    /** Set the name of image and return this object.
     */
    public ExploratoryLibListStatusDTO withImageName(String imageName) {
        setImageName(imageName);
        return this;
    }
    

    @Override
    public ToStringHelper toStringHelper(Object self) {
    	return super.toStringHelper(self)
    			.add("imageName", imageName)
    			.add("libs", (libs == null ? "null" : "..."));
    }
}
