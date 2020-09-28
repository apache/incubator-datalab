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

package com.epam.datalab.core;

import com.epam.datalab.core.parser.ParserBase;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.logging.AppenderBase;
import com.epam.datalab.logging.AppenderConsole;
import com.epam.datalab.logging.AppenderFile;
import com.epam.datalab.module.AdapterConsole;
import com.epam.datalab.module.AdapterFile;
import com.epam.datalab.module.ParserCsv;
import com.epam.datalab.module.aws.AdapterS3File;
import com.epam.datalab.module.aws.FilterAWS;
import com.epam.datalab.mongo.AdapterMongoDb;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class BillingUtilsTest {

    @Test
    public void stringsToMap() {
        Map<String, String> map = BillingUtils.stringsToMap(
                "key1", "value1",
                "key2", "value2");
        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));

        try {
            map = BillingUtils.stringsToMap(
                    "key1", "value1",
                    "key2");
            fail("Missed value2 is passed");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

    private void checkModule(List<Class<?>> list, Class<?> moduleClass) {
        for (Class<?> module : list) {
            if (module == moduleClass) {
                return;
            }
        }
        fail("Module " + moduleClass.getName() + " is not in module list");
    }

    @Test
    public void getModuleClassList() throws InitializationException {
        List<Class<?>> list = BillingUtils.getModuleClassList();
        checkModule(list, AdapterConsole.class);
        checkModule(list, AdapterFile.class);
        checkModule(list, AdapterS3File.class);
        checkModule(list, AdapterMongoDb.class);
        checkModule(list, FilterAWS.class);
        checkModule(list, ParserCsv.class);
        checkModule(list, AppenderConsole.class);
        checkModule(list, AppenderFile.class);
    }

    @Test
    public void classChildOf() {
        assertEquals(true, BillingUtils.classChildOf(AdapterConsole.class, AdapterBase.class));
        assertEquals(false, BillingUtils.classChildOf(AdapterConsole.class, FilterBase.class));
        assertEquals(true, BillingUtils.classChildOf(FilterAWS.class, FilterBase.class));
        assertEquals(false, BillingUtils.classChildOf(FilterAWS.class, ParserBase.class));
        assertEquals(true, BillingUtils.classChildOf(ParserCsv.class, ParserBase.class));
        assertEquals(false, BillingUtils.classChildOf(ParserCsv.class, AppenderBase.class));
        assertEquals(true, BillingUtils.classChildOf(AppenderConsole.class, AppenderBase.class));
        assertEquals(false, BillingUtils.classChildOf(AppenderConsole.class, AdapterBase.class));
    }

    @Test
    public void getModuleType() {
        assertEquals(ModuleType.ADAPTER, BillingUtils.getModuleType(AdapterConsole.class));
        assertEquals(ModuleType.ADAPTER, BillingUtils.getModuleType(AdapterFile.class));
        assertEquals(ModuleType.ADAPTER, BillingUtils.getModuleType(AdapterS3File.class));
        assertEquals(ModuleType.ADAPTER, BillingUtils.getModuleType(AdapterMongoDb.class));
        assertEquals(ModuleType.FILTER, BillingUtils.getModuleType(FilterAWS.class));
        assertEquals(ModuleType.PARSER, BillingUtils.getModuleType(ParserCsv.class));
        assertEquals(ModuleType.LOGAPPENDER, BillingUtils.getModuleType(AppenderConsole.class));
        assertEquals(ModuleType.LOGAPPENDER, BillingUtils.getModuleType(AppenderFile.class));
    }
}
