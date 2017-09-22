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

package com.epam.dlab.dto.aws;

import com.epam.dlab.dto.base.CloudSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AwsCloudSettings extends CloudSettings {

    @JsonProperty("aws_iam_user")
    private String awsIamUser;
    @JsonProperty("aws_region")
    private String awsRegion;
    @JsonProperty("aws_subnet_id")
    private String awsSubnetId;
    @JsonProperty("aws_security_groups_ids")
    private String awsSecurityGroupIds;
    @JsonProperty("aws_vpc_id")
    private String awsVpcId;
    @JsonProperty("conf_tag_resource_id")
    private String confTagResourceId;

    @Builder
    public AwsCloudSettings(String awsIamUser, String awsRegion, String awsSubnetId,
                            String awsSecurityGroupIds, String awsVpcId, String confTagResourceId) {

        this.awsIamUser = awsIamUser;
        this.awsRegion = awsRegion;
        this.awsSubnetId = awsSubnetId;
        this.awsSecurityGroupIds = awsSecurityGroupIds;
        this.awsVpcId = awsVpcId;
        this.confTagResourceId = confTagResourceId;
    }

    @Override
    @JsonIgnore
    public String getIamUser() {
        return awsIamUser;
    }
}
