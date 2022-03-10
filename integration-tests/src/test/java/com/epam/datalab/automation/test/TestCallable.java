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
import com.epam.datalab.automation.cloud.aws.AmazonHelper;
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
import com.epam.datalab.automation.model.CreateNotebookDto;
import com.epam.datalab.automation.model.DeployClusterDto;
import com.epam.datalab.automation.model.DeploySparkDto;
import com.epam.datalab.automation.model.ExploratoryImageDto;
import com.epam.datalab.automation.model.ImageDto;
import com.epam.datalab.automation.model.JsonMapperDto;
import com.epam.datalab.automation.model.Lib;
import com.epam.datalab.automation.model.NotebookConfig;
import com.epam.datalab.automation.test.libs.LibsHelper;
import com.epam.datalab.automation.test.libs.TestLibGroupStep;
import com.epam.datalab.automation.test.libs.TestLibInstallStep;
import com.epam.datalab.automation.test.libs.TestLibListStep;
import com.epam.datalab.automation.test.libs.models.LibToSearchData;
import com.jayway.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.testng.Assert.fail;

public class TestCallable implements Callable<Boolean> {
    private final static Logger LOGGER = LogManager.getLogger(TestCallable.class);

    private final String notebookTemplate;
    private final boolean fullTest;
	private final String token, ssnExpEnvURL, ssnProUserResURL, ssnCompResURL;
    private final String storageName;
    private final String notebookName, clusterName, dataEngineType;
    private final NotebookConfig notebookConfig;
	private final List<Lib> skippedLibraries;
	private final boolean imageTestRequired;
	private int libsFailedToInstall = 0;

	TestCallable(NotebookConfig notebookConfig) {
    	this.notebookTemplate = notebookConfig.getNotebookTemplate();
    	this.dataEngineType = notebookConfig.getDataEngineType();
        this.fullTest = notebookConfig.isFullTest();

		this.notebookConfig = notebookConfig;
		this.skippedLibraries = notebookConfig.getSkippedLibraries();
		this.imageTestRequired = notebookConfig.isImageTestRequired();
        
        this.token = NamingHelper.getSsnToken();
        this.ssnExpEnvURL = NamingHelper.getSelfServiceURL(ApiPath.EXP_ENVIRONMENT);
        this.ssnProUserResURL = NamingHelper.getSelfServiceURL(ApiPath.PROVISIONED_RES);
        this.storageName = NamingHelper.getStorageName();

        final String suffixName = NamingHelper.generateRandomValue(notebookTemplate);
        notebookName = "nb" + suffixName;

		if (NamingHelper.DATA_ENGINE.equals(dataEngineType)) {
        	this.ssnCompResURL=NamingHelper.getSelfServiceURL(ApiPath.COMPUTATIONAL_RES_SPARK);
			clusterName = "spark" + suffixName;
		} else if (NamingHelper.DATA_ENGINE_SERVICE.equals(dataEngineType)) {
        	this.ssnCompResURL=NamingHelper.getSelfServiceURL(ApiPath.COMPUTATIONAL_RES);
			clusterName = "des" + suffixName;
        } else {
			ssnCompResURL = "";
			clusterName = NamingHelper.CLUSTER_ABSENT;
			LOGGER.info("illegal argument dataEngineType {} , should be dataengine or dataengine-service",
					dataEngineType);
        }

        LOGGER.info("   SSN exploratory environment URL is {}", ssnExpEnvURL);
        LOGGER.info("   SSN provisioned user resources URL is {}", ssnProUserResURL);
    }

    private static Duration getDuration(String duration) {
    	return Duration.parse("PT" + duration);
    }

