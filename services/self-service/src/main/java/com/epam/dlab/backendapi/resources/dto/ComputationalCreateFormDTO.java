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
import org.hibernate.validator.constraints.NotBlank;

public class ComputationalCreateFormDTO {
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

    @NotBlank
    @JsonProperty("emr_version")
    private String version;

    @NotBlank
    @JsonProperty("notebook_name")
    private String notebookName;

    public String getName() {
        return name;
    }

    public String getInstanceCount() {
        return instanceCount;
    }

    public String getMasterInstanceType() {
        return masterInstanceType;
    }

    public String getSlaveInstanceType() {
        return slaveInstanceType;
    }

    public String getVersion() {
        return version;
    }

    public String getNotebookName() {
        return notebookName;
    }
}
