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

import com.epam.datalab.core.aggregate.AggregateGranularity;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.exceptions.ParseException;
import com.epam.datalab.module.aws.FilterAWS;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class ParserCsvTest {

    @Test
    public void config() throws InitializationException {
        ParserCsv parser = new ParserCsv();

        parser.setFieldSeparator('a');
        parser.setFieldTerminator('b');
        parser.setEscapeChar('c');
        parser.setDecimalSeparator('d');
        parser.setGroupingSeparator('e');
        parser.setAggregate(AggregateGranularity.DAY.toString());
        parser.setColumnMapping("ColumnMapping");
        parser.setHeaderLineNo(123);
        parser.setSkipLines(321);

        assertEquals(ModuleName.PARSER_CSV, parser.getType());
        assertEquals('a', parser.getFieldSeparator());
        assertEquals('b', parser.getFieldTerminator());
        assertEquals('c', parser.getEscapeChar());
        assertEquals('d', parser.getDecimalSeparator());
        assertEquals('e', parser.getGroupingSeparator());
        assertEquals(AggregateGranularity.DAY, parser.getAggregate());
        assertEquals("ColumnMapping", parser.getColumnMapping());
        assertEquals(123, parser.getHeaderLineNo());
        assertEquals(321, parser.getSkipLines());

        AdapterConsole adapterIn = new AdapterConsole();
        AdapterConsole adapterOut = new AdapterConsole();
        FilterAWS filter = new FilterAWS();
        parser.build(adapterIn, adapterOut, filter);

        assertEquals(adapterIn, parser.getAdapterIn());
        assertEquals(adapterOut, parser.getAdapterOut());
        assertEquals(filter, parser.getFilter());

        parser.initialize();
    }

    @Test
    public void parseRow() throws ParseException {
        ParserCsv parser = new ParserCsv();
        final List<String> row = Lists.newArrayList("qwe", "rty", "\"uio\"", "asd\"fgh\"jkl");
        final String line = "\"qwe\",\"rty\",\"\\\"uio\\\"\",\"asd\\\"fgh\\\"jkl\"";
        List<String> rowParsed = parser.parseRow(line);
        assertEquals(MoreObjects.toStringHelper(this).add("row", row).toString(), MoreObjects.toStringHelper(this).add("row", rowParsed).toString());
    }
}
