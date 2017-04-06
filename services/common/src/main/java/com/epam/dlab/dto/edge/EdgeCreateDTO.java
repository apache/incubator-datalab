/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.dto.edge;

import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;

public class EdgeCreateDTO extends ResourceSysBaseDTO<EdgeCreateDTO> {
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

    public EdgeCreateDTO withAwsVpcId(String awsVpcId) {
        setAwsVpcId(awsVpcId);
        return this;
    }

     public String getAwsSubnetId() {
        return awsSubnetId;
    }

    public void setAwsSubnetId(String awsSubnetId) {
        this.awsSubnetId = awsSubnetId;
    }

    public EdgeCreateDTO withAwsSubnetId(String awsSubnetId) {
        setAwsSubnetId(awsSubnetId);
        return this;
    }

    public String getAwsSecurityGroupIds() {
        return awsSecurityGroupIds;
    }

    public void setAwsSecurityGroupIds(String awsSecurityGroupIds) {
        this.awsSecurityGroupIds = awsSecurityGroupIds;
    }

    public EdgeCreateDTO withAwsSecurityGroupIds(String awsSecurityGroupIds) {
        setAwsSecurityGroupIds(awsSecurityGroupIds);
        return this;
    }

    public String getEdgeElasticIp() {
        return edgeElasticIp;
    }

    public void setEdgeElasticIp(String edgeElasticIp) {
        this.edgeElasticIp = edgeElasticIp;
    }

    public EdgeCreateDTO withEdgeElasticIp(String edgeElasticIp) {
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
