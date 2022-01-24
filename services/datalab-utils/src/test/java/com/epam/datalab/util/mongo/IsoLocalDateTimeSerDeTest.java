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

package com.epam.datalab.util.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;

public class IsoLocalDateTimeSerDeTest {

    private static ObjectMapper objectMapper;

    @BeforeClass
    public static void setup() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void shouldProperlySerializeLocalDateTimeToJson() throws JsonProcessingException {
        String actual = objectMapper.writeValueAsString(new SampleClass());
        assertEquals("{\"localDateTime\":{\"$date\":\"2018-04-10T15:30:45\"}}", actual);
    }

    @Test
    public void shouldProperlyDeserializeLocalDateTimeFromJson() throws IOException {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        long l = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        SampleClass actual = objectMapper
                .readValue("{\"localDateTime\":{\"$date\":" + l + "}}", SampleClass.class);

        assertEquals(now, actual.getLocalDateTime());
    }

    private static class SampleClass {

        @JsonSerialize(using = IsoLocalDateTimeSerializer.class)
        @JsonDeserialize(using = IsoLocalDateTimeDeSerializer.class)
        private final LocalDateTime localDateTime = LocalDateTime.parse("2018-04-10T15:30:45");

        LocalDateTime getLocalDateTime() {
            return localDateTime;
        }
    }
}
