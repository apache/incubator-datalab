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

import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

public class ComputationalTerminateDTO extends ComputationalBase<ComputationalTerminateDTO> {
    @JsonProperty("emr_cluster_name")
    private String clusterName;
    @JsonProperty("notebook_instance_name")
    private String notebookInstanceName;
    @JsonProperty("conf_key_dir")
    private String confKeyDir;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public ComputationalTerminateDTO withClusterName(String clusterName) {
        setClusterName(clusterName);
        return this;
    }

    public String getNotebookInstanceName() {
        return notebookInstanceName;
    }

    public void setNotebookInstanceName(String notebookInstanceName) {
        this.notebookInstanceName = notebookInstanceName;
    }

    public ComputationalTerminateDTO withNotebookInstanceName(String notebookInstanceName) {
        setNotebookInstanceName(notebookInstanceName);
        return this;
    }

    public String getConfKeyDir() {
        return confKeyDir;
    }

    public void setConfKeyDir(String confKeyDir) {
        this.confKeyDir = confKeyDir;
    }

    public ComputationalTerminateDTO withConfKeyDir(String confKeyDir) {
        setConfKeyDir(confKeyDir);
        return this;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
    	return super.toStringHelper(self)
    	        .add("clusterName", clusterName)
    	        .add("notebookInstanceName", notebookInstanceName)
    	        .add("confKeyDir", confKeyDir);
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
