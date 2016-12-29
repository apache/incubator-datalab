/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.dto.edge;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EdgeCreateDTO extends EdgeBaseDTO<EdgeCreateDTO> {
    @JsonProperty("edge_vpc_id")
    private String vpcId;
    @JsonProperty("creds_subnet_id")
    private String subnetId;
    @JsonProperty("creds_iam_user")
    private String iamUser;
    @JsonProperty("creds_security_groups_ids")
    private String securityGroupIds;

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public EdgeCreateDTO withVpcId(String vpcId) {
        setVpcId(vpcId);
        return this;
    }

     public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public EdgeCreateDTO withSubnetId(String subnetId) {
        setSubnetId(subnetId);
        return this;
    }

    public String getIamUser() {
        return iamUser;
    }

    public void setIamUser(String iamUser) {
        this.iamUser = iamUser;
    }

    public EdgeCreateDTO withIamUser(String iamUser) {
        setIamUser(iamUser);
        return this;
    }

    public String getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(String securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }

    public EdgeCreateDTO withSecurityGroupIds(String securityGroupIds) {
        setSecurityGroupIds(securityGroupIds);
        return this;
    }

}
