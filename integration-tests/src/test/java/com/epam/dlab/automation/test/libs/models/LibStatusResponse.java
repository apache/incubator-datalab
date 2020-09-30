/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.automation.test.libs.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LibStatusResponse {
    @JsonProperty
    private String group;
    @JsonProperty
    private String name;
    @JsonProperty
    private String version;
    @JsonProperty
    private List<LibraryStatus> status;

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public List<LibraryStatus> getStatus() {
        return status;
    }


    @Override
    public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LibStatusResponse that = (LibStatusResponse) o;

		return (group != null ? group.equals(that.group) : that.group == null) && (name != null ? name.equals(that
				.name) : that.name == null) && (version != null ? version.equals(that.version) : that.version == null)
				&& (status != null ? status.equals(that.status) : that.status == null);
	}

    @Override
    public int hashCode() {
        int result = group != null ? group.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("group", group)
                .add("name", name)
                .add("version", version)
                .add("status", status)
                .toString();
    }
}

