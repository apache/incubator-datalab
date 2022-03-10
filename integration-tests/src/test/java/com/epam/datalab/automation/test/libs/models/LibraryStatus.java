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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class LibraryStatus {
	    @JsonProperty
	    private String resource;
	    @JsonProperty
	    private String resourceType;
	    @JsonProperty
	    private String status;
	    @JsonProperty
	    private String error;
	    
		public String getResource() {
			return resource;
		}
	    public String getResourceType() { return resourceType;}
	    public String getStatus() {
			return status;
		}
		public String getError() {
			return error;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((error == null) ? 0 : error.hashCode());
			result = prime * result + ((resource == null) ? 0 : resource.hashCode());
			result = prime * result + ((status == null) ? 0 : status.hashCode());
			result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LibraryStatus other = (LibraryStatus) obj;
			if (error == null) {
				if (other.error != null)
					return false;
			} else if (!error.equals(other.error))
				return false;
			if (resource == null) {
				if (other.resource != null)
					return false;
			} else if (!resource.equals(other.resource))
				return false;
			if (status == null) {
				if (other.status != null)
					return false;
			} else if (!status.equals(other.status))
				return false;
			if (resourceType == null) {
				return other.resourceType == null;
			} else return resourceType.equals(other.resourceType);
		}
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("resource", resource)
					.add("resourceType", resourceType)
					.add("status", status)
					.add("error", error)
					.toString();
		}
	    
	    
}
