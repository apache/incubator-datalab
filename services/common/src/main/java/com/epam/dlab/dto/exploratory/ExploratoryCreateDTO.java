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

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExploratoryCreateDTO extends ExploratoryBaseDTO<ExploratoryCreateDTO> {
    @JsonProperty("notebook_instance_type")
    private String notebookInstanceType;
    @JsonProperty("creds_security_groups_ids")
    private String securityGroupIds;

    public String getNotebookInstanceType() {
        return notebookInstanceType;
    }

    public void setNotebookInstanceType(String notebookInstanceType) {
        this.notebookInstanceType = notebookInstanceType;
    }

    public ExploratoryCreateDTO withNotebookInstanceType(String notebookInstanceType) {
        setNotebookInstanceType(notebookInstanceType);
        return this;
    }

    public String getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(String securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }

    public ExploratoryCreateDTO withSecurityGroupIds(String securityGroupIds) {
        setSecurityGroupIds(securityGroupIds);
        return this;
    }

}
