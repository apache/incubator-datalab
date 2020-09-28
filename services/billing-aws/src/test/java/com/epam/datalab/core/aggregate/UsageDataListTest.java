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

import com.epam.datalab.model.aws.ReportLine;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class UsageDataListTest {

    @Test
    public void day() {
        DataAggregator agg = new DataAggregator(AggregateGranularity.DAY);
        ReportLine r;

        r = new ReportLine();
        r.setUsageDate("2017-04-12 12:34:56");
        r.setCost(1.2);
        r.setUsage(10.1);
        agg.append(r);

        r = new ReportLine();
        r.setUsageDate("2017-04-12 12:34:78");
        r.setCost(3.4);
        r.setUsage(20.2);
        agg.append(r);

        r = new ReportLine();
        r.setUsageDate("2017-04-14 11:22:33");
        r.setCost(5.6);
        r.setUsage(40.4);
        agg.append(r);

        assertEquals(AggregateGranularity.DAY, agg.getGranularity());
        assertEquals(2, agg.size());

        assertEquals("2017-04-12", agg.get(0).getUsageDate());
        assertEquals(4.6, agg.get(0).getCost());
        assertEquals(30.3, agg.get(0).getUsage(), 0.000001);

        assertEquals("2017-04-14", agg.get(1).getUsageDate());
        assertEquals(5.6, agg.get(1).getCost());
        assertEquals(40.4, agg.get(1).getUsage());

        agg.clear();
        assertEquals(0, agg.size());
    }

    @Test
    public void month() {
        DataAggregator agg = new DataAggregator(AggregateGranularity.MONTH);
        ReportLine r;

        r = new ReportLine();
        r.setUsageDate("2017-04-12 12:34:56");
        r.setCost(1.2);
        r.setUsage(10.1);
        agg.append(r);

        r = new ReportLine();
        r.setUsageDate("2017-04-12 12:34:78");
        r.setCost(3.4);
        r.setUsage(20.2);
        agg.append(r);

        r = new ReportLine();
        r.setUsageDate("2017-05-14 11:22:33");
        r.setCost(5.6);
        r.setUsage(40.4);
        agg.append(r);

        assertEquals(AggregateGranularity.MONTH, agg.getGranularity());
        assertEquals(2, agg.size());

        assertEquals("2017-04", agg.get(0).getUsageDate());
        assertEquals(4.6, agg.get(0).getCost());
        assertEquals(30.3, agg.get(0).getUsage(), 0.000001);

        assertEquals("2017-05", agg.get(1).getUsageDate());
        assertEquals(5.6, agg.get(1).getCost());
        assertEquals(40.4, agg.get(1).getUsage());

        agg.clear();
        assertEquals(0, agg.size());
    }

    @Test
    public void none() {
        try {
            new DataAggregator(AggregateGranularity.NONE);
            fail("DataArggregator should not have been created");
        } catch (IllegalArgumentException e) {
            // OK
        }
    }

}
