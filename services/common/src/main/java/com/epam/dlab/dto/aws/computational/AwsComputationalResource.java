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

package com.epam.dlab.dto.aws.computational;

import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;

/**
 * Stores info about the user's computational resources for notebook.
 */
@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class AwsComputationalResource extends UserComputationalResource {

    @JsonProperty("instance_id")
    private String instanceId;
    @JsonProperty("master_node_shape")
    private String masterShape;
    @JsonProperty("slave_node_shape")
    private String slaveShape;
    @JsonProperty("slave_node_spot")
    private Boolean slaveSpot = false;
    @JsonProperty("slave_node_spot_pct_price")
    private Integer slaveSpotPctPrice;
    @JsonProperty("total_instance_number")
    private String slaveNumber;
    @JsonProperty("emr_version")
    private String version;

    @Builder
    public AwsComputationalResource(String computationalName, String computationalId, String imageName,
									String templateName, String status, Date uptime,
									SchedulerJobDTO schedulerJobData, boolean reuploadKeyRequired,
									String instanceId, String masterShape, String slaveShape, Boolean slaveSpot,
									Integer slaveSpotPctPrice, String slaveNumber, String version) {

		super(computationalName, computationalId, imageName, templateName, status, uptime, schedulerJobData,
				reuploadKeyRequired);
        this.instanceId = instanceId;
        this.masterShape = masterShape;
        this.slaveShape = slaveShape;
        this.slaveSpot = slaveSpot;
        this.slaveSpotPctPrice = slaveSpotPctPrice;
        this.slaveNumber = slaveNumber;
        this.version = version;
    }
}
