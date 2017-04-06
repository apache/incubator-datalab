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

package com.epam.dlab.dto.computational;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

public class ComputationalCreateDTO extends ComputationalBaseDTO<ComputationalCreateDTO> {
    @JsonProperty("emr_instance_count")
    private String instanceCount;
    @JsonProperty("emr_master_instance_type")
    private String masterInstanceType;
    @JsonProperty("emr_slave_instance_type")
    private String slaveInstanceType;
    @JsonProperty("emr_slave_instance_spot")
    private Boolean slaveInstanceSpot;
    @JsonProperty("emr_slave_instance_spot_pct_price")
    private Integer slaveInstanceSpotPctPrice;
    @JsonProperty("emr_version")
    private String version;
    @JsonProperty("notebook_instance_name")
    private String notebookInstanceName;
    @JsonProperty("notebook_template_name")
    private String notebookTemplateName;

    public String getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(String instanceCount) {
        this.instanceCount = instanceCount;
    }

    public ComputationalCreateDTO withInstanceCount(String instanceCount) {
        setInstanceCount(instanceCount);
        return this;
    }

    public String getMasterInstanceType() {
        return masterInstanceType;
    }

    public void setMasterInstanceType(String masterInstanceType) {
        this.masterInstanceType = masterInstanceType;
    }

    public ComputationalCreateDTO withMasterInstanceType(String masterInstanceType) {
        setMasterInstanceType(masterInstanceType);
        return this;
    }

    public String getSlaveInstanceType() {
        return slaveInstanceType;
    }

    public void setSlaveInstanceType(String slaveInstanceType) {
        this.slaveInstanceType = slaveInstanceType;
    }

    public ComputationalCreateDTO withSlaveInstanceType(String slaveInstanceType) {
        setSlaveInstanceType(slaveInstanceType);
        return this;
    }

    public Boolean getSlaveInstanceSpot() {
        return slaveInstanceSpot;
    }

    public void setSlaveInstanceSpot(Boolean slaveInstanceSpot) {
        this.slaveInstanceSpot = slaveInstanceSpot;
    }

    public ComputationalCreateDTO withSlaveInstanceSpot(Boolean slaveInstanceSpot) {
        setSlaveInstanceSpot(slaveInstanceSpot);
        return this;
    }

    public Integer getSlaveInstanceSpotPctPrice() {
        return slaveInstanceSpotPctPrice;
    }

    public void setSlaveInstanceSpotPctPrice(Integer slaveInstanceSpotPctPrice) {
        this.slaveInstanceSpotPctPrice = slaveInstanceSpotPctPrice;
    }

    public ComputationalCreateDTO withSlaveInstanceSpotPctPrice(Integer slaveInstanceSpotPctPrice) {
        setSlaveInstanceSpotPctPrice(slaveInstanceSpotPctPrice);
        return this;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ComputationalCreateDTO withVersion(String version) {
        setVersion(version);
        return this;
    }

    public String getNotebookInstanceName() {
        return notebookInstanceName;
    }

    public void setNotebookInstanceName(String notebookInstanceName) {
        this.notebookInstanceName = notebookInstanceName;
    }

    public ComputationalCreateDTO withNotebookInstanceName(String notebookInstanceName) {
        setNotebookInstanceName(notebookInstanceName);
        return this;
    }
    
    public String getNotebookTemplateName() {
        return notebookTemplateName;
    }

    public void setNotebookTemplateName(String notebookTemplateName) {
        this.notebookTemplateName = notebookTemplateName;
    }

    public ComputationalCreateDTO withNotebookTemplateName(String notebookTemplateName) {
    	setNotebookTemplateName(notebookTemplateName);
        return this;
    }
    
    @Override
    public ToStringHelper toStringHelper(Object self) {
    	return super.toStringHelper(self)
    	        .add("notebookInstanceName", notebookInstanceName)
    	        .add("notebookTemplateName", notebookTemplateName)
    	        .add("version", version)
    	        .add("masterInstanceType", masterInstanceType)
    	        .add("slaveInstanceType", slaveInstanceType)
    	        .add("slaveInstanceSpot", slaveInstanceSpot)
    	        .add("slaveInstanceSpotPctPrice", slaveInstanceSpotPctPrice)
    	        .add("instanceCount", instanceCount);
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
