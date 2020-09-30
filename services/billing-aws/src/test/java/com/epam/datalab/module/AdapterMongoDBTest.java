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

package com.epam.datalab.module;

import com.epam.datalab.core.AdapterBase.Mode;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.mongo.AdapterMongoDb;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class AdapterMongoDBTest {

    @Test
    public void config() throws InitializationException {
        AdapterMongoDb adapter = new AdapterMongoDb();
        adapter.setMode(Mode.WRITE);
        adapter.setWriteHeader(false);
        adapter.setHost("host");
        adapter.setPort(123);
        adapter.setDatabase("database");
        adapter.setUsername("username");
        adapter.setPassword("password");
        adapter.setBufferSize(321);
        adapter.setUpsert(true);

        assertEquals(ModuleName.ADAPTER_MONGO_DATALAB, adapter.getType());
        assertEquals(Mode.WRITE, adapter.getMode());
        assertEquals(false, adapter.isWriteHeader());
        assertEquals("host", adapter.getHost());
        assertEquals(123, adapter.getPort());
        assertEquals("database", adapter.getDatabase());
        assertEquals("username", adapter.getUsername());
        assertEquals("password", adapter.getPassword());
        assertEquals(321, adapter.getBufferSize());
        assertEquals(true, adapter.isUpsert());
    }
}
