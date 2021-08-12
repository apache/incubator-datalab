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

package com.epam.datalab.configuration;

import com.epam.datalab.core.BillingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Generate the json configuration of billing tool.
 */
public class ConfigJsonGenerator {

    /**
     * Buffer for configuration properties.
     */
    private Map<String, Map<String, String>[]> config = new HashMap<>();

    /**
     * Add the properties of module to configuration.
     *
     * @param moduleName the name of module.
     * @param properties the properties: key and value sequence.
     */
    private ConfigJsonGenerator withModule(String moduleName, String... properties) {
        if (properties == null) {
            config.remove(moduleName);
            return this;
        }

        @SuppressWarnings("unchecked")
        Map<String, String>[] map = new Map[1];
        map[0] = BillingUtils.stringsToMap(properties);
        config.put(moduleName, map);
        return this;
    }

    /**
     * Add the properties of input adapter to configuration.
     *
     * @param properties the properties: key and value sequence.
     */
    public ConfigJsonGenerator withAdapterIn(String... properties) {
        return withModule("adapterIn", properties);
    }

    /**
     * Add the properties of output adapter to configuration.
     *
     * @param properties the properties: key and value sequence.
     */
    public ConfigJsonGenerator withAdapterOut(String... properties) {
        return withModule("adapterOut", properties);
    }

    /**
     * Add the properties of parser to configuration.
     *
     * @param properties the properties: key and value sequence.
     */
    public ConfigJsonGenerator withParser(String... properties) {
        return withModule("parser", properties);
    }

    /**
     * Add the properties of filter to configuration.
     *
     * @param properties the properties: key and value sequence.
     */
    public ConfigJsonGenerator withFilter(String... properties) {
        return withModule("filter", properties);
    }

    /**
     * Build and return json configuration.
     *
     * @param properties the properties: key and value sequence.
     */
    public JsonNode build() {
        return new ObjectMapper()
                .valueToTree(config);
    }
}
