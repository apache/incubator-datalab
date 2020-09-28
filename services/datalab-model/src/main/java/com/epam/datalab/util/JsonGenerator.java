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

package com.epam.datalab.util;

import com.epam.datalab.dto.ResourceBaseDTO;
import com.epam.datalab.dto.base.CloudSettings;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonGenerator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .addMixIn(ResourceBaseDTO.class, CloudSettingsUnwrapping.class);

    private JsonGenerator() {
    }

    public static String generateJson(ResourceBaseDTO<?> resourceBaseDTO) throws JsonProcessingException {
        return generateJson(resourceBaseDTO, false);
    }

    private static String generateJson(ResourceBaseDTO<?> resourceBaseDTO, boolean pretty) throws
            JsonProcessingException {
        if (pretty) {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(resourceBaseDTO);
        } else {
            return OBJECT_MAPPER.writeValueAsString(resourceBaseDTO);
        }
    }

    private abstract static class CloudSettingsUnwrapping {
        @JsonUnwrapped
        private CloudSettings cloudSettings;
    }
}
