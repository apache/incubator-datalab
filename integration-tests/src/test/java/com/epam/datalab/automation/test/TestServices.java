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

package com.epam.datalab.automation.test;

import com.epam.datalab.automation.cloud.VirtualMachineStatusChecker;
import com.epam.datalab.automation.docker.Docker;
import com.epam.datalab.automation.helper.CloudHelper;
import com.epam.datalab.automation.helper.CloudProvider;
import com.epam.datalab.automation.helper.ConfigPropertyValue;
import com.epam.datalab.automation.helper.NamingHelper;
import com.epam.datalab.automation.helper.PropertiesResolver;
import com.epam.datalab.automation.helper.WaitForStatus;
import com.epam.datalab.automation.http.ApiPath;
import com.epam.datalab.automation.http.ContentType;
import com.epam.datalab.automation.http.HttpRequest;
import com.epam.datalab.automation.http.HttpStatusCode;
import com.epam.datalab.automation.jenkins.JenkinsService;
import com.epam.datalab.automation.model.Lib;
import com.epam.datalab.automation.model.LoginDto;
import com.epam.datalab.automation.model.NotebookConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.fail;

@Test(singleThreaded = true)
public class TestServices {

	private final static Logger LOGGER = LogManager.getLogger(TestServices.class);
	// This time 3 notebooks are tested in parallel - so 3 threads are used,
	// restartNotebookAndRedeployToTerminate are a pool for future notebooks grow.
	// needed to investigate Amazon behaviour when same AIM requests set of
	// computation resources in parallel
	// looks like running test in 1 thread mostly succeeds, running in 2 and more
	// threads - usually fails.
	private static final int N_THREADS = 10;
	private static final long NOTEBOOK_CREATION_DELAY = 60000;

	private long testTimeMillis;
	private List<NotebookConfig> notebookConfigs;
	private List<Lib> skippedLibs;


	@BeforeClass
	public void Setup() throws IOException {
		testTimeMillis = System.currentTimeMillis();
		// Load properties
		ConfigPropertyValue.getJenkinsJobURL();

		ObjectMapper mapper = new ObjectMapper();
		notebookConfigs = mapper.readValue(ConfigPropertyValue.getNotebookTemplates(),
				new TypeReference<ArrayList<NotebookConfig>>() {
				});
		skippedLibs = mapper.readValue(ConfigPropertyValue.getSkippedLibs(),
				new TypeReference<ArrayList<Lib>>() {
				});
	}

	@AfterClass
	public void Cleanup() {
		testTimeMillis = System.currentTimeMillis() - testTimeMillis;
		LOGGER.info("Test time {} ms", testTimeMillis);
	}

	@Test
	public void runTest() throws Exception {
		testJenkinsJob();
		testLoginSsnService();

		RestAssured.baseURI = NamingHelper.getSsnURL();
		NamingHelper.setSsnToken(ssnLoginAndKeyUpload());
		runTestsInNotebooks();
	}

	private void testJenkinsJob() throws Exception {
		/*
		 * LOGGER.info("1. Jenkins Job will be started ...");
		 *
		 * JenkinsService jenkins = new
		 * JenkinsService(ConfigPropertyValue.getJenkinsUsername(),
		 * ConfigPropertyValue.getJenkinsPassword()); String buildNumber =
		 * jenkins.runJenkinsJob(ConfigPropertyValue.getJenkinsJobURL());
		 * LOGGER.info("   Jenkins Job has been completed");
		 */

		LOGGER.info("1. Looking for last Jenkins Job ...");
		JenkinsService jenkins = new JenkinsService();
		String buildNumber = jenkins.getJenkinsJob();
		LOGGER.info("   Jenkins Job found:");
		LOGGER.info("Build number is: {}", buildNumber);

		NamingHelper.setSsnURL(jenkins.getSsnURL().replaceAll(" ", ""));
		NamingHelper.setServiceBaseName(jenkins.getServiceBaseName().replaceAll(" ", ""));
		Assert.assertNotNull(NamingHelper.getSsnURL(), "Jenkins URL was not generated");
		Assert.assertNotNull(NamingHelper.getServiceBaseName(), "Service BaseName was not generated");
		LOGGER.info("Self-Service URL is: " + NamingHelper.getSsnURL());
		LOGGER.info("ServiceBaseName is: " + NamingHelper.getServiceBaseName());
	}

	private ResponseBody<?> login(String username, String password, int expectedStatusCode, String errorMessage) {
		final String ssnLoginURL = NamingHelper.getSelfServiceURL(ApiPath.LOGIN);
		LoginDto requestBody = new LoginDto(username, password);
		Response response = new HttpRequest().webApiPost(ssnLoginURL, ContentType.JSON, requestBody);
		LOGGER.info("   login response body for user {} is {}", username, response.getBody().asString());
		Assert.assertEquals(response.statusCode(), expectedStatusCode, errorMessage);
		return response.getBody();
	}

