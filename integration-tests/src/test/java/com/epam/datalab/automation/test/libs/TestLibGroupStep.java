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
import com.epam.datalab.automation.http.HttpRequest;
import com.epam.datalab.automation.http.HttpStatusCode;
import com.epam.datalab.automation.model.JsonMapperDto;
import com.jayway.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@TestDescription("Test \"Show available library groups\" ")
public class TestLibGroupStep extends TestLibStep {
    private static final Logger LOGGER = LogManager.getLogger(TestLibGroupStep.class);
    private List<String> expectedGroups;

    public TestLibGroupStep(String url, String token, String notebookName, long initTimeout, String jsonFilePath) {
        super(NamingHelper.getSelfServiceURL(url), token, notebookName, initTimeout);
        this.expectedGroups = JsonMapperDto.readListOf(jsonFilePath, String.class);
    }

    @Override
    public void init() throws InterruptedException {

        long currentTime = System.currentTimeMillis() / 1000L;
        long expiredTime = currentTime + initTimeoutSec;

        while (expiredTime > currentTime) {
            HttpRequest httpRequest = new HttpRequest();
            
            Map<String, Object> params = new HashMap<>();
            params.put("exploratory_name", notebookName);
			Response groups= httpRequest.webApiGet(url, token,params );
            if (groups.getStatusCode() != HttpStatusCode.OK) {
                LOGGER.error("Response status {}, body {}", groups.getStatusCode(), groups.getBody().print());
                Assert.fail("Cannot get lib groups " + notebookName);
            } else {
                List<String> availableGroups = groups.getBody().jsonPath().getList("", String.class);

                if (availableGroups == null || availableGroups.isEmpty()) {
                    LOGGER.info("Init lib group. Wait for time out {} seconds left for {}", expiredTime - currentTime, notebookName);
                    TimeUnit.SECONDS.sleep(ConfigPropertyValue.isRunModeLocal() ? 3L : 20L);
                } else {
                    return;
                }
            }

            currentTime = System.currentTimeMillis() / 1000L;
        }

        Assert.fail("Timeout Cannot get lib groups " + notebookName);
    }

    @Override
    public void verify() {
        HttpRequest httpRequest = new HttpRequest();
        
        Map<String, Object> params = new HashMap<>();
        params.put("exploratory_name", notebookName);
		Response response= httpRequest.webApiGet(url, token,params );
        if (response.getStatusCode() == HttpStatusCode.OK) {
            List<String> availableGroups = response.getBody().jsonPath().getList("", String.class);

            LOGGER.info("Expected groups {}", expectedGroups);

            LOGGER.info("Available groups {}", availableGroups);

            for (String lib : expectedGroups) {
                Assert.assertTrue(availableGroups.contains(lib), String.format("%s lib groups is not available for %s", lib, notebookName));
            }

        } else {
            LOGGER.error("Response status {}, body {}", response.getStatusCode(), response.getBody().print());
            Assert.fail("Lib group request failed for " + notebookName);
        }

        LOGGER.info(getDescription() + "passed");
    }
}
