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

package com.epam.datalab.automation.test.libs;

import com.epam.datalab.automation.helper.ConfigPropertyValue;
import com.epam.datalab.automation.helper.NamingHelper;
import com.epam.datalab.automation.http.ContentType;
import com.epam.datalab.automation.http.HttpRequest;
import com.epam.datalab.automation.http.HttpStatusCode;
import com.epam.datalab.automation.model.Lib;
import com.epam.datalab.automation.test.libs.models.LibSearchRequest;
import com.epam.datalab.automation.test.libs.models.LibToSearchData;
import com.jayway.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@TestDescription("Test \"Search libraries by group and prefix\" ")
public class TestLibListStep extends TestLibStep {
	private static final Logger LOGGER = LogManager.getLogger(TestLibListStep.class);
	private LibToSearchData libToSearchData;
	private List<Lib> libs = new ArrayList<>();

	public TestLibListStep(String url, String token, String notebookName, long initTimeoutSec,
						   LibToSearchData libToSearchData) {
		super(NamingHelper.getSelfServiceURL(url), token, notebookName, initTimeoutSec);
		this.libToSearchData = libToSearchData;
	}

	@Override
	public void init() throws InterruptedException {
		LibSearchRequest request = new LibSearchRequest(notebookName, libToSearchData.getGroup(),
				libToSearchData.getStartWith());

		long currentTime = System.currentTimeMillis() / 1000L;
		long expiredTime = currentTime + initTimeoutSec;

		while (expiredTime > currentTime) {
			Response response = new HttpRequest().webApiPost(url, ContentType.JSON, request, token);
			LOGGER.info("Request libraries {}", request);

			if (response.getStatusCode() != HttpStatusCode.OK) {
				LOGGER.error("Response status {}, body {}", response.getStatusCode(), response.getBody().print());
				Assert.fail("Cannot get lib list for " + request);
			} else {
				Map<String, String> foundLibs =
						getLibMap(response);
				if (foundLibs == null || foundLibs.isEmpty()) {
					LOGGER.info("Init lib list. Wait for time out {} seconds left for {}", expiredTime - currentTime,
							notebookName);
					TimeUnit.SECONDS.sleep(ConfigPropertyValue.isRunModeLocal() ? 3L : 20L);
				} else {
					return;
				}
			}

			currentTime = System.currentTimeMillis() / 1000L;
		}

		Assert.fail("Timeout Cannot get lib list " + notebookName);
	}

	private Map<String, String> getLibMap(Response response) {
		return response.getBody().jsonPath().getList("")
				.stream()
				.collect(Collectors.toMap(o -> (String) ((Map) o).get("name"),
						o -> (String) ((Map) o).get("version")));
	}

	@Override
	public void verify() {
		Map<String, String> actualFoundLibs = new HashMap<>();

		LibSearchRequest request = new LibSearchRequest(notebookName, libToSearchData.getGroup(),
				libToSearchData.getStartWith());
		Response response = new HttpRequest().webApiPost(url, ContentType.JSON, request, token);
		LOGGER.info("Request libraries {}", request);
		if (response.getStatusCode() == HttpStatusCode.OK) {
			actualFoundLibs = getLibMap(response);
			if (actualFoundLibs == null || actualFoundLibs.isEmpty()) {
				Assert.fail("Libraries not found");
			} else {
				LOGGER.info("Found libraries for {} are {}", request, actualFoundLibs);
				for (Map.Entry<String, String> entry : actualFoundLibs.entrySet()) {
					Assert.assertTrue(entry.getKey().toLowerCase().startsWith(libToSearchData.getStartWith().toLowerCase()),
							String.format("Nor expected lib is found %s-%s", entry.getKey(), entry.getValue()));
				}
				LOGGER.info("Libraries are verified");
			}

		} else {
			LOGGER.error("Response {}", response);
			Assert.fail("Lib list request failed for " + request);
		}
		LOGGER.info(getDescription() + "passed");

		for (Map.Entry<String, String> entry : actualFoundLibs.entrySet()) {
			libs.add(new Lib(libToSearchData.getGroup(), entry.getKey(), entry.getValue()));
		}
	}

	public List<Lib> getLibs() {
		return libs;
	}
}