	@Override
    public Boolean call() throws Exception {
		try {
			final String notebookIp = createNotebook(notebookName, "");
			testLibs();

			if (imageTestRequired) {
				executeImageTest();
			}

			final DeployClusterDto deployClusterDto = createClusterDto();
			final String actualClusterName = deployClusterDto != null ? NamingHelper.getClusterName(
					NamingHelper.getClusterInstanceNameForTestDES(notebookName, clusterName, dataEngineType),
					dataEngineType, true) : NamingHelper.CLUSTER_ABSENT;

			LOGGER.info("Actual cluster name of {} is {}", dataEngineType, actualClusterName);

			if (NamingHelper.DATA_ENGINE.equals(dataEngineType)) {
				LOGGER.debug("Spark cluster {} is stopping...", clusterName);
				stopCluster();
				LOGGER.debug("Starting Spark cluster {}...", clusterName);
				startCluster();
			}

			if (!ConfigPropertyValue.isRunModeLocal()) {

				TestDataEngineService test = new TestDataEngineService();
				test.run(notebookName, notebookTemplate, actualClusterName);

				String notebookScenarioFilesLocation = PropertiesResolver.getPropertyByName(
						String.format(PropertiesResolver.NOTEBOOK_SCENARIO_FILES_LOCATION_PROPERTY_TEMPLATE,
								notebookTemplate));
				String notebookTemplatesLocation = PropertiesResolver.getPropertyByName(
						String.format(PropertiesResolver.NOTEBOOK_TEST_TEMPLATES_LOCATION, notebookTemplate));
				test.run2(NamingHelper.getSsnIp(), notebookIp, actualClusterName,
						new File(notebookScenarioFilesLocation),
						new File(notebookTemplatesLocation), notebookName);
			}

			if (NamingHelper.DATA_ENGINE_SERVICE.equals(dataEngineType) && fullTest && deployClusterDto != null) {
				stopEnvironment();
				restartNotebookAndRedeployToTerminate(deployClusterDto);
			}
			if (deployClusterDto != null) {
				terminateNotebook(deployClusterDto);
			} else {
				terminateNotebook(notebookName);
			}

			LOGGER.info("{} All tests finished successfully", notebookName);
			return true;
		} catch (AssertionError | Exception e) {
			LOGGER.error("Error occurred while testing notebook {} with configuration {}", notebookName,
					notebookConfig, e);
			throw e;
		}
	}

	private void executeImageTest() throws Exception {
		LOGGER.debug("Tests with machine image are starting...");
		try {
			String imageName = "TestIm" +
					String.valueOf(new Random().ints(0, 1000).findFirst().orElse(0));
			LOGGER.info("Machine image with name {} from notebook {} is creating...", imageName, notebookName);
			createMachineImageFromNotebook(notebookName, imageName);
			LOGGER.info("Machine image with name {} was successfully created.", imageName);

			String copyNotebookName = "cp" + notebookName;
			LOGGER.info("Notebook {} from machine image {} is creating...", copyNotebookName, imageName);
			createNotebook(copyNotebookName, imageName);
			LOGGER.info("Notebook {} from machine image {} was successfully created.", copyNotebookName, imageName);

			LOGGER.info("Comparing notebooks: {} with {}...", notebookName, copyNotebookName);
			if (areNotebooksEqual(notebookName, copyNotebookName)) {
				LOGGER.info("Notebooks with names {} and {} are equal", notebookName, copyNotebookName);
			} else {
				Assert.fail("Notebooks aren't equal. Created from machine image notebook is different from base " +
						"exploratory");
			}

			LOGGER.debug("Notebook {} created from image {} is terminating...", copyNotebookName, imageName);
			terminateNotebook(copyNotebookName);

			LOGGER.info("Tests with machine image creation finished successfully");
		} catch (AssertionError | Exception e) {
			LOGGER.error("Error occurred while testing notebook {} and machine image {}", notebookName, e);
			throw e;
		}
	}

