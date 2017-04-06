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

package com.epam.dlab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

abstract public class ResourceBaseDTO<T extends ResourceBaseDTO<?>> {
    @JsonProperty("aws_region")
    private String awsRegion;
    @JsonProperty("aws_iam_user")
    private String awsIamUser;
    @JsonProperty("edge_user_name")
    private String edgeUserName;

    @SuppressWarnings("unchecked")
	private final T self = (T)this;

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public T withAwsRegion(String region) {
        setAwsRegion(region);
        return self;
    }

    public String getAwsIamUser() {
        return awsIamUser;
    }

    public void setAwsIamUser(String awsIamUser) {
        this.awsIamUser = awsIamUser;
    }

    public T withAwsIamUser(String awsIamUser) {
        setAwsIamUser(awsIamUser);
        return self;
    }

    public String getEdgeUserName() {
        return edgeUserName;
    }

    public void setEdgeUserName(String edgeUserName) {
        this.edgeUserName = edgeUserName;
    }

    public T withEdgeUserName(String edgeUserName) {
        setEdgeUserName(edgeUserName);
        return self;
    }

    public ToStringHelper toStringHelper(Object self) {
    	return MoreObjects.toStringHelper(self)
    	        .add("awsRegion", awsRegion)
    	        .add("awsIamUser", awsIamUser)
    	        .add("edgeUserName", edgeUserName);
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
