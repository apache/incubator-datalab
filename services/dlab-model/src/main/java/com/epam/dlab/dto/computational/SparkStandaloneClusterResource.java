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

package com.epam.dlab.dto.computational;

import com.epam.dlab.dto.SchedulerJobDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotBlank;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SparkStandaloneClusterResource extends UserComputationalResource {
    @NotBlank
    @JsonProperty("dataengine_instance_count")
    private String dataEngineInstanceCount;

    @NotBlank
    @JsonProperty("dataengine_instance_shape")
    private String dataEngineInstanceShape;

    @Builder
    public SparkStandaloneClusterResource(String computationalName, String computationalId, String imageName,
										  String templateName, String status, Date uptime,
										  SchedulerJobDTO schedulerJobData, boolean reuploadKeyRequired,
										  String dataEngineInstanceCount, String dataEngineInstanceShape) {

		super(computationalName, computationalId, imageName, templateName, status, uptime, schedulerJobData,
				reuploadKeyRequired);
        this.dataEngineInstanceCount = dataEngineInstanceCount;
        this.dataEngineInstanceShape = dataEngineInstanceShape;
    }
}
