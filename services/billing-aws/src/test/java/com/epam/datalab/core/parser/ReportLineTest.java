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
import com.epam.datalab.exceptions.ParseException;
import com.epam.datalab.model.aws.BillingResourceType;
import com.epam.datalab.model.aws.ReportLine;
import com.google.common.collect.Maps;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class ReportLineTest {

    private void checkGetters(ReportLine r) {
        assertEquals("datalabId", r.getDatalabId());
        assertEquals("user", r.getUser());
        assertEquals("2016-01-22", r.getUsageDate());
        assertEquals("Amazon Elastic Compute Cloud", r.getProduct());
        assertEquals("usageType", r.getUsageType());
        assertEquals(56.7, r.getUsage());
        assertEquals(12.34, r.getCost());
        assertEquals("USD", r.getCurrencyCode());
        assertEquals("i-1234567890abcdefg", r.getResourceId());
        assertEquals("value1", r.getTags().get("tag1"));
        assertEquals("value2", r.getTags().get("tag2"));
    }

    @Test
    public void set() throws ParseException {
        ReportLine r = new ReportLine();

        r.setDatalabId("datalabId");
        r.setCost(12.34);
        r.setCurrencyCode("USD");
        r.setProduct("Amazon Elastic Compute Cloud");
        r.setResourceTypeId("i-1234567890abcdefg");
        r.setUsage(56.7);
        r.setUsageDate("2016-01-22");
        r.setUsageType("usageType");
        r.setUser("user");
        r.setTags(Maps.newLinkedHashMap(BillingUtils.stringsToMap("tag1", "value1", "tag2", "value2")));

        checkGetters(r);
    }

    private void checkResourceType(String product, String resourceTypeId,
                                   BillingResourceType expectedResourceType, String expectedResourceId) throws
            ParseException {
        ReportLine r = new ReportLine();
        r.setProduct(product);
        r.setResourceTypeId(resourceTypeId);

        assertEquals(expectedResourceType, r.getResourceType());
        assertEquals(expectedResourceId, r.getResourceId());
    }

    @Test
    public void resourceType() throws ParseException {
        checkResourceType("Amazon Elastic Compute Cloud", "i-000c0e51d117e3b4a", BillingResourceType.COMPUTER,
                "i-000c0e51d117e3b4a");
        checkResourceType("Amazon Elastic Compute Cloud", "vol-04c20f339836c56b6", BillingResourceType.STORAGE_EBS,
                "vol-04c20f339836c56b6");
        checkResourceType("Amazon Elastic Compute Cloud", "34.208.106.54", BillingResourceType.IP_ADDRESS,
                "34.208.106.54");

        checkResourceType("Amazon Elastic MapReduce",
                "arn:aws:elasticmapreduce:us-west-2:203753054073:cluster/j-1FOBGFRC8X4XY", BillingResourceType
                        .CLUSTER, "j-1FOBGFRC8X4XY");

        checkResourceType("Amazon Simple Storage Service", "datalab-s3", BillingResourceType.STORAGE_BUCKET, "datalab-s3");
        checkResourceType("AmazonCloudWatch",
                "arn:aws:logs:us-west-2:203753054073:log-group:CloudTrail/DefaultLogGroup", BillingResourceType.OTHER,
                "arn:aws:logs:us-west-2:203753054073:log-group:CloudTrail/DefaultLogGroup");
    }
}
