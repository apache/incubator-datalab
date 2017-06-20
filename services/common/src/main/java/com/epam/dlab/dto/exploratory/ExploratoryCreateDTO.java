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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

public class ExploratoryCreateDTO extends ExploratoryBaseDTO<ExploratoryCreateDTO> {
    @JsonProperty("aws_notebook_instance_type")
    private String notebookInstanceType;
    @JsonProperty("aws_security_groups_ids")
    private String securityGroupIds;
    @JsonProperty("git_creds")
    private List<ExploratoryGitCreds> gitCreds;

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

    public ExploratoryCreateDTO withAwsSecurityGroupIds(String securityGroupIds) {
        setSecurityGroupIds(securityGroupIds);
        return this;
    }

    /** Return the list of GIT credentials.
     */
    public List<ExploratoryGitCreds> getGitCreds() {
        return gitCreds;
    }

    /** Set the list of GIT credentials.
     */
    public void setGitCreds(List<ExploratoryGitCreds> gitCreds) {
        this.gitCreds = gitCreds;
    }

    /** Set the list of GIT credentials and return this object.
     */
    public ExploratoryCreateDTO withGitCreds(List<ExploratoryGitCreds> gitCreds) {
        setGitCreds(gitCreds);
        return this;
    }
    
    
    @Override
    public ToStringHelper toStringHelper(Object self) {
    	return super.toStringHelper(self)
    			.add("notebookInstanceType", notebookInstanceType)
    			.add("securityGroupIds", securityGroupIds)
    			.add("gitCreds", gitCreds);
    }
}
