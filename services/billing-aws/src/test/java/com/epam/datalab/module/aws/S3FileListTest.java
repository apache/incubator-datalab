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

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.epam.datalab.core.ModuleData;
import com.epam.datalab.exceptions.AdapterException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class S3FileListTest {

    @Test
    public void sort() throws AdapterException {
        final AmazonS3Client s3Client = mock(AmazonS3Client.class);
        final ListObjectsV2Result result = mock(ListObjectsV2Result.class);
        final ModuleData moduleData = mock(ModuleData.class);
        final String[] array = {
                "report-prefix/report/20160101-20160131/123456789/report-1.csv",
                "report-prefix/report/20160101-20160131/123456789/report-2.csv",
                "report-prefix/report/20160101-20160131/123456789/report-3.csv",
                "report-prefix/report/20160202-20160231/123456789/report.csv",
                "report-prefix/report/20160202-20160301/123456789/report.csv",
                "report-prefix/report/20160303-20160301/123456789/report-1.csv",
                "report-prefix/report/20160303-20160301/123456789/report-2.csv",
                "report-prefix/report/20160303-20160302/123456789/report-1.csv",
                "report-prefix/report/20160303-20160302/123456789/report-2.csv"
        };
        final List<S3ObjectSummary> objectSummaries = Arrays.asList(
                getObjectSummary(array[0], LocalDate.of(2018, 4, 4)),
                getObjectSummary(array[4], LocalDate.of(2018, 4, 4)),
                getObjectSummary(array[1], LocalDate.of(2018, 4, 4)),
                getObjectSummary(array[2], LocalDate.of(2018, 4, 4)),
                getObjectSummary(array[3], LocalDate.of(2018, 4, 4)),
                getObjectSummary(array[5], LocalDate.of(2018, 4, 4)),
                getObjectSummary(array[6], LocalDate.of(2018, 4, 4)),
                getObjectSummary(array[7], LocalDate.of(2018, 4, 4)),
                getObjectSummary(array[8], LocalDate.of(2018, 4, 4))

        );
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(result);
        when(result.getObjectSummaries()).thenReturn(objectSummaries);
        when(moduleData.wasProcessed(any(), any(), anyString())).thenReturn(false);

        S3FileList s3list = new S3FileList(false, "test", moduleData);
        final List<String> list = s3list.getFiles(s3Client);

        assertEquals(array.length, list.size());
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], list.get(i));
        }
    }

    @Test
    public void testGettingLastFilesInBillingPeriod() throws Exception {
        final ListObjectsV2Result result = mock(ListObjectsV2Result.class);
        final ModuleData moduleData = mock(ModuleData.class);
        final List<S3ObjectSummary> objectSummaries = Arrays.asList(
                getObjectSummary("DATALAB-billing/reportName/20180101-20180201/guid1/test-1.csv.zip",
                        LocalDate.of(2018, 4, 1)),
                getObjectSummary("DATALAB-billing/reportName/20180101-20180201/guid1/test-2.csv.zip",
                        LocalDate.of(2018, 4, 1)),
                getObjectSummary("DATALAB-billing/reportName/20180101-20180201/guid0/test-1.csv.zip",
                        LocalDate.of(2018, 1, 1)),
                getObjectSummary("DATALAB-billing/reportName/20180201-20180301/guid0/test-1.csv.zip",
                        LocalDate.of(2018, 1, 1)),
                getObjectSummary("DATALAB-billing/reportName/20180202-20180301/guid0/test-2.csv.zip",
                        LocalDate.of(2018, 1, 1))

        );
        when(result.getObjectSummaries()).thenReturn(objectSummaries);
        final List<String> files = new S3FileList(true, null, moduleData).lastFilesPerBillingPeriod(result
                .getObjectSummaries());

        assertEquals(4, files.size());
        assertTrue(files.contains("DATALAB-billing/reportName/20180101-20180201/guid1/test-1.csv.zip"));
        assertTrue(files.contains("DATALAB-billing/reportName/20180101-20180201/guid1/test-2.csv.zip"));
        assertTrue(files.contains("DATALAB-billing/reportName/20180201-20180301/guid0/test-1.csv.zip"));
        assertTrue(files.contains("DATALAB-billing/reportName/20180202-20180301/guid0/test-2.csv.zip"));


    }

    private S3ObjectSummary getObjectSummary(String key, LocalDate modificationDate) {
        final S3ObjectSummary objectSummary = new S3ObjectSummary();
        objectSummary.setKey(key);
        objectSummary.setLastModified(Date.from(modificationDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return objectSummary;
    }
}
