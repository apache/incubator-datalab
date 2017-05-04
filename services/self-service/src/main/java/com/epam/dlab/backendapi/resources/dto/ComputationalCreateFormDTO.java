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

/** Stores info about creation of the computational resource.
 */
public class ComputationalCreateFormDTO {
    @NotBlank
    @JsonProperty
    private String image;

    @NotBlank
    @JsonProperty("template_name")
    private String templateName;

    @NotBlank
    @JsonProperty
    private String name;

    @NotBlank
    @JsonProperty("emr_instance_count")
    private String instanceCount;

    @NotBlank
    @JsonProperty("emr_master_instance_type")
    private String masterInstanceType;

    @NotBlank
    @JsonProperty("emr_slave_instance_type")
    private String slaveInstanceType;

    @JsonProperty("emr_slave_instance_spot")
    private Boolean slaveInstanceSpot = false;

    @JsonProperty("emr_slave_instance_spot_pct_price")
    private Integer slaveInstanceSpotPctPrice;

    @NotBlank
    @JsonProperty("emr_version")
    private String version;

    @NotBlank
    @JsonProperty("notebook_name")
    private String notebookName;

    /** Returns the image name of EMR. */
    public String getImage() {
    	return image;
    }

    /** Sets the image name of EMR. */
    public void setImage(String image) {
    	this.image = image;
    }

    /** Returns name of template. */
    public String getTemplateName() {
    	return templateName;
    }

    /** Sets name of template. */
    public void setTemplateName(String templateName) {
    	this.templateName = templateName;
    }

    /** Returns the name of resource. */
    public String getName() {
        return name;
    }

    /** Returns the number of instances. */
    public String getInstanceCount() {
        return instanceCount;
    }

    /** Returns the type of master instance. */
    public String getMasterInstanceType() {
        return masterInstanceType;
    }

    /** Returns the type of slave instances. */
    public String getSlaveInstanceType() {
        return slaveInstanceType;
    }

    /** Returns the flag is slave spot or not. */
    public Boolean getSlaveInstanceSpot() {
        return slaveInstanceSpot;
    }

    /** Returns the version of resource. */
    public String getVersion() {
        return version;
    }

    /** Returns the name of notebook. */
    public String getNotebookName() {
        return notebookName;
    }

    public Integer getSlaveInstanceSpotPctPrice() {
        return slaveInstanceSpotPctPrice;
    }

    @Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("name", name)
    			.add("notebookName", notebookName)
    			.add("version", version)
    			.add("masterInstanceType", masterInstanceType)
    	        .add("slaveInstanceType", slaveInstanceType)
                .add("slaveInstanceSpot", slaveInstanceSpot)
                .add("slaveInstanceSpotPctPrice", slaveInstanceSpotPctPrice)
    			.add("instanceCount", instanceCount)
    	        .toString();
    }
}