	private DeployClusterDto createClusterDto() throws Exception {
	if (ConfigPropertyValue.getCloudProvider().equalsIgnoreCase(CloudProvider.AZURE_PROVIDER)
			&& NamingHelper.DATA_ENGINE_SERVICE.equals(dataEngineType)) {
        LOGGER.info("There are no available dataengine services for Azure. Cluster creation is skipped.");
        return null;
    }
	if (!NamingHelper.DATA_ENGINE_SERVICE.equals(dataEngineType) && !NamingHelper.DATA_ENGINE.equals(dataEngineType)) {
		LOGGER.info("Parameter 'dataEngineType' is unspecified or isn't valid. Cluster creation is skipped.");
		return null;
	}
	String gettingStatus;
    LOGGER.info("7. {} cluster {} will be deployed for {} ...",dataEngineType, clusterName, notebookName);
    LOGGER.info("  {} : SSN computational resources URL is {}", notebookName, ssnCompResURL);

    DeployClusterDto clusterDto = null;
	if (NamingHelper.DATA_ENGINE.equals(dataEngineType)) {
		clusterDto = JsonMapperDto.readNode(
					Paths.get(String.format("%s/%s", CloudHelper.getClusterConfFileLocation(), notebookTemplate), "spark_cluster.json").toString(),
					DeploySparkDto.class);
	} else if (NamingHelper.DATA_ENGINE_SERVICE.equals(dataEngineType)) {
		clusterDto = JsonMapperDto.readNode(
				Paths.get(String.format("%s/%s", CloudHelper.getClusterConfFileLocation(), notebookTemplate),
						CloudHelper.getDockerTemplateFileForDES(notebookConfig.isDesSpotRequired())).toString(),
				CloudHelper.getDeployClusterClass());
    } else {
		LOGGER.error("illegal argument dataEngineType {} , should be dataengine or dataengine-service", dataEngineType);
		fail("illegal argument dataEngineType " + dataEngineType + ", should be dataengine or dataengine-service");
	}

    clusterDto.setName(clusterName);
		clusterDto.setNotebookName(notebookName);
		clusterDto = CloudHelper.populateDeployClusterDto(clusterDto, notebookConfig);
		LOGGER.info("{}: {} cluster = {}", notebookName, dataEngineType, clusterDto);
    Response responseDeployingCluster = new HttpRequest().webApiPut(ssnCompResURL, ContentType.JSON,
    		clusterDto, token);
	LOGGER.info("{}:   responseDeployingCluster.getBody() is {}", notebookName,
			responseDeployingCluster.getBody().asString());
	Assert.assertEquals(responseDeployingCluster.statusCode(), HttpStatusCode.OK, dataEngineType +
			" cluster " + clusterName + " was not deployed");

	gettingStatus = WaitForStatus.cluster(ssnProUserResURL, token, notebookName, clusterName, "creating",
			getDuration(notebookConfig.getTimeoutClusterCreate()));
    if(!ConfigPropertyValue.isRunModeLocal()) {
        if (!(gettingStatus.contains("configuring") || gettingStatus.contains("running")))
			throw new Exception(notebookName + ": " + dataEngineType + " cluster " + clusterName +
					" has not been deployed. Cluster status is " + gettingStatus);
        LOGGER.info("{}: {} cluster {} has been deployed", notebookName, dataEngineType, clusterName);

		VirtualMachineStatusChecker.checkIfRunning(
				NamingHelper.getClusterInstanceName(notebookName, clusterName, dataEngineType), false);

		Docker.checkDockerStatus(
				NamingHelper.getClusterContainerName(notebookName, clusterName, "create"), NamingHelper.getSsnIp());
    }
    LOGGER.info("{}:   Waiting until {} cluster {} has been configured ...", notebookName,dataEngineType,clusterName);

	gettingStatus = WaitForStatus.cluster(ssnProUserResURL, token, notebookName, clusterName, "configuring",
			getDuration(notebookConfig.getTimeoutClusterCreate()));
    if (!gettingStatus.contains("running"))
		throw new Exception(notebookName + ": " + dataEngineType + " cluster " + clusterName +
				" has not been configured. Spark cluster status is " + gettingStatus);
    LOGGER.info(" {}: {} cluster {} has been configured", notebookName, dataEngineType , clusterName);

    if(!ConfigPropertyValue.isRunModeLocal()) {
		VirtualMachineStatusChecker.checkIfRunning(
				NamingHelper.getClusterInstanceName(notebookName, clusterName, dataEngineType), false);
		Docker.checkDockerStatus(
				NamingHelper.getClusterContainerName(notebookName, clusterName, "create"), NamingHelper.getSsnIp());
    }
    if(ConfigPropertyValue.getCloudProvider().equalsIgnoreCase(CloudProvider.AWS_PROVIDER)){
        LOGGER.info("{}:   Check bucket {}", notebookName, storageName);
        AmazonHelper.printBucketGrants(storageName);
    }

    return clusterDto;
	}

	private String createNotebook(String notebookName, String imageName) throws Exception {
		LOGGER.info("6. Notebook {} will be created ...", notebookName);
		String notebookConfigurationFile =
				String.format(PropertiesResolver.NOTEBOOK_CONFIGURATION_FILE_TEMPLATE, notebookTemplate, notebookTemplate);
		LOGGER.info("{} notebook configuration file: {}", notebookName, notebookConfigurationFile);

		CreateNotebookDto createNoteBookRequest =
				JsonMapperDto.readNode(
						Paths.get(Objects.requireNonNull(CloudHelper.getClusterConfFileLocation()),
								notebookConfigurationFile).toString(), CreateNotebookDto.class);

		createNoteBookRequest.setName(notebookName);
		if (!StringUtils.isEmpty(notebookConfig.getNotebookShape())) {
			createNoteBookRequest.setShape(notebookConfig.getNotebookShape());
		}

		if (StringUtils.isNotBlank(imageName)) {
			final String ssnImageDataUrl =
					String.format(NamingHelper.getSelfServiceURL(ApiPath.IMAGE_CREATION + "/%s"), imageName);
			LOGGER.info("Image data fetching URL: {}", ssnImageDataUrl);

			Response response = new HttpRequest().webApiGet(ssnImageDataUrl, token);
			Assert.assertEquals(response.statusCode(), HttpStatusCode.OK, "Cannot get data of machine image with name "
					+ imageName);
			ImageDto dto = response.as(ImageDto.class);
			LOGGER.info("Image dto is: {}", dto);
			createNoteBookRequest.setImageName(dto.getFullName());
		}

		LOGGER.info("Inside createNotebook(): createNotebookRequest: image is {}, templateName is {}, shape is {}, " +
						"version is {}", createNoteBookRequest.getImage(), createNoteBookRequest.getTemplateName(),
				createNoteBookRequest.getShape(), createNoteBookRequest.getVersion());

		Response responseCreateNotebook = new HttpRequest().webApiPut(ssnExpEnvURL, ContentType.JSON,
				createNoteBookRequest, token);

		LOGGER.info(" {}:  responseCreateNotebook.getBody() is {}", notebookName,
				responseCreateNotebook.getBody().asString());

		LOGGER.info("Inside createNotebook(): responseCreateNotebook.statusCode() is {}",
				responseCreateNotebook.statusCode());

		Assert.assertEquals(responseCreateNotebook.statusCode(), HttpStatusCode.OK,
				"Notebook " + notebookName + " was not created");

		String gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, notebookName, "creating",
				getDuration(notebookConfig.getTimeoutNotebookCreate()));
		if (!gettingStatus.contains("running")) {
			LOGGER.error("Notebook {} is in state {}", notebookName, gettingStatus);
			throw new Exception("Notebook " + notebookName + " has not been created. Notebook status is " + gettingStatus);
		}
		LOGGER.info("   Notebook {} has been created", notebookName);

