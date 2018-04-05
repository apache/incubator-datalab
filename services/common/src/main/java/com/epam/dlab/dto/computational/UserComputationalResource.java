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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserComputationalResource {
    @JsonProperty("computational_name")
    private String computationalName;
    @JsonProperty("computational_id")
    private String computationalId;
    @JsonProperty("image")
    private String imageName;
    @JsonProperty("template_name")
    private String templateName;
    @JsonProperty
    private String status;
    @JsonProperty("up_time")
    private Date uptime;
	@JsonProperty("scheduler_data")
	private SchedulerJobDTO schedulerData;
	@JsonProperty("reupload_key_required")
	private boolean reuploadKeyRequired = false;
}
