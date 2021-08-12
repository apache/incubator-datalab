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
import com.epam.datalab.exceptions.InitializationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;

import java.io.IOException;
import java.io.InputStream;

/**
 * Build the instance of class {@link BillingToolConfiguration}.
 */
public class BillingToolConfigurationFactory {

    /**
     * Mapper for reading configuration.
     */
    private static ObjectMapper mapper;

    /**
     * Build the instance of class {@link BillingToolConfiguration} from YAML file.
     *
     * @param filename  the name of file.
     * @param confClass configuration class.
     * @return the instance of configuration.
     * @throws InitializationException
     */
    public static <T extends BillingToolConfiguration> T build(String filename, Class<T> confClass) throws InitializationException {
        try {
            InputStream is = new FreeMarkerConfig().getInputStream(filename);
            JsonNode node = getMapper().readTree(new YAMLFactory().createParser(is));
            return build(node, confClass);
        } catch (IOException | InitializationException e) {
            throw new InitializationException("Cannot parse configuration file " + filename + ". " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Build the instance of class {@link BillingToolConfiguration} from YAML file.
     *
     * @param node      the content of configuration.
     * @param confClass configuration class.
     * @return the instance of configuration.
     * @throws InitializationException
     */
    public static <T extends BillingToolConfiguration> T build(JsonNode node, Class<T> confClass) throws InitializationException {
        T conf;
        try {
            conf = getMapper().readValue(node.toString(), confClass);
        } catch (Exception e) {
            throw new InitializationException("Cannot parse json configuration. " + e.getLocalizedMessage(), e);
        }

        try {
            LoggingConfigurationFactory logging = conf.getLogging();
            if (logging != null) {
                logging.configure();
            }
        } catch (Exception e) {
            throw new InitializationException("Cannot initialize configuration. " + e.getLocalizedMessage(), e);
        }

        new ConfigurationValidator<T>()
                .validate(conf);

        return conf;
    }

    /**
     * Return the mapper for reading configuration.
     *
     * @throws InitializationException
     */
    private static ObjectMapper getMapper() throws InitializationException {
        if (mapper != null) {
            return mapper;
        }
        mapper = new ObjectMapper()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.registerModule(new GuavaModule());
        for (Class<?> clazz : BillingUtils.getModuleClassList()) {
            mapper.registerSubtypes(clazz);
        }

        return mapper;
    }
}
