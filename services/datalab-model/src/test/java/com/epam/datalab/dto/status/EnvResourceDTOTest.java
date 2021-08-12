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

package com.epam.datalab.dto.status;

import com.epam.datalab.dto.UserEnvironmentResources;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EnvResourceDTOTest {

    private static String getJsonString(Object object) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper.writeValueAsString(object);
    }

    private static <T> T getJsonObject(String string, Class<T> objectType) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(string, objectType);
    }

    @Test
    public void serde() throws IOException {
        List<EnvResource> hosts1 = new ArrayList<EnvResource>();
        hosts1.add(new EnvResource().withId("1"));
        hosts1.add(new EnvResource().withId("2"));
        hosts1.add(new EnvResource().withId("3").withStatus("state3"));
        assertEquals(hosts1.get(0).getId(), "1");
        assertEquals(hosts1.get(2).getStatus(), "state3");

        List<EnvResource> clusters1 = new ArrayList<EnvResource>();
        clusters1.add(new EnvResource().withId("10"));
        clusters1.add(new EnvResource().withId("11"));
        assertEquals(clusters1.get(0).getId(), "10");

        EnvResourceList r1 = EnvResourceList.builder()
                .hostList(hosts1)
                .clusterList(clusters1)
                .build();
        assertEquals(r1.getHostList().get(1).getId(), "2");
        assertEquals(r1.getHostList().get(2).getId(), "3");
        assertEquals(r1.getClusterList().get(1).getId(), "11");

        UserEnvironmentResources rs1 = new UserEnvironmentResources()
                .withResourceList(r1);
        assertEquals(rs1.getResourceList().getHostList().get(0).getId(), "1");
        assertEquals(rs1.getResourceList().getClusterList().get(0).getId(), "10");

        String json1 = getJsonString(rs1);

        UserEnvironmentResources rs2 = getJsonObject(json1, UserEnvironmentResources.class);
        String json2 = getJsonString(rs2);
        assertEquals(rs1.getResourceList().getHostList().size(), rs2.getResourceList().getHostList().size());
        assertEquals(rs1.getResourceList().getClusterList().size(), rs2.getResourceList().getClusterList().size());

        assertEquals("Json SerDe error", json1, json2);
    }
}
