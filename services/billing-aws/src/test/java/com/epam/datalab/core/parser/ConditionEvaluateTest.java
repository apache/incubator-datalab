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

package com.epam.datalab.core.parser;

import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.exceptions.ParseException;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class ConditionEvaluateTest {
    private static final List<String> columnNames = Lists.newArrayList("column1", "column2", "column3");

    private void checkCondition(boolean excpectedResult, String condition, String... values
    ) throws InitializationException, ParseException {
        ConditionEvaluate c = new ConditionEvaluate(columnNames, condition);
        final List<String> row = Lists.newArrayList(values);
        assertEquals(excpectedResult, c.evaluate(row));
    }

    private void isTrue(String condition, String... values
    ) throws InitializationException, ParseException {
        checkCondition(true, condition, values);
    }

    private void isFalse(String condition, String... values
    ) throws InitializationException, ParseException {
        checkCondition(false, condition, values);
    }

    @Test
    public void mixCondition() throws InitializationException, ParseException {
        String condition = "column1 == 123 && column2 == '456' && column3 > 0.2";
        ConditionEvaluate c = new ConditionEvaluate(columnNames, condition);

        List<String> row = Lists.newArrayList("123", "456", "0.5");
        assertEquals(true, c.evaluate(row));

        row = Lists.newArrayList("123", "456", "-5");
        assertEquals(false, c.evaluate(row));

        isFalse("column1 == 123 || column2 == 321", "321", "123");
        isTrue("column1 == 123 || column2 == 321", "123", "456");
        isTrue("column1 == 123 || column2 == 321", "456", "321");

        isFalse("(column1 == 123 || column2 == 321) && column3 == 5", "321", "123", "5");
        isFalse("(column1 == 123 || column2 == 321) && column3 == 5", "123", "321", "4");

        isTrue("(column1 == 123 || column2 == 321) && column3 == 5", "123", "456", "5");
        isTrue("(column1 == 123 || column2 == 321) && column3 == 5", "234", "321", "5");
    }

    @Test
    public void compareInteger() throws InitializationException, ParseException {
        isFalse("column1 == 123", "456");
        isTrue("column1 == 123", "123");

        isFalse("column1 != 123", "123");
        isTrue("column1 != 123", "456");

        isFalse("column1 < 123", "123");
        isTrue("column1 < 123", "122");

        isFalse("column1 > 123", "123");
        isTrue("column1 > 123", "124");

        isFalse("column1 <= 123", "124");
        isTrue("column1 <= 123", "123");
        isTrue("column1 <= 123", "122");

        isFalse("column1 >= 123", "122");
        isTrue("column1 >= 123", "123");
        isTrue("column1 >= 123", "124");
    }

    @Test
    public void compareString() throws InitializationException, ParseException {
        isFalse("column1 == 'abc'", "abcd");
        isTrue("column1 == 'abc'", "abc");

        isFalse("column1 != 'abc'", "abc");
        isTrue("column1 != 'abc'", "asd");

        isFalse("column1 < 'abc'", "abd");
        isTrue("column1 < 'abc'", "abb");

        isFalse("column1 > 'abc'", "abb");
        isTrue("column1 > 'abc'", "abd");

        isFalse("column1 <= 'abc'", "abd");
        isTrue("column1 <= 'abc'", "abc");
        isTrue("column1 <= 'abc'", "abb");

        isFalse("column1 >= 'abc'", "abb");
        isTrue("column1 >= 'abc'", "abc");
        isTrue("column1 >= 'abc'", "abd");
    }
}
