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


package com.epam.datalab.dto.imagemetadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties
public class ExploratoryMetadataDTO extends ImageMetadataDTO {
    protected String image;
    @JsonProperty("exploratory_environment_versions")
    private List<ExploratoryEnvironmentVersion> exploratoryEnvironmentVersions;
    @JsonProperty("exploratory_environment_shapes")
    private HashMap<String, List<ComputationalResourceShapeDto>> exploratoryEnvironmentShapes;
    @JsonProperty("exploratory_environment_images")
    private List<ExploratoryEnvironmentImages> exploratoryEnvironmentImages;
    @JsonProperty("request_id")
    private String requestId;
    private List<String> computationGPU;

    public ExploratoryMetadataDTO(String imageName) {
        this.image = imageName;
        setImageType(ImageType.EXPLORATORY);
    }
}
