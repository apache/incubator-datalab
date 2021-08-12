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

package com.epam.datalab.core.aggregate;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class DataAggregatorTest {

    @Test
    public void append() {
        UsageDataList list = new UsageDataList();

        list.append("2017-04-12");
        list.append("2017-04-12");
        list.append("2017-04-14");

        assertEquals(2, list.size());

        assertEquals(Boolean.FALSE, list.get("2017-04-12"));
        assertEquals(Boolean.FALSE, list.get("2017-04-14"));

        list.set("2017-04-14", true);
        assertEquals(Boolean.TRUE, list.get("2017-04-14"));

        list.clear();
        assertEquals(0, list.size());
    }
}
