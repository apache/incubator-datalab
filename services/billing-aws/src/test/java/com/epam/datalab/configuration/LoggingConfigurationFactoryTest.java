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

import ch.qos.logback.classic.Level;
import com.epam.datalab.core.BillingUtils;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.logging.AppenderBase;
import com.epam.datalab.logging.AppenderConsole;
import com.epam.datalab.logging.AppenderFile;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class LoggingConfigurationFactoryTest {

    private ObjectMapper getMapper() throws InitializationException {
        ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.registerModule(new GuavaModule());
        for (Class<?> clazz : BillingUtils.getModuleClassList()) {
            mapper.registerSubtypes(clazz);
        }
        return mapper;
    }

    @Test
    public void config() throws IOException, InitializationException {
        // {"filter":[{"filterProperty":"filterValue"}],"parser":[{"parserProperty":"parserValue"}],"adapterIn":[{"adapterInProperty":"adapterInValue"}],"adapterOut":[{"adapterOutProperty":"adapterOutValue"}]}
        final String jsonString =
                "{\n" +
                        "  \"level\":\"INFO\",\n" +
                        "  \"loggers\":{\n" +
                        "    \"com.epam\":\"DEBUG\",\n" +
                        "    \"org.apache.http\":\"WARN\"},\n" +
                        "  \"appenders\":[\n" +
                        "    {\"type\":\"console\"},\n" +
                        "    {\"type\":\"file\",\n" +
                        "      \"currentLogFilename\":\"billing.log\",\n" +
                        "      \"archive\":true,\n" +
                        "      \"archivedLogFilenamePattern\":\"billing-%d{yyyy-MM-dd}.log.gz\",\n" +
                        "      \"archivedFileCount\":10}]\n" +
                        "}";

        ObjectMapper mapper = getMapper();
        JsonNode conf = mapper.readTree(jsonString); // validate JSON
        LoggingConfigurationFactory logger = mapper.readValue(conf.toString(), LoggingConfigurationFactory.class);

        assertEquals(Level.INFO, logger.getLevel());
        assertEquals(Level.DEBUG, logger.getLoggers().get("com.epam"));
        assertEquals(Level.WARN, logger.getLoggers().get("org.apache.http"));

        List<AppenderBase> appenders = logger.getAppenders();
        assertEquals("Invalid number of appenders", 2, appenders.size());

        AppenderConsole ac = (AppenderConsole) appenders.get(0);
        assertNotNull(ac);

        AppenderFile af = (AppenderFile) appenders.get(1);
        assertEquals("billing.log", af.getCurrentLogFilename());
        assertEquals(true, af.getArchive());
        assertEquals("billing-%d{yyyy-MM-dd}.log.gz", af.getArchivedLogFilenamePattern());
        assertEquals(10, af.getArchivedFileCount());
    }
}
