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

import static junit.framework.TestCase.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class S3FileListTest {

	@Test
	public void sort() {
		final String [] array = {
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
		s3list.sort(list);

		assertEquals(array.length, list.size());
		for (int i = 0; i < array.length; i++) {
			assertEquals(array[i], list.get(i));
		}
	}
}
