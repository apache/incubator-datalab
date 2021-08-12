/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.epam.datalab.backendapi.resources.dto;

import com.epam.datalab.model.systeminfo.DiskInfo;
import com.epam.datalab.model.systeminfo.MemoryInfo;
import com.epam.datalab.model.systeminfo.OsInfo;
import com.epam.datalab.model.systeminfo.ProcessorInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class SystemInfoDto {

    @JsonProperty
    private OsInfo osInfo;
    @JsonProperty
    private ProcessorInfo processorInfo;
    @JsonProperty
    private MemoryInfo memoryInfo;
    @JsonProperty
    private List<DiskInfo> disksInfo;

}
