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

package com.epam.datalab.dto.exploratory;

import com.epam.datalab.dto.StatusBaseDTO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
public class ImageCreateStatusDTO extends StatusBaseDTO<ImageCreateStatusDTO> {

    private ImageCreateDTO imageCreateDTO;
    private String name;
    private String exploratoryName;
    private String project;
    private String endpoint;

    public ImageCreateStatusDTO withImageCreateDto(ImageCreateDTO imageCreateDto) {
        setImageCreateDTO(imageCreateDto);
        return this;
    }

    public ImageCreateStatusDTO withoutImageCreateDto() {
        setImageCreateDTO(new ImageCreateDTO());
        return this;
    }

    @Data
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @NoArgsConstructor
    public static class ImageCreateDTO {
        private String externalName;
        private String fullName;
        private String user;
        private String application;
        private ImageStatus status;
        private String ip;

        @JsonCreator
        public ImageCreateDTO(@JsonProperty("notebook_image_name") String externalName,
                              @JsonProperty("full_image_name") String fullName,
                              @JsonProperty("user_name") String user, @JsonProperty("application") String application,
                              @JsonProperty("status") ImageStatus status, @JsonProperty("ip") String ip) {
            this.externalName = externalName;
            this.fullName = fullName;
            this.user = user;
            this.application = application;
            this.status = status;
            this.ip = ip;
        }
    }
}
