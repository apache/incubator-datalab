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

import com.epam.datalab.dto.aws.AwsCloudSettings;
import com.epam.datalab.dto.azure.AzureCloudSettings;
import com.epam.datalab.dto.base.CloudSettings;
import com.epam.datalab.dto.gcp.GcpCloudSettings;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class CloudSettingsDeserializer extends JsonDeserializer<CloudSettings> {
    @Override
    public CloudSettings deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode jsonNode = p.readValueAsTree();

        Map<String, String> mapJson = new HashMap<>();
        for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
            String s = it.next();
            mapJson.put(s, jsonNode.get(s).textValue());
        }

        try {
            return createFromMap(detectCloudSettings(mapJson), mapJson);
        } catch (IllegalAccessException e) {
            log.error("Cannot deserialize object due to {}", e.getMessage(), e);
            throw new IllegalArgumentException("Cannot deserialize cloud settings " + mapJson);
        }
    }

    private CloudSettings detectCloudSettings(Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey().startsWith("aws")) {
                return new AwsCloudSettings();
            } else if (entry.getKey().startsWith("azure")) {
                return new AzureCloudSettings();
            } else if (entry.getKey().startsWith("gcp")) {
                return new GcpCloudSettings();
            }
        }
        throw new IllegalArgumentException("Unknown properties " + properties);
    }

    private <T extends CloudSettings> T createFromMap(T settings, Map<String, String> map) throws
            IllegalAccessException {
        for (Field field : settings.getClass().getDeclaredFields()) {
            if (field.getAnnotation(JsonProperty.class) != null) {
                String value = map.get(field.getAnnotation(JsonProperty.class).value());
                if (value != null) {
                    field.setAccessible(true);
                    field.set(settings, value);

                }
            }
        }

        return settings;
    }

}