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
import com.epam.datalab.automation.test.libs.models.LibInstallRequest;
import com.epam.datalab.automation.test.libs.models.LibStatusResponse;
import com.epam.datalab.automation.test.libs.models.LibraryStatus;
import com.jayway.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@TestDescription("Test \"Install libraries\" ")
public class TestLibInstallStep extends TestLibStep {
    private final static Logger LOGGER = LogManager.getLogger(TestLibInstallStep.class);
    private String statusUrl;
    private Lib libToInstall;
	private boolean isInstalled = true;

	public TestLibInstallStep(String requestUrl, String statusUrl, String token, String notebookName, long
			initTimeoutSec,
							  Lib libToInstall) {

        super(NamingHelper.getSelfServiceURL(requestUrl), token, notebookName, initTimeoutSec);
        this.statusUrl = NamingHelper.getSelfServiceURL(statusUrl);
        this.libToInstall = libToInstall;
    }

    @Override
    public void init() throws InterruptedException {
        LibInstallRequest request = new LibInstallRequest(Collections.singletonList(libToInstall), notebookName);

        LOGGER.info("Install lib {}", request);

        long currentTime = System.currentTimeMillis() / 1000L;
        long expiredTime = currentTime + initTimeoutSec;

        Response response = new HttpRequest().webApiPost(url, ContentType.JSON, request, token);
        if (response.getStatusCode() != HttpStatusCode.OK) {
            LOGGER.error("Response status {}, body {}", response.getStatusCode(), response.getBody().print());
            Assert.fail("Cannot install libs for " + request);
        }

        while (expiredTime > currentTime) {

            HttpRequest httpRequest = new HttpRequest();
            Map<String,Object> params = new HashMap<>();
            params.put("exploratory_name", notebookName);
            response = httpRequest.webApiGet(statusUrl, token,params);
            if (response.getStatusCode() == HttpStatusCode.OK) {

                List<LibStatusResponse> actualStatuses = Arrays.asList(response.getBody().as(LibStatusResponse[].class));

                LOGGER.info("Actual statuses {}", actualStatuses);

                LibStatusResponse s = actualStatuses.stream()
                        .filter(e -> e.getGroup().equals(libToInstall.getGroup())
                                && e.getName().equals(libToInstall.getName())
                                && (e.getVersion().equals(libToInstall.getVersion()) || "N/A".equals(libToInstall.getVersion())))
						.findFirst().orElseThrow(() -> new LibraryNotFoundException(String.format("Library " +
										"template with parameters: group=%s, name=%s, version=%s not found.",
								libToInstall.getGroup(), libToInstall.getName(), libToInstall.getVersion())));

                LOGGER.info("Lib status is {}", s);
                
                boolean allLibStatusesDone = true;
                
                for (LibraryStatus libStatus : s.getStatus()) {
                	if (libStatus.getStatus().equals("installing")) {
                		allLibStatusesDone = false;
                    } 
				}
                if(!allLibStatusesDone) {
                	LOGGER.info("Wait {} sec left for installation libs {}", expiredTime - currentTime, request);
                    TimeUnit.SECONDS.sleep(ConfigPropertyValue.isRunModeLocal() ? 3L : 20L);
                } else {
                    return;
                }
                
            } else {
                LOGGER.error("Response status{}, body {}", response.getStatusCode(), response.getBody().print());
                Assert.fail("Install libs failed for " + notebookName);
            }

            currentTime = System.currentTimeMillis() / 1000L;
        }

        Assert.fail("Timeout Cannot install libs on " + notebookName + " " + request);
    }

    @Override
    public void verify() {
        HttpRequest httpRequest = new HttpRequest();
        Map<String,Object> params = new HashMap<>();
        params.put("exploratory_name", notebookName);
        Response response = httpRequest.webApiGet(statusUrl, token,params);
        if (response.getStatusCode() == HttpStatusCode.OK) {

            List<LibStatusResponse> actualStatuses = Arrays.asList(response.getBody().as(LibStatusResponse[].class));
            LOGGER.info("Actual statuses {}", actualStatuses);

            LibStatusResponse libStatusResponse = actualStatuses.stream()
                    .filter(e -> e.getGroup().equals(libToInstall.getGroup())
                            && e.getName().equals(libToInstall.getName())
                            && (e.getVersion().equals(libToInstall.getVersion()) || "N/A".equals(libToInstall.getVersion())))
					.findFirst().orElseThrow(() -> new LibraryNotFoundException(String.format("Library " +
									"template with parameters: group=%s, name=%s, version=%s not found.",
							libToInstall.getGroup(), libToInstall.getName(), libToInstall.getVersion())));

            for (LibraryStatus libStatus : libStatusResponse.getStatus()) {
            	if ("installed".equals(libStatus.getStatus())) {
                    LOGGER.info("Library status of {} is {}", libToInstall, libStatusResponse);
                } else if ("failed".equals(libStatus.getStatus())) {
                    LOGGER.warn("Failed status with proper error message happend for {}", libStatusResponse);
					isInstalled = false;
                } else {
					Assert.assertEquals("installed", libStatus.getStatus(), "Lib " + libToInstall + " is not " +
							"installed" +
							". Status " + libStatusResponse);
                }
			}
        } else {
            LOGGER.error("Response status{}, body {}", response.getStatusCode(), response.getBody().print());
            Assert.fail("Install libs failed for " + notebookName);
        }
        LOGGER.info(getDescription() + "passed");
    }

	public boolean isLibraryInstalled() {
		return isInstalled;
	}
}
