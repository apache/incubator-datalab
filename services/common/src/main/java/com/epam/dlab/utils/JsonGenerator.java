/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.utils;

import com.epam.dlab.dto.ResourceBaseDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonGenerator {

    private JsonGenerator() { }

    public static String generateJson(ResourceBaseDTO<?> resourceBaseDTO) throws JsonProcessingException {
        return generateJson(resourceBaseDTO, false);
    }

    public static String generateJson(ResourceBaseDTO<?> resourceBaseDTO, boolean pretty) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if(pretty) {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resourceBaseDTO);
        }
        else {
            return objectMapper.writeValueAsString(resourceBaseDTO);
        }
    }

}
