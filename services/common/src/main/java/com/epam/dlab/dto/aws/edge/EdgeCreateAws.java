/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.dto.aws.edge;

import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

public class EdgeCreateAws extends ResourceSysBaseDTO<EdgeCreateAws> {
    @JsonProperty("aws_vpc_id")
    private String awsVpcId;
    @JsonProperty("aws_subnet_id")
    private String awsSubnetId;
    @JsonProperty("aws_security_groups_ids")
    private String awsSecurityGroupIds;
    @JsonProperty("edge_elastic_ip")
    private String edgeElasticIp;

    public String getAwsVpcId() {
        return awsVpcId;
    }

    public void setAwsVpcId(String awsVpcId) {

        this.awsVpcId = awsVpcId;
    }

    public EdgeCreateAws withAwsVpcId(String awsVpcId) {
        setAwsVpcId(awsVpcId);
        return this;
    }

     public String getAwsSubnetId() {
        return awsSubnetId;
    }

    public void setAwsSubnetId(String awsSubnetId) {
        this.awsSubnetId = awsSubnetId;
    }

    public EdgeCreateAws withAwsSubnetId(String awsSubnetId) {
        setAwsSubnetId(awsSubnetId);
        return this;
    }

    public String getAwsSecurityGroupIds() {
        return awsSecurityGroupIds;
    }

    public void setAwsSecurityGroupIds(String awsSecurityGroupIds) {
        this.awsSecurityGroupIds = awsSecurityGroupIds;
    }

    public EdgeCreateAws withAwsSecurityGroupIds(String awsSecurityGroupIds) {
        setAwsSecurityGroupIds(awsSecurityGroupIds);
        return this;
    }

    public String getEdgeElasticIp() {
        return edgeElasticIp;
    }

    public void setEdgeElasticIp(String edgeElasticIp) {
        this.edgeElasticIp = edgeElasticIp;
    }

    public EdgeCreateAws withEdgeElasticIp(String edgeElasticIp) {
    	setEdgeElasticIp(edgeElasticIp);
        return this;
    }

    @Override
    public ToStringHelper toStringHelper(Object self) {
    	return super.toStringHelper(self)
    	        .add("awsVpcId", awsVpcId)
    	        .add("awsSubnetId", awsSubnetId)
    	        .add("awsSecurityGroupIds", awsSecurityGroupIds)
    	        .add("edgeElasticIp", edgeElasticIp);
    }

    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