	private void testLoginSsnService() throws Exception {

		String cloudProvider = ConfigPropertyValue.getCloudProvider();

		LOGGER.info("Check status of SSN node on {}: {}", cloudProvider.toUpperCase(), NamingHelper.getSsnName());

		String publicSsnIp = CloudHelper.getInstancePublicIP(NamingHelper.getSsnName(), true);
		LOGGER.info("Public IP is: {}", publicSsnIp);
		String privateSsnIp = CloudHelper.getInstancePrivateIP(NamingHelper.getSsnName(), true);
		LOGGER.info("Private IP is: {}", privateSsnIp);
		if (publicSsnIp == null || privateSsnIp == null) {
			Assert.fail("There is not any virtual machine in " + cloudProvider + " with name " + NamingHelper.getSsnName());
			return;
		}
		NamingHelper.setSsnIp(PropertiesResolver.DEV_MODE ? publicSsnIp : privateSsnIp);
		VirtualMachineStatusChecker.checkIfRunning(NamingHelper.getSsnName(), true);
		LOGGER.info("{} instance state is running", cloudProvider.toUpperCase());

		LOGGER.info("2. Waiting for SSN service ...");
		Assert.assertTrue(WaitForStatus.selfService(ConfigPropertyValue.getTimeoutSSNStartup()), "SSN service was " +
				"not" +
				" " +
				"started");
		LOGGER.info("   SSN service is available");

		LOGGER.info("3. Check login");
		final String ssnLoginURL = NamingHelper.getSelfServiceURL(ApiPath.LOGIN);
		LOGGER.info("   SSN login URL is {}", ssnLoginURL);

		ResponseBody<?> responseBody;
		// TODO Choose username and password for this check
		// if (!ConfigPropertyValue.isRunModeLocal()) {
		// responseBody = login(ConfigPropertyValue.getNotIAMUsername(),
		// ConfigPropertyValue.getNotIAMPassword(),
		// HttpStatusCode.UNAUTHORIZED, "Unauthorized user " +
		// ConfigPropertyValue.getNotIAMUsername());
		// Assert.assertEquals(responseBody.asString(), "Please contact AWS
		// administrator to create corresponding IAM User");
		// }

		responseBody = login(ConfigPropertyValue.getNotDataLabUsername(), ConfigPropertyValue.getNotDataLabPassword(),
				HttpStatusCode.UNAUTHORIZED, "Unauthorized user " + ConfigPropertyValue.getNotDataLabUsername());

		Assert.assertEquals(responseBody.path("message"), "Username or password is invalid");

		if (!ConfigPropertyValue.isRunModeLocal()) {
			responseBody = login(ConfigPropertyValue.getUsername(), ".", HttpStatusCode.UNAUTHORIZED,
					"Unauthorized user " + ConfigPropertyValue.getNotDataLabUsername());
			Assert.assertEquals(responseBody.path("message"), "Username or password is invalid");
		}

		LOGGER.info("Logging in with credentials {}/***", ConfigPropertyValue.getUsername());
		responseBody = login(ConfigPropertyValue.getUsername(), ConfigPropertyValue.getPassword(), HttpStatusCode.OK,
				"User login " + ConfigPropertyValue.getUsername() + " was not successful");

		LOGGER.info("4. Check logout");
		final String ssnlogoutURL = NamingHelper.getSelfServiceURL(ApiPath.LOGOUT);
		LOGGER.info("   SSN logout URL is {}", ssnlogoutURL);

		Response responseLogout = new HttpRequest().webApiPost(ssnlogoutURL, ContentType.ANY);
		LOGGER.info("responseLogout.statusCode() is {}", responseLogout.statusCode());
		Assert.assertEquals(responseLogout.statusCode(), HttpStatusCode.UNAUTHORIZED,
				"User log out was not successful"/*
				 * Replace to HttpStatusCode.OK when EPMCBDCCSS-938 will be fixed
				 * and merged
				 */);
	}

