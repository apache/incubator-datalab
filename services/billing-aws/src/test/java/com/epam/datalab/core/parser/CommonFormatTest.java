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

import com.epam.datalab.core.BillingUtils;
import com.epam.datalab.exceptions.InitializationException;
import com.epam.datalab.exceptions.ParseException;
import com.epam.datalab.model.aws.BillingResourceType;
import com.epam.datalab.model.aws.ReportLine;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class CommonFormatTest {

    private CommonFormat getInstance() throws InitializationException {
        final List<String> columns = Lists.newArrayList("sCol1", "sCol2", "sCol3", "sCol4");
        final String mapping = ReportLine.FIELD_DATALAB_ID + "=sCol2;" +
                ReportLine.FIELD_USER_ID + "=$1;" + ReportLine.FIELD_TAGS + "=$4,sCol3";
        ColumnMeta meta = new ColumnMeta(mapping, columns);
        return new CommonFormat(meta, '.', ' ');
    }

    @Test
    public void toCommonFormat() throws InitializationException, ParseException {
        final CommonFormat format = getInstance();
        final List<String> values = Lists.newArrayList("value1", "value2", "value3", "value4");
        final ReportLine r = format.toCommonFormat(values);

        assertEquals("value2", r.getDatalabId());
        assertEquals("value1", r.getUser());
        assertEquals(2, r.getTags().size());
        assertEquals("value4", r.getTags().get("sCol4"));
        assertEquals("value3", r.getTags().get("sCol3"));

        assertEquals(123456.789, format.parseDouble("column1", "123 456.789"));
        assertEquals("12345.678", CommonFormat.doubleToString(12345.678));
    }

    @Test
    public void rowToString() throws ParseException {
        final List<String> values = Lists.newArrayList("val\"ue1", "\"val,;ue2\"", "value3", "value4");
        String line = CommonFormat.rowToString(values);
        assertEquals("\"val\\\"ue1\",\"\\\"val,;ue2\\\"\",\"value3\",\"value4\"", line);

        final ReportLine r = new ReportLine();
        r.setDatalabId("accountId");
        r.setUser("user");
        r.setUsageDate("2016-03-20");
        r.setProduct("Amazon Elastic Compute Cloud");
        r.setUsageType("usageType");
        r.setUsage(56.7);
        r.setCost(1234.56789);
        r.setCurrencyCode("USD");
        r.setResourceTypeId("i-1234567890abcdefg");
        r.setTags(Maps.newLinkedHashMap(BillingUtils.stringsToMap("tag1", "value1", "tag2", "value2")));
        line = CommonFormat.rowToString(r);
        assertEquals("\"accountId\",\"user\",\"2016-03-20\",\"Amazon Elastic Compute Cloud\"," +
                "\"usageType\",\"56.7\",\"1234.56789\",\"USD\"," +
                "\"COMPUTER\",\"i-1234567890abcdefg\"," +
                "\"value1\",\"value2\"", line);
    }

    @Test
    public void toReportLine() throws InitializationException, ParseException {
        final CommonFormat format = getInstance();
        final List<String> values = Lists.newArrayList(
                "accountId", "user", "2016-03-27",
                "Amazon Elastic Compute Cloud", "usageType", "56.7", "1234.56789", "USD",
                "i-1234567890abcdefg", "value1", "value2");
        final ReportLine r = format.toReportLine(values);

        assertEquals("accountId", r.getDatalabId());
        assertEquals("user", r.getUser());
        assertEquals("2016-03-27", r.getUsageDate());
        assertEquals("Amazon Elastic Compute Cloud", r.getProduct());
        assertEquals("usageType", r.getUsageType());
        assertEquals(56.7, r.getUsage());
        assertEquals(1234.56789, r.getCost());
        assertEquals("USD", r.getCurrencyCode());
        assertEquals(BillingResourceType.COMPUTER, r.getResourceType());
        assertEquals("i-1234567890abcdefg", r.getResourceId());
        assertEquals("value1", r.getTags().get("sCol4"));
        assertEquals("value2", r.getTags().get("sCol3"));
    }
}
