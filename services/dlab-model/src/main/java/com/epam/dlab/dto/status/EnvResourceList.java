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

package com.epam.dlab.dto.status;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/** Describe the lists of resources (host, cluster, storage) for check status in Cloud.
 */
public class EnvResourceList {
    @JsonProperty("host")
    private List<EnvResource> hostList;
    @JsonProperty("cluster")
    private List<EnvResource> clusterList;

    /** Return the list of hosts. */
    public List<EnvResource> getHostList() {
        return hostList;
    }

    /** Set the list of hosts. */
    public void setHostList(List<EnvResource> hostList) {
        this.hostList = hostList;
    }

    /** Set the list of hosts. */
    public EnvResourceList withHostList(List<EnvResource> hostList) {
        setHostList(hostList);
        return this;
    }

    /** Return the list of clusters. */
    public List<EnvResource> getClusterList() {
        return clusterList;
    }

    /** Set the list of clusters. */
    public void setClusterList(List<EnvResource> clusterList) {
        this.clusterList = clusterList;
    }

    /** Set the list of clusters. */
    public EnvResourceList withClusterList(List<EnvResource> clusterList) {
        setClusterList(clusterList);
        return this;
    }

    public ToStringHelper toStringHelper(Object self) {
    	return MoreObjects.toStringHelper(self)
    	        .add("host", hostList)
    	        .add("cluster", clusterList);
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
