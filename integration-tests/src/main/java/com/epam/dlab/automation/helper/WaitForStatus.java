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

package com.epam.datalab.automation.helper;

import com.epam.datalab.automation.http.ContentType;
import com.epam.datalab.automation.http.HttpRequest;
import com.epam.datalab.automation.http.HttpStatusCode;
import com.jayway.restassured.path.json.JsonPath;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class WaitForStatus {

	private static final Logger LOGGER = LogManager.getLogger(WaitForStatus.class);
	private static final String EXPLORATORY_PATH = "exploratory";

	private static long getSsnRequestTimeout() {
		return ConfigPropertyValue.isRunModeLocal() ? 1000 : 10000;
	}

	private WaitForStatus() {
	}

	public static boolean selfService(Duration duration) throws InterruptedException {
		HttpRequest request = new HttpRequest();
		int actualStatus;
		long timeout = duration.toMillis();
		long expiredTime = System.currentTimeMillis() + timeout;

		while ((actualStatus = request.webApiGet(NamingHelper.getSsnURL(), ContentType.TEXT).statusCode()) !=
				HttpStatusCode.OK) {
			if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
				break;
			}
			Thread.sleep(getSsnRequestTimeout());
		}

		if (actualStatus != HttpStatusCode.OK) {
			LOGGER.info("ERROR: Timeout has been expired for SSN available. Timeout was {}", duration);
			return false;
		} else {
			LOGGER.info("Current status code for SSN is {}", actualStatus);
		}

		return true;
	}

	public static int uploadKey(String url, String token, int status, Duration duration)
			throws InterruptedException {
		LOGGER.info(" Waiting until status code {} with URL {} with token {}", status, url, token);
		HttpRequest request = new HttpRequest();
		int actualStatus;
		long timeout = duration.toMillis();
		long expiredTime = System.currentTimeMillis() + timeout;

		while ((actualStatus = request.webApiGet(url, token).getStatusCode()) == status) {
			if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
				break;
			}
			Thread.sleep(getSsnRequestTimeout());
		}

		if (actualStatus == status) {
			LOGGER.info("ERROR: {}: Timeout has been expired for request.");
			LOGGER.info("  URL is {}", url);
			LOGGER.info("  token is {}", token);
			LOGGER.info("  status is {}", status);
			LOGGER.info("  timeout is {}", duration);
		} else {
			LOGGER.info(" Current status code for {} is {}", url, actualStatus);
		}

		return actualStatus;
	}

	public static String notebook(String url, String token, String notebookName, String status, Duration duration)
			throws InterruptedException {
		LOGGER.info("Waiting for status {} with URL {} with token {} for notebook {}", status, url, token,
				notebookName);
		HttpRequest request = new HttpRequest();
		String actualStatus;
		long timeout = duration.toMillis();
		long expiredTime = System.currentTimeMillis() + timeout;

		do {
			actualStatus = getNotebookStatus(request.webApiGet(url, token)
					.getBody()
					.jsonPath(), notebookName);
			if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
				break;
			}
			Thread.sleep(getSsnRequestTimeout());
		}
		while (status.contains(actualStatus));

		if (status.contains(actualStatus)) {
			LOGGER.info("ERROR: {}: Timeout has been expired for request.", notebookName);
			LOGGER.info("  {}: URL is {}", notebookName, url);
			LOGGER.info("  {}: token is {}", notebookName, token);
			LOGGER.info("  {}: status is {}", notebookName, status);
			LOGGER.info("  {}: timeout is {}", notebookName, duration);
		} else {
			LOGGER.info("{}: Current state for Notebook {} is {}", notebookName, notebookName, actualStatus);
		}

		return actualStatus;
	}

	public static String cluster(String url, String token, String notebookName, String computationalName, String
			status, Duration duration)
			throws InterruptedException {
		LOGGER.info("{}: Waiting until status {} with URL {} with token {} for computational {} on notebook {}",
				notebookName, status, url, token, computationalName, notebookName);
		HttpRequest request = new HttpRequest();
		String actualStatus;
		long timeout = duration.toMillis();
		long expiredTime = System.currentTimeMillis() + timeout;

		do {
			actualStatus = getClusterStatus(request.webApiGet(url, token)
					.getBody()
					.jsonPath(), notebookName, computationalName);
			if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
				break;
			}
			Thread.sleep(getSsnRequestTimeout());
		}
		while (actualStatus.contains(status));

		if (actualStatus.contains(status)) {
			LOGGER.info("ERROR: Timeout has been expired for request.");
			LOGGER.info("  URL is {}", url);
			LOGGER.info("  token is {}", token);
			LOGGER.info("  status is {}", status);
			LOGGER.info("  timeout is {}", duration);
		} else {
			LOGGER.info("{}: Current state for cluster {} on notebook is {}", notebookName, computationalName,
					actualStatus);
		}

		return actualStatus;
	}

	@SuppressWarnings("unchecked")
	public static String getClusterStatus(JsonPath json, String notebookName, String computationalName) {
		return (String) json.getList(EXPLORATORY_PATH)
				.stream()
				.filter(exploratoryNamePredicate(notebookName))
				.flatMap(computationalResourcesStream())
				.filter(computationalNamePredicate(computationalName))
				.map(statusFieldPredicate())
				.findAny()
				.orElse(StringUtils.EMPTY);
	}

	private static String getNotebookStatus(JsonPath json, String notebookName) {
		List<Map<String, String>> notebooks = json.getList(EXPLORATORY_PATH);
		return notebooks.stream().filter(exploratoryNamePredicate(notebookName))
				.map(e -> e.get("status"))
				.findAny()
				.orElse(StringUtils.EMPTY);
	}

	private static Function<Object, Object> statusFieldPredicate() {
		return cr -> (((HashMap) cr).get("status"));
	}

	private static Predicate<Object> computationalNamePredicate(String computationalName) {
		return cr -> computationalName.equals(((HashMap) cr).get("computational_name"));
	}

	private static Function<Object, Stream<?>> computationalResourcesStream() {
		return d -> ((List) ((HashMap) d).get("computational_resources")).stream();
	}

	private static Predicate<Object> exploratoryNamePredicate(String notebookName) {
		return d -> notebookName.equals(((HashMap) d).get("exploratory_name"));
	}
}
