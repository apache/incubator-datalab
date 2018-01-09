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

import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class ComputationalCreateGcp extends ComputationalBase<ComputationalCreateGcp> {
    @JsonProperty("dataproc_master_count")
    private String masterInstanceCount;
    @JsonProperty("dataproc_slave_count")
    private String slaveInstanceCount;
    @JsonProperty("dataproc_master_instance_type")
    private String masterInstanceType;
    @JsonProperty("dataproc_slave_instance_type")
    private Boolean slaveInstanceType;
    @JsonProperty("dataproc_preemtible_count")
    private Integer preemtibleCount;
    @JsonProperty("dataproc_version")
    private String version;
}