	private String ssnLoginAndKeyUpload() throws Exception {
		LOGGER.info("5. Login as {} ...", ConfigPropertyValue.getUsername());
		final String ssnLoginURL = NamingHelper.getSelfServiceURL(ApiPath.LOGIN);
		final String ssnUploadKeyURL = NamingHelper.getSelfServiceURL(ApiPath.UPLOAD_KEY);
		LOGGER.info("   SSN login URL is {}", ssnLoginURL);
		LOGGER.info("   SSN upload key URL is {}", ssnUploadKeyURL);

		ResponseBody<?> responseBody = login(ConfigPropertyValue.getUsername(), ConfigPropertyValue.getPassword(),
				HttpStatusCode.OK, "Failed to login");
		String token = responseBody.asString();
		LOGGER.info("   Logged in. Obtained token: {}", token);

		LOGGER.info("5.a Checking for user Key...");
		Response respCheckKey = new HttpRequest().webApiGet(ssnUploadKeyURL, token);

		if (respCheckKey.getStatusCode() == HttpStatusCode.NOT_FOUND) {
			LOGGER.info("5.b Upload Key will be started ...");

			Response respUploadKey = new HttpRequest().webApiPost(ssnUploadKeyURL, ContentType.FORMDATA, token);
			LOGGER.info("   respUploadKey.getBody() is {}", respUploadKey.getBody().asString());

			Assert.assertEquals(respUploadKey.statusCode(), HttpStatusCode.OK, "The key uploading was not successful");
			int responseCodeAccessKey = WaitForStatus.uploadKey(ssnUploadKeyURL, token, HttpStatusCode.ACCEPTED,
					ConfigPropertyValue.getTimeoutUploadKey());
			LOGGER.info("   Upload Key has been completed");
			LOGGER.info("responseAccessKey.statusCode() is {}", responseCodeAccessKey);
			Assert.assertEquals(responseCodeAccessKey, HttpStatusCode.OK, "The key uploading was not successful");
		} else if (respCheckKey.getStatusCode() == HttpStatusCode.OK) {
			LOGGER.info("   Key has been uploaded already");
		} else {
			Assert.assertEquals(200, respCheckKey.getStatusCode(), "Failed to check User Key.");
		}

		final String nodePrefix = ConfigPropertyValue.getUsernameSimple();
		Docker.checkDockerStatus(nodePrefix + "_create_edge_", NamingHelper.getSsnIp());

		VirtualMachineStatusChecker.checkIfRunning(NamingHelper.getEdgeName(), true);

		final String ssnExpEnvURL = NamingHelper.getSelfServiceURL(ApiPath.EXP_ENVIRONMENT);
		LOGGER.info("   SSN exploratory environment URL is {}", ssnExpEnvURL);
		final String ssnProUserResURL = NamingHelper.getSelfServiceURL(ApiPath.PROVISIONED_RES);
		LOGGER.info("   SSN provisioned user resources URL is {}", ssnProUserResURL);

		return token;
	}

	private void populateNotebookConfigWithSkippedLibs(NotebookConfig notebookCfg) {
		if (Objects.isNull(notebookCfg.getSkippedLibraries())) {
			notebookCfg.setSkippedLibraries(skippedLibs);
		}
	}

	private void runTestsInNotebooks() throws Exception {

		ExecutorService executor = Executors.newFixedThreadPool(
				ConfigPropertyValue.getExecutionThreads() > 0 ? ConfigPropertyValue.getExecutionThreads() : N_THREADS);
		notebookConfigs.forEach(this::populateNotebookConfigWithSkippedLibs);
		List<FutureTask<Boolean>> futureTasks = new ArrayList<>();
		if (CloudProvider.GCP_PROVIDER.equals(ConfigPropertyValue.getCloudProvider())) {
			LOGGER.debug("Image creation tests are skipped for all types of notebooks in GCP.");
			notebookConfigs.forEach(config -> config.setImageTestRequired(false));
		}
		LOGGER.info("Testing the following notebook configs: {}", notebookConfigs);
		for (NotebookConfig notebookConfig : notebookConfigs) {
			if (!ConfigPropertyValue.isRunModeLocal() &&
					CloudProvider.AZURE_PROVIDER.equals(ConfigPropertyValue.getCloudProvider())) {
				LOGGER.debug("Waiting " + NOTEBOOK_CREATION_DELAY / 1000 + " sec to start notebook creation...");
				TimeUnit.SECONDS.sleep(NOTEBOOK_CREATION_DELAY / 1000);
			}
			FutureTask<Boolean> runScenarioTask = new FutureTask<>(new TestCallable(notebookConfig));
			futureTasks.add(runScenarioTask);
			executor.execute(runScenarioTask);
		}
		final long checkThreadTimeout = ConfigPropertyValue.isRunModeLocal() ? 1000 : 5000;
		while (true) {
			boolean done = allScenariosDone(futureTasks);
			if (done) {
				verifyResults(futureTasks);
				executor.shutdown();
				return;
			} else {
				TimeUnit.SECONDS.sleep(checkThreadTimeout / 1000);
			}
		}
	}

	private void verifyResults(List<FutureTask<Boolean>> futureTasks) {
		List<Exception> resExceptions = new ArrayList<>();
		for (FutureTask<Boolean> ft : futureTasks) {
			try {
				ft.get();
			} catch (Exception exception) {
				resExceptions.add(exception);
			}
		}

		if (resExceptions.size() > 0) {
			for (Exception exception : resExceptions) {
				LOGGER.error("{} :\n {} ", exception, exception.getStackTrace());
				exception.printStackTrace();
			}
			fail("There were failed tests with " + resExceptions.size() + " from " + futureTasks.size()
					+ " notebooks, see stacktrace above.");
		}
	}

	private boolean allScenariosDone(List<FutureTask<Boolean>> futureTasks) {
		boolean done = true;
		for (FutureTask<Boolean> ft : futureTasks) {
			if (!ft.isDone()) {
				done = ft.isDone();
			}
		}
		return done;
	}
}
