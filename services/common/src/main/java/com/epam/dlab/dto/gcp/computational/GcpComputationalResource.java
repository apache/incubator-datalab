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

package com.epam.dlab.dto.gcp.computational;

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
public class GcpComputationalResource extends UserComputationalResource {

    @JsonProperty("instance_id")
    private String instanceId;
    @JsonProperty("master_node_shape")
    private String masterShape;
    @JsonProperty("slave_node_shape")
    private String slaveShape;
    @JsonProperty("total_slave_instance_number")
    private String slaveNumber;
    @JsonProperty("total_master_instance_number")
    private String masterNumber;
	@JsonProperty("total_preemptible_number")
	private String preemptibleNumber;
    @JsonProperty("dataproc_version")
    private String version;

    @Builder
	public GcpComputationalResource(String computationalName, String computationalId, String imageName,
									String templateName, String status, Date uptime,
									SchedulerJobDTO schedulerJobData, boolean reuploadKeyRequired,
									String instanceId, String masterShape, String slaveShape, String slaveNumber,
									String masterNumber,String preemptibleNumber, String version) {
		super(computationalName, computationalId, imageName, templateName, status, uptime, schedulerJobData,
				reuploadKeyRequired);
        this.instanceId = instanceId;
        this.masterShape = masterShape;
        this.slaveShape = slaveShape;
        this.slaveNumber = slaveNumber;
        this.masterNumber = masterNumber;
        this.version = version;
        this.preemptibleNumber = preemptibleNumber;
    }
}