		VirtualMachineStatusChecker.checkIfRunning(NamingHelper.getNotebookInstanceName(notebookName), false);

		Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "create"),
				NamingHelper.getSsnIp());

		LOGGER.info("   Notebook {} status has been verified", notebookName);
		//get notebook IP
		String notebookIp =
				CloudHelper.getInstancePrivateIP(NamingHelper.getNotebookInstanceName(notebookName), false);

		LOGGER.info("   Notebook {} IP is {}", notebookName, notebookIp);

		return notebookIp;
	}

	private void createMachineImageFromNotebook(String notebookName, String imageName) throws InterruptedException {
		final String ssnImageCreationURL = NamingHelper.getSelfServiceURL(ApiPath.IMAGE_CREATION);
		ExploratoryImageDto requestBody =
				new ExploratoryImageDto(notebookName, imageName, "Machine image for testing");

		final String ssnImageDataUrl = ssnImageCreationURL + "/" + imageName;
		LOGGER.info("Machine image data fetching URL: {}", ssnImageDataUrl);

		long currentTime = System.currentTimeMillis() / 1000L;
		long expiredTime = currentTime + getDuration(notebookConfig.getTimeoutImageCreate()).getSeconds();

		Response imageCreationResponse =
				new HttpRequest().webApiPost(ssnImageCreationURL, ContentType.JSON, requestBody, token);
		if (imageCreationResponse.getStatusCode() != HttpStatusCode.ACCEPTED) {
			LOGGER.error("Machine image creation response status {}, body {}", imageCreationResponse.getStatusCode(),
					imageCreationResponse.getBody().print());
			Assert.fail("Cannot create machine image for " + requestBody);
		}

		while (expiredTime > currentTime) {

			imageCreationResponse = new HttpRequest().webApiGet(ssnImageDataUrl, token);
			if (imageCreationResponse.getStatusCode() == HttpStatusCode.OK) {

				LOGGER.info("Image creation response body for notebook {} is {}", notebookName,
						imageCreationResponse.getBody().asString());

				String actualImageStatus = imageCreationResponse.as(ImageDto.class).getStatus();

				LOGGER.info("Current machine image status is: {}", actualImageStatus);

				if (!"created".equalsIgnoreCase(actualImageStatus)) {
					LOGGER.info("Wait {} sec left for machine image status {}", expiredTime - currentTime,
							requestBody);
					TimeUnit.SECONDS.sleep(ConfigPropertyValue.isRunModeLocal() ? 3L : 20L);
				} else {
					break;
				}

			} else {
				LOGGER.error("Response status{}, body {}", imageCreationResponse.getStatusCode(),
						imageCreationResponse.getBody().print());
				Assert.fail("Machine image creation failed for " + notebookName);
			}
			currentTime = System.currentTimeMillis() / 1000L;
		}

		if (expiredTime <= currentTime) {
			Assert.fail("Due to timeout cannot create machine image on " + notebookName + " " + requestBody);
		}
	}

	private boolean areNotebooksEqual(String firstNotebookName, String secondNotebookName) {
		if (firstNotebookName == null || secondNotebookName == null) {
			Assert.fail("Wrong exploratory names passed");
			return false;
		}
		Response fetchExploratoriesResponse = new HttpRequest().webApiGet(ssnProUserResURL, token);
		if (fetchExploratoriesResponse.statusCode() != HttpStatusCode.OK) {
			LOGGER.error("Response status: {}, body: {}", fetchExploratoriesResponse.getStatusCode(),
					fetchExploratoriesResponse.getBody().print());
			Assert.fail("Fetching resource list is failed");
			return false;
		}
		List<Map<String, String>> notebooksTotal = fetchExploratoriesResponse.jsonPath().getList("exploratory");
		List<Map<String, String>> notebooksFilterred = notebooksTotal.stream()
				.filter(map -> map.get("exploratory_name").equals(firstNotebookName) ||
						map.get("exploratory_name").equals(secondNotebookName))
				.collect(Collectors.toList());

		if (notebooksFilterred.isEmpty()) {
			Assert.fail("Notebooks with names " + firstNotebookName + ", " + secondNotebookName + " don't exist");
			return false;
		}
		if (notebooksFilterred.size() == 1) {
			Assert.fail("Only one notebook with name " + notebooksFilterred.get(0).get("exploratory_name") +
					" found. There is nothing for comparison");
			return false;
		}
		if (notebooksFilterred.size() > 2) {
			Assert.fail("Error occured: found " + notebooksFilterred.size() + " notebooks, but only 2 expected");
			return false;
		}

		return areNotebooksEqualByFields(notebooksFilterred.get(0), notebooksFilterred.get(1)) &&
				areLibListsEqual(getNotebookLibList(firstNotebookName), getNotebookLibList(secondNotebookName));

	}

	private boolean areNotebooksEqualByFields(Map<String, String> firstNotebook, Map<String, String> secondNotebook) {
		if (!firstNotebook.get("shape").equals(secondNotebook.get("shape"))) {
			Assert.fail("Notebooks aren't equal: they have different shapes");
			return false;
		}
		if (!firstNotebook.get("image").equals(secondNotebook.get("image"))) {
			Assert.fail("Notebooks aren't equal: they are created from different Docker images");
			return false;
		}
		if (!firstNotebook.get("template_name").equals(secondNotebook.get("template_name"))) {
			Assert.fail("Notebooks aren't equal: they are created from different templates");
			return false;
		}
		if (!firstNotebook.get("version").equals(secondNotebook.get("version"))) {
			Assert.fail("Notebooks aren't equal: they have different versions");
			return false;
		}
		return true;
	}

	private List<Lib> getNotebookLibList(String notebookName) {
		Map<String, String> params = new HashMap<>();
		params.put("exploratory_name", notebookName);
		Response libListResponse = new HttpRequest()
				.webApiGet(NamingHelper.getSelfServiceURL(ApiPath.LIB_LIST_EXPLORATORY_FORMATTED), token, params);
		List<Lib> libs = null;
		if (libListResponse.getStatusCode() == HttpStatusCode.OK) {
			libs = Arrays.asList(libListResponse.getBody().as(Lib[].class));
		} else {
			LOGGER.error("Response status {}, body {}", libListResponse.getStatusCode(), libListResponse.getBody()
					.print());
			Assert.fail("Cannot get lib list for " + libListResponse);
			return libs;
		}
		return libs.stream().filter(Objects::nonNull).collect(Collectors.toList());
	}

	private boolean areLibListsEqual(List<Lib> firstLibList, List<Lib> secondLibList) {
		if (firstLibList == null && secondLibList == null) {
			return true;
		}
		if (firstLibList == null || secondLibList == null || firstLibList.size() != secondLibList.size()) {
			return false;
		}
		for (Lib lib : firstLibList) {
			String libGroup = lib.getGroup();
			String libName = lib.getName();
			String libVersion = lib.getVersion();
			List<Lib> filterred = secondLibList.stream().filter(l ->
					l.getGroup().equals(libGroup) && l.getName().equals(libName) && l.getVersion().equals(libVersion))
					.collect(Collectors.toList());
			if (filterred.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private void testLibs() throws Exception {
		LOGGER.info("{}: install libraries  ...", notebookName);

		TestLibGroupStep testLibGroupStep = new TestLibGroupStep(ApiPath.LIB_GROUPS, token, notebookName,
				getDuration(notebookConfig.getTimeoutLibGroups()).getSeconds(),
				getTemplateTestLibFile(LibsHelper.getLibGroupsPath(notebookName)));

		testLibGroupStep.init();
		testLibGroupStep.verify();

		List<LibToSearchData> libToSearchDataList = JsonMapperDto.readListOf(
				getTemplateTestLibFile(LibsHelper.getLibListPath(notebookName)), LibToSearchData.class);

		LOGGER.debug("Skipped libraries for notebook {}: {}", notebookName, skippedLibraries);
		int maxLibsFailedToInstall = libToSearchDataList.size();

		for (LibToSearchData libToSearchData : libToSearchDataList) {
			TestLibListStep testLibListStep = new TestLibListStep(ApiPath.LIB_LIST, token, notebookName,
					getDuration(notebookConfig.getTimeoutLibList()).getSeconds(), libToSearchData);

			testLibListStep.init();
			testLibListStep.verify();

			Lib lib;
			do {
				lib = testLibListStep.getLibs().get(new Random().nextInt(testLibListStep.getLibs().size()));
			} while (skippedLibraries.contains(lib));

			TestLibInstallStep testLibInstallStep =
					new TestLibInstallStep(ApiPath.LIB_INSTALL, ApiPath.LIB_LIST_EXPLORATORY_FORMATTED,
							token, notebookName, getDuration(notebookConfig.getTimeoutLibInstall()).getSeconds(), lib);

			testLibInstallStep.init();
			testLibInstallStep.verify();
			if (!testLibInstallStep.isLibraryInstalled()) {
				libsFailedToInstall++;
			}
			if (libsFailedToInstall == maxLibsFailedToInstall) {
				Assert.fail("Test for library installing is failed: there are not any installed library");
			}

			LOGGER.info("{}: current quantity of failed libs to install: {}", notebookName, libsFailedToInstall);
		}
		LOGGER.info("{}: installed {} testing libraries from {}", notebookName,
				(maxLibsFailedToInstall - libsFailedToInstall), maxLibsFailedToInstall);
	}

	private String getTemplateTestLibFile(String fileName) {
        String absoluteFileName = Paths.get(PropertiesResolver.getNotebookTestLibLocation(), fileName).toString();
        LOGGER.info("Absolute file name is {}", absoluteFileName);
        return absoluteFileName;
   }

   private void restartNotebookAndRedeployToTerminate(DeployClusterDto deployClusterDto) throws Exception {
	   restartNotebook();
	   final String clusterNewName = redeployCluster(deployClusterDto);
	   terminateCluster(clusterNewName);
   }


	private void restartNotebook() throws Exception {
       LOGGER.info("9. Notebook {} will be re-started ...", notebookName);
       String requestBody = "{\"notebook_instance_name\":\"" + notebookName + "\"}";
       Response respStartNotebook = new HttpRequest().webApiPost(ssnExpEnvURL, ContentType.JSON, requestBody, token);
       LOGGER.info("    respStartNotebook.getBody() is {}", respStartNotebook.getBody().asString());
       Assert.assertEquals(respStartNotebook.statusCode(), HttpStatusCode.OK);

		String gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, notebookName,
			VirtualMachineStatusChecker.getStartingStatus(), getDuration(notebookConfig.getTimeoutNotebookStartup()));
       String status = VirtualMachineStatusChecker.getRunningStatus();
       if (!Objects.requireNonNull(status).contains(gettingStatus)){
           throw new Exception("Notebook " + notebookName + " has not been started. Notebook status is " + gettingStatus);
       }
       LOGGER.info("    Notebook {} has been started", notebookName);

       VirtualMachineStatusChecker.checkIfRunning(NamingHelper.getNotebookInstanceName(notebookName), false);

       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "start"), NamingHelper.getSsnIp());
   }

   private void terminateNotebook(String notebookName) throws Exception {
       String gettingStatus;
       LOGGER.info("12. Notebook {} will be terminated ...", notebookName);
       final String ssnTerminateNotebookURL = NamingHelper.getSelfServiceURL(ApiPath.getTerminateNotebookUrl(notebookName));
       Response respTerminateNotebook = new HttpRequest().webApiDelete(ssnTerminateNotebookURL, ContentType.JSON, token);
       LOGGER.info("    respTerminateNotebook.getBody() is {}", respTerminateNotebook.getBody().asString());
       Assert.assertEquals(respTerminateNotebook.statusCode(), HttpStatusCode.OK);

	   gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, notebookName, "terminating",
			   getDuration(notebookConfig.getTimeoutClusterTerminate()));
       if (!gettingStatus.contains("terminated"))
           throw new Exception("Notebook" + notebookName + " has not been terminated. Notebook status is " +
				   gettingStatus);

       VirtualMachineStatusChecker.checkIfTerminated(NamingHelper.getNotebookInstanceName(notebookName), false);
       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "terminate"), NamingHelper.getSsnIp());
   }

   private void terminateNotebook(DeployClusterDto deployCluster) throws Exception {
	   terminateNotebook(deployCluster.getNotebookName());

       String gettingStatus = WaitForStatus.getClusterStatus(
				new HttpRequest()
					.webApiGet(ssnProUserResURL, token)
					.getBody()
					.jsonPath(),
			   deployCluster.getNotebookName(), deployCluster.getName());
       if (!gettingStatus.contains("terminated"))
		   throw new Exception(dataEngineType + " cluster " + deployCluster.getName() + " has not been terminated for Notebook "
				   + deployCluster.getNotebookName() + ". Cluster status is " + gettingStatus);
	   LOGGER.info("    {} cluster {} has been terminated for Notebook {}", dataEngineType, deployCluster.getName(),
			   deployCluster.getNotebookName());

	   VirtualMachineStatusChecker.checkIfTerminated(
			   NamingHelper.getClusterInstanceName(
					   deployCluster.getNotebookName(), deployCluster.getName(), dataEngineType), true);

   }

	private void startCluster() throws Exception {
		String gettingStatus;
		LOGGER.info("    Cluster {} will be started for notebook {} ...", clusterName, notebookName);
		final String ssnStartClusterURL =
				NamingHelper.getSelfServiceURL(ApiPath.getStartClusterUrl(notebookName, clusterName));
		LOGGER.info("    SSN start cluster URL is {}", ssnStartClusterURL);

		Response respStartCluster = new HttpRequest().webApiPut(ssnStartClusterURL, ContentType.JSON, token);
		LOGGER.info("    respStartCluster.getBody() is {}", respStartCluster.getBody().asString());
		Assert.assertEquals(respStartCluster.statusCode(), HttpStatusCode.OK);

		gettingStatus = WaitForStatus.cluster(ssnProUserResURL, token, notebookName, clusterName, "starting",
				getDuration(notebookConfig.getTimeoutClusterStartup()));
		if (!gettingStatus.contains("running"))
			throw new Exception(dataEngineType + " cluster " + clusterName +
					" has not been started. Cluster status is " + gettingStatus);
		LOGGER.info("    {} cluster {} has been started for notebook {}", dataEngineType, clusterName,
				notebookName);

		VirtualMachineStatusChecker.checkIfRunning(
				NamingHelper.getClusterInstanceName(notebookName, clusterName, dataEngineType), true);

		Docker.checkDockerStatus(
				NamingHelper.getClusterContainerName(notebookName, clusterName, "start"), NamingHelper.getSsnIp());
	}

	private void stopCluster() throws Exception {
		String gettingStatus;
		LOGGER.info("    Cluster {} will be stopped for notebook {} ...", clusterName, notebookName);
		final String ssnStopClusterURL =
				NamingHelper.getSelfServiceURL(ApiPath.getStopClusterUrl(notebookName, clusterName));
		LOGGER.info("    SSN stop cluster URL is {}", ssnStopClusterURL);

		Response respStopCluster = new HttpRequest().webApiDelete(ssnStopClusterURL, ContentType.JSON, token);
		LOGGER.info("    respStopCluster.getBody() is {}", respStopCluster.getBody().asString());
		Assert.assertEquals(respStopCluster.statusCode(), HttpStatusCode.OK);

		gettingStatus = WaitForStatus.cluster(ssnProUserResURL, token, notebookName, clusterName, "stopping",
				getDuration(notebookConfig.getTimeoutClusterStop()));
		if (!gettingStatus.contains("stopped"))
			throw new Exception(dataEngineType + " cluster " + clusterName +
					" has not been stopped. Cluster status is " + gettingStatus);
		LOGGER.info("    {} cluster {} has been stopped for notebook {}", dataEngineType, clusterName,
				notebookName);

		VirtualMachineStatusChecker.checkIfStopped(
				NamingHelper.getClusterInstanceName(notebookName, clusterName, dataEngineType), true);

		Docker.checkDockerStatus(
				NamingHelper.getClusterContainerName(notebookName, clusterName, "stop"), NamingHelper.getSsnIp());
	}
   
   private void terminateCluster(String clusterNewName) throws Exception {
       String gettingStatus;
       LOGGER.info("    New cluster {} will be terminated for notebook {} ...", clusterNewName, notebookName);
	   final String ssnTerminateClusterURL =
			   NamingHelper.getSelfServiceURL(ApiPath.getTerminateClusterUrl(notebookName, clusterNewName));
       LOGGER.info("    SSN terminate cluster URL is {}", ssnTerminateClusterURL);

       Response respTerminateCluster = new HttpRequest().webApiDelete(ssnTerminateClusterURL, ContentType.JSON, token);
       LOGGER.info("    respTerminateCluster.getBody() is {}", respTerminateCluster.getBody().asString());
       Assert.assertEquals(respTerminateCluster.statusCode(), HttpStatusCode.OK);

	   gettingStatus = WaitForStatus.cluster(ssnProUserResURL, token, notebookName, clusterNewName, "terminating",
			   getDuration(notebookConfig.getTimeoutClusterTerminate()));
       if (!gettingStatus.contains("terminated"))
		   throw new Exception("New " + dataEngineType + " cluster " + clusterNewName +
				   " has not been terminated. Cluster status is " + gettingStatus);
       LOGGER.info("    New {} cluster {} has been terminated for notebook {}",dataEngineType, clusterNewName,
			   notebookName);

	   VirtualMachineStatusChecker.checkIfTerminated(
			   NamingHelper.getClusterInstanceName(notebookName, clusterNewName, dataEngineType), true);

	   Docker.checkDockerStatus(
			   NamingHelper.getClusterContainerName(notebookName, clusterNewName, "terminate"),
			   NamingHelper.getSsnIp());
   }

   private String redeployCluster(DeployClusterDto deployCluster) throws Exception {
       final String clusterNewName = "New" + clusterName;
       String gettingStatus;

	   LOGGER.info("10. New {} cluster {} will be deployed for termination for notebook {} ...", dataEngineType,
			   clusterNewName, notebookName);

       deployCluster.setName(clusterNewName);
	   deployCluster.setNotebookName(notebookName);
       Response responseDeployingClusterNew = new HttpRequest().webApiPut(ssnCompResURL, ContentType.JSON, deployCluster, token);
       LOGGER.info("    responseDeployingClusterNew.getBody() is {}", responseDeployingClusterNew.getBody().asString());
       Assert.assertEquals(responseDeployingClusterNew.statusCode(), HttpStatusCode.OK);

	   gettingStatus = WaitForStatus.cluster(ssnProUserResURL, token, notebookName, clusterNewName, "creating",
			   getDuration(notebookConfig.getTimeoutClusterCreate()));
       if (!(gettingStatus.contains("configuring") || gettingStatus.contains("running")))
           throw new Exception("New cluster " + clusterNewName + " has not been deployed. Cluster status is " + gettingStatus);
       LOGGER.info("    New cluster {} has been deployed", clusterNewName);

       LOGGER.info("   Waiting until cluster {} has been configured ...", clusterNewName);
	   gettingStatus = WaitForStatus.cluster(ssnProUserResURL, token, notebookName, clusterNewName, "configuring",
			   getDuration(notebookConfig.getTimeoutClusterCreate()));
       if (!gettingStatus.contains("running"))
           throw new Exception("Cluster " + clusterNewName + " has not been configured. Cluster status is " +
				   gettingStatus);
       LOGGER.info("   Cluster {} has been configured", clusterNewName);

	   VirtualMachineStatusChecker.checkIfRunning(
			   NamingHelper.getClusterInstanceName(notebookName, clusterNewName, dataEngineType), true);

	   Docker.checkDockerStatus(NamingHelper.getClusterContainerName(notebookName, clusterNewName, "create"),
			   NamingHelper.getSsnIp());
       return clusterNewName;
   }

   private void stopEnvironment() throws Exception {
       String gettingStatus;
       LOGGER.info("8. Notebook {} will be stopped ...", notebookName);
       final String ssnStopNotebookURL = NamingHelper.getSelfServiceURL(ApiPath.getStopNotebookUrl(notebookName));
       LOGGER.info("   SSN stop notebook URL is {}", ssnStopNotebookURL);

       Response responseStopNotebook = new HttpRequest().webApiDelete(ssnStopNotebookURL, ContentType.JSON, token);
       LOGGER.info("   responseStopNotebook.getBody() is {}", responseStopNotebook.getBody().asString());
	   Assert.assertEquals(responseStopNotebook.statusCode(), HttpStatusCode.OK, "Notebook " + notebookName +
			   " was not stopped");

	   gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, notebookName, "stopping",
			   getDuration(notebookConfig.getTimeoutNotebookShutdown()));
       if (!gettingStatus.contains("stopped"))
           throw new Exception("Notebook " + notebookName + " has not been stopped. Notebook status is " +
				   gettingStatus);
       LOGGER.info("   Notebook {} has been stopped", notebookName);
	   if (!clusterName.equalsIgnoreCase(NamingHelper.CLUSTER_ABSENT)) {
		   gettingStatus = WaitForStatus.getClusterStatus(
				   new HttpRequest()
						   .webApiGet(ssnProUserResURL, token)
						   .getBody()
						   .jsonPath(),
				   notebookName, clusterName);

		   if (NamingHelper.DATA_ENGINE.equals(dataEngineType) && !gettingStatus.contains("stopped")){
			   throw new Exception("Computational resources has not been stopped for Notebook " + notebookName +
					   ". Data engine status is " + gettingStatus);
		   } else if (NamingHelper.DATA_ENGINE_SERVICE.equals(dataEngineType) &&
				   !ConfigPropertyValue.getCloudProvider().equalsIgnoreCase(CloudProvider.AZURE_PROVIDER)
				   && !gettingStatus.contains("terminated")){
			   throw new Exception("Computational resources has not been terminated for Notebook " + notebookName +
					   ". Data engine service status is " + gettingStatus);
		   }

		   LOGGER.info("   Computational resources has been terminated for notebook {}", notebookName);

		   if (NamingHelper.DATA_ENGINE.equals(dataEngineType)){
			   VirtualMachineStatusChecker.checkIfStopped(NamingHelper.getClusterInstanceName(notebookName,
					   clusterName, dataEngineType), true);
		   } else if (NamingHelper.DATA_ENGINE_SERVICE.equals(dataEngineType)){
			   VirtualMachineStatusChecker.checkIfTerminated(NamingHelper.getClusterInstanceName(notebookName,
					   clusterName, dataEngineType), true);
		   }

	   }
       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "stop"), NamingHelper.getSsnIp());
   }
}
