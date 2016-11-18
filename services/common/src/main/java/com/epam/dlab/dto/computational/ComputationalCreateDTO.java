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

public class ComputationalCreateDTO extends ComputationalBaseDTO<ComputationalCreateDTO> {
    @JsonProperty("emr_instance_count")
    private String instanceCount;
    @JsonProperty("emr_master_instance_type")
    private String masterInstanceType;
    @JsonProperty("emr_slave_instance_type")
    private String slaveInstanceType;
    @JsonProperty("emr_version")
    private String version;
    @JsonProperty("notebook_name")
    private String notebookName;
    @JsonProperty("creds_security_groups_ids")
    private String securityGroupIds;


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

    public String getNotebookName() {
        return notebookName;
    }

    public void setNotebookName(String notebookName) {
        this.notebookName = notebookName;
    }

    public ComputationalCreateDTO withNotebookName(String notebookName) {
        setNotebookName(notebookName);
        return this;
    }

    public String getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(String securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }

    public ComputationalCreateDTO withSecurityGroupIds(String securityGroupIds) {
        setSecurityGroupIds(securityGroupIds);
        return this;
    }
}
