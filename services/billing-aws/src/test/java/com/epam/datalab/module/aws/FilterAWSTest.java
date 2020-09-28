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

package com.epam.datalab.module.aws;

import com.epam.datalab.exceptions.ParseException;
import com.epam.datalab.model.aws.ReportLine;
import com.epam.datalab.module.ModuleName;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class FilterAWSTest {

    @Test
    public void config() {
        FilterAWS filter = new FilterAWS();
        filter.setCurrencyCode("currency");

        assertEquals(ModuleName.FILTER_AWS, filter.getType());
        assertEquals("currency", filter.getCurrencyCode());
    }

    @Test
    public void canAccept() throws ParseException {
        FilterAWS filter = new FilterAWS();
        filter.setCurrencyCode("currency");

        ReportLine row = new ReportLine();
        row = filter.canAccept(row);

        assertEquals(filter.getCurrencyCode(), row.getCurrencyCode());
    }
}
