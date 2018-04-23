/***************************************************************************

 Copyright (c) 2016, EPAM SYSTEMS INC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 ****************************************************************************/

package com.epam.dlab.module.aws;

import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.epam.dlab.core.ModuleData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class S3FileListTest {

	@Test
	public void sort() {
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
		List<String> list = new ArrayList<>();
		list.add(array[8]);
		list.add(array[2]);
		list.add(array[5]);
		list.add(array[4]);
		list.add(array[7]);
		list.add(array[3]);
		list.add(array[6]);
		list.add(array[0]);
		list.add(array[1]);

		S3FileList s3list = new S3FileList(null, null);
		list.sort(String::compareTo);

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
				getObjectSummary("DLAB-billing/reportName/20180101-20180201/guid1/test-1.csv.zip",
						LocalDate.of(2018, 4, 1)),
				getObjectSummary("DLAB-billing/reportName/20180101-20180201/guid1/test-2.csv.zip",
						LocalDate.of(2018, 4, 1)),
				getObjectSummary("DLAB-billing/reportName/20180101-20180201/guid0/test-1.csv.zip",
						LocalDate.of(2018, 1, 1)),
				getObjectSummary("DLAB-billing/reportName/20180201-20180301/guid0/test-1.csv.zip",
						LocalDate.of(2018, 1, 1)),
				getObjectSummary("DLAB-billing/reportName/20180202-20180301/guid0/test-2.csv.zip",
						LocalDate.of(2018, 1, 1))

		);
		when(result.getObjectSummaries()).thenReturn(objectSummaries);
		final List<String> files = new S3FileList(null, moduleData).lastFilesPerBillingPeriod(result
				.getObjectSummaries());

		assertEquals(4, files.size());
		assertTrue(files.contains("DLAB-billing/reportName/20180101-20180201/guid1/test-1.csv.zip"));
		assertTrue(files.contains("DLAB-billing/reportName/20180101-20180201/guid1/test-2.csv.zip"));
		assertTrue(files.contains("DLAB-billing/reportName/20180201-20180301/guid0/test-1.csv.zip"));
		assertTrue(files.contains("DLAB-billing/reportName/20180202-20180301/guid0/test-2.csv.zip"));


	}

	private S3ObjectSummary getObjectSummary(String key, LocalDate modificationDate) {
		final S3ObjectSummary objectSummary = new S3ObjectSummary();
		objectSummary.setKey(key);
		objectSummary.setLastModified(Date.from(modificationDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
		return objectSummary;
	}
}
