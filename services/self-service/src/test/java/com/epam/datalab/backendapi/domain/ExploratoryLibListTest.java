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
package com.epam.datalab.backendapi.domain;

import com.epam.datalab.backendapi.resources.dto.LibraryAutoCompleteDTO;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class ExploratoryLibListTest {

	@Test
	public void getLibs() {
		String content =
				"{" +
						"\"os_pkg\": {\"htop\": \"2.0.1-1ubuntu1\", \"python-mysqldb\": \"1.3.7-1build2\"}," +
						"\"pip2\": {\"requests\": \"N/A\", \"configparser\": \"N/A\"}," +
						"\"pip3\": {\"configparser\": \"N/A\"}," +
						"\"r_pkg\": {\"rmarkdown\": \"1.5\"}" +
						"}";

		ExploratoryLibList libs = new ExploratoryLibList("imageName", content);

		assertEquals("imageName", libs.getGroup());
		assertFalse(libs.isExpired());
		assertFalse(libs.isUpdateNeeded());
		assertFalse(libs.isUpdating());

		List<String> groups = libs.getGroupList();
		assertEquals(4, groups.size());
		assertEquals("os_pkg", groups.get(0));
		assertEquals("r_pkg", groups.get(3));

		Map<String, String> map = libs.getLibs("os_pkg");
		assertEquals(2, map.size());
		assertEquals("2.0.1-1ubuntu1", map.get("htop"));
		assertEquals("1.3.7-1build2", map.get("python-mysqldb"));

		final LibraryAutoCompleteDTO dtoList = libs.getLibs("os_pkg", "py");
		assertEquals(1, dtoList.getLibraries().size());
		assertEquals("1.3.7-1build2", dtoList.getLibraries().get(0).getVersion());

		libs.setUpdating();
		assertTrue(libs.isUpdating());
	}
}
