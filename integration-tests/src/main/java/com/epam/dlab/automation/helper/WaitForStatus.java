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

package com.epam.dlab.automation.helper;

import com.epam.dlab.automation.http.ContentType;
import com.epam.dlab.automation.http.HttpRequest;
import com.epam.dlab.automation.http.HttpStatusCode;
import com.jayway.restassured.path.json.JsonPath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        while ((actualStatus = request.webApiGet(NamingHelper.getSsnURL(), ContentType.TEXT).statusCode()) != HttpStatusCode.OK) {
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
        LOGGER.info("Waiting for status {} with URL {} with token {} for notebook {}", status, url, token, notebookName);
        HttpRequest request = new HttpRequest();
        String actualStatus;
        long timeout = duration.toMillis();
        long expiredTime = System.currentTimeMillis() + timeout;

        do{
            actualStatus = getNotebookStatus(request.webApiGet(url, token)
                    .getBody()
                    .jsonPath(), notebookName);
            if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
                break;
            }
            Thread.sleep(getSsnRequestTimeout());
        }
        while(status.contains(actualStatus));

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

    public static String cluster(String url, String token, String notebookName, String computationalName, String status, Duration duration)
            throws InterruptedException {
        LOGGER.info("{}: Waiting until status {} with URL {} with token {} for computational {} on notebook {}", notebookName, status, url, token, computationalName, notebookName);
        HttpRequest request = new HttpRequest();
        String actualStatus;
        long timeout = duration.toMillis();
        long expiredTime = System.currentTimeMillis() + timeout;

        do{
            actualStatus = getClusterStatus(request.webApiGet(url, token)
                    .getBody()
                    .jsonPath(), notebookName, computationalName);
            if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
                break;
            }
            Thread.sleep(getSsnRequestTimeout());
        }
        while(actualStatus.contains(status));

        if (actualStatus.contains(status)) {
            LOGGER.info("ERROR: Timeout has been expired for request.");
            LOGGER.info("  URL is {}", url);
            LOGGER.info("  token is {}", token);
            LOGGER.info("  status is {}", status);
            LOGGER.info("  timeout is {}", duration);
        } else {
            LOGGER.info("{}: Current state for cluster {} on notebook is {}", notebookName, computationalName, actualStatus);
        }

        return actualStatus;
    }

    public static String getClusterStatus(JsonPath json, String notebookName, String computationalName) {

		List<Map<String, List<Map<String, String>>>> notebooks = json.getList(EXPLORATORY_PATH);
		List<Map<String, String>> notebooksWithReducedData = json.getList(EXPLORATORY_PATH);

		List<Integer> indexesOfSeekingNotebooks = notebooksWithReducedData.stream()
				.map(e -> notebookName.equals(e.get("exploratory_name")) ? notebooksWithReducedData.indexOf(e) : -1)
				.filter(e -> e >= 0).collect(Collectors.toList());

		if (indexesOfSeekingNotebooks == null || indexesOfSeekingNotebooks.size() != 1) {
            return "";
        }
		List<Map<String, String>> resources = notebooks.get(indexesOfSeekingNotebooks.get(0))
				.get("computational_resources");
        for (Map<String, String> resource : resources) {
            String comp = resource.get("computational_name");
            if (comp != null && comp.equals(computationalName)) {
                return resource.get("status");
            }
        }
        return "";
    }

    private static String getNotebookStatus(JsonPath json, String notebookName) {
		List<Map<String, String>> notebooks = json.getList(EXPLORATORY_PATH);
		notebooks = notebooks.stream().filter(e -> notebookName.equals(e.get("exploratory_name")))
				.collect(Collectors.toList());

        if (notebooks == null || notebooks.size() != 1) {
            return "";
        }
        Map<String, String> notebook = notebooks.get(0);
        String status = notebook.get("status");
        return (status == null ? "" : status);
    }
}
