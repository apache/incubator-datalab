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

package com.epam.dlab.automation.test;

import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import com.epam.dlab.automation.test.libs.TestLibGroupStep;
import com.epam.dlab.automation.test.libs.TestLibInstallStep;
import com.epam.dlab.automation.test.libs.TestLibListStep;
import com.epam.dlab.automation.test.libs.models.Lib;
import com.epam.dlab.automation.test.libs.models.LibToSearchData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import com.epam.dlab.automation.aws.AmazonHelper;
import com.epam.dlab.automation.aws.AmazonInstanceState;
import com.epam.dlab.automation.docker.Docker;
import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.epam.dlab.automation.helper.PropertiesResolver;
import com.epam.dlab.automation.helper.NamingHelper;
import com.epam.dlab.automation.helper.WaitForStatus;
import com.epam.dlab.automation.http.ApiPath;
import com.epam.dlab.automation.http.ContentType;
import com.epam.dlab.automation.http.HttpRequest;
import com.epam.dlab.automation.http.HttpStatusCode;
import com.epam.dlab.automation.model.CreateNotebookDto;
import com.epam.dlab.automation.model.DeployEMRDto;
import com.epam.dlab.automation.model.DeploySparkStandaloneDto;
import com.epam.dlab.automation.model.JsonMapperDto;
import com.jayway.restassured.response.Response;

public class TestCallable implements Callable<Boolean> {
    private final static Logger LOGGER = LogManager.getLogger(TestCallable.class);

    private final String notebookTemplate;
    private final boolean fullTest;
    private final String token, ssnExpEnvURL, ssnProUserResURL;
    private final String bucketName;
    private final String notebookName, emrName, dataEngineType;

    public TestCallable(String notebookTemplate,String dataEngineType, boolean fullTest) {
    	this.notebookTemplate = notebookTemplate;
    	this.dataEngineType = dataEngineType;
        this.fullTest = fullTest;

        this.token = NamingHelper.getSsnToken();
        this.ssnExpEnvURL = NamingHelper.getSelfServiceURL(ApiPath.EXP_ENVIRONMENT);
        this.ssnProUserResURL = NamingHelper.getSelfServiceURL(ApiPath.PROVISIONED_RES);
        this.bucketName = NamingHelper.getBucketName();

        final String suffixName = NamingHelper.generateRandomValue(notebookTemplate);
        notebookName = "nb" + suffixName;
        emrName = "eimr" + suffixName;

        LOGGER.info("   SSN exploratory environment URL is {}", ssnExpEnvURL);
        LOGGER.info("   SSN provisioned user resources URL is {}", ssnProUserResURL);
    }

    @Override
    public Boolean call() throws Exception {
       
        switch (dataEngineType) {
		case "dataengine":
			testEMR();
			break;
		case "dataengine-service":
			testSparkStandalone();
			break;
		default:
			LOGGER.error("illegal dataengine type {}",dataEngineType);
			fail("illegal dataengine type"+dataEngineType, new IllegalArgumentException("illegal dataengine type"+dataEngineType));
			break;
		}
        

       	// Create notebook from AMI
       	String notebookNewName = "AMI" + notebookName;
       	createNotebook(notebookNewName);

       	terminateNotebook(notebookName);

       	LOGGER.info("{} All tests finished successfully", notebookName);
        return true;
   }
    
    private void testEMR() throws Exception {
		// TODO Auto-generated method stub
    	final String notebookIp = createNotebook(notebookName);
        testLibs();
    	final DeployEMRDto deployEMR = createEMR();

        final String emrClusterName = NamingHelper.getEmrClusterName(NamingHelper.getEmrInstanceName(notebookName, emrName));
        if (!ConfigPropertyValue.isRunModeLocal()) {
        	TestEmr test = new TestEmr();
        	test.run(notebookName, emrClusterName);

            String notebookFilesLocation = PropertiesResolver.getPropertyByName(String.format(PropertiesResolver.NOTEBOOK_FILES_LOCATION_PROPERTY_TEMPLATE, notebookTemplate));
            test.run2(NamingHelper.getSsnIp(), notebookIp, emrClusterName, new File(notebookFilesLocation), notebookName);
        }

        stopEnvironment();

        if (fullTest) {
        	restartNotebookAndRedeployToTerminate(deployEMR);
        }
        if (deployEMR != null) {
        	terminateNotebook(deployEMR);
        }
	}

   private void testSparkStandalone() throws Exception {
		// TODO Auto-generated method stub
	   final String notebookIp = createNotebook(notebookName);
       testLibs();
//   	final DeployEMRDto deployEMR = createEMR();
   	final DeploySparkStandaloneDto dSparkStandaloneDto = createStandalone();

       final String emrClusterName = NamingHelper.getEmrClusterName(NamingHelper.getEmrInstanceName(notebookName, emrName));
       if (!ConfigPropertyValue.isRunModeLocal()) {
       	TestEmr test = new TestEmr();
       	test.run(notebookName, emrClusterName);

           String notebookFilesLocation = PropertiesResolver.getPropertyByName(String.format(PropertiesResolver.NOTEBOOK_FILES_LOCATION_PROPERTY_TEMPLATE, notebookTemplate));
           test.run2(NamingHelper.getSsnIp(), notebookIp, emrClusterName, new File(notebookFilesLocation), notebookName);
       }

       stopEnvironment();

       if (fullTest) {
       	restartNotebookAndRedeployToTerminate(dSparkStandaloneDto);
       }
       if (dSparkStandaloneDto != null) {
       	terminateNotebook(dSparkStandaloneDto);
       }
	}

  

private DeploySparkStandaloneDto createStandalone() throws Exception {
	// TODO Auto-generated method stub
	String gettingStatus;
    LOGGER.info("7. SparkStamdalone {} will be deployed for {} ...", emrName, notebookName);
    final String ssnCompResURL = NamingHelper.getSelfServiceURL(ApiPath.COMPUTATIONAL_RES_STANDALONE);
    LOGGER.info("  {} : SSN computational resources URL is {}", notebookName, ssnCompResURL);

    DeploySparkStandaloneDto sparkStandaloneDto=
            JsonMapperDto.readNode(
                    Paths.get(PropertiesResolver.getClusterConfFileLocation(), "SparkStandalone.json").toString(),
                    DeploySparkStandaloneDto.class);

    //TODO: add parameter for switching from regular ec2 instances to spot instances
    /*DeployEMRDto deployEMRSpot40 =
            NodeReader.readNode(
                    Paths.get(PropertiesResolver.getClusterConfFileLocation(), "EMR_spot.json").toString(),
                    DeployEMRDto.class);*/

    sparkStandaloneDto.setName(emrName);
    sparkStandaloneDto.setNotebook_name(notebookName);
    LOGGER.info("{}: SparkStamdalone = {}",notebookName, sparkStandaloneDto);
    Response responseDeployingSparkStandalone = new HttpRequest().webApiPut(ssnCompResURL, ContentType.JSON,
    		sparkStandaloneDto, token);
    LOGGER.info("{}:   responseDeployingSparkStandalone.getBody() is {}",notebookName, responseDeployingSparkStandalone.getBody().asString());
    Assert.assertEquals(responseDeployingSparkStandalone.statusCode(), HttpStatusCode.OK, "SparkStamdalone  " + emrName + " was not deployed");

    gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrName, "creating", ConfigPropertyValue.getTimeoutEMRCreate());
    if(!ConfigPropertyValue.isRunModeLocal()) {
        if (!(gettingStatus.contains("configuring") || gettingStatus.contains("running")))
            throw new Exception(notebookName + ": SparkStamdalone " + emrName + " has not been deployed. SparkStamdalone status is " + gettingStatus);
        LOGGER.info("{}: SparkStamdalone {} has been deployed", notebookName, emrName);

        AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.RUNNING);
        Docker.checkDockerStatus(NamingHelper.getEmrContainerName(emrName, "create"), NamingHelper.getSsnIp());
    }
    LOGGER.info("{}:   Waiting until SparkStamdalone has been configured ...", notebookName);

    gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrName, "configuring", ConfigPropertyValue.getTimeoutEMRCreate());
    if (!gettingStatus.contains("running"))
        throw new Exception(notebookName + ": SparkStamdalone " + emrName + " has not been configured. SparkStamdalone status is " + gettingStatus);
    LOGGER.info(" {}:  SparkStamdalone {} has been configured", notebookName, emrName);

    if(!ConfigPropertyValue.isRunModeLocal()) {
        AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.RUNNING);
        Docker.checkDockerStatus(NamingHelper.getEmrContainerName(emrName, "create"), NamingHelper.getSsnIp());
    }

    LOGGER.info("{}:   Check bucket {}", notebookName, bucketName);
    AmazonHelper.printBucketGrants(bucketName);

    return sparkStandaloneDto;
}

private String  createNotebook(String notebookName) throws Exception {
       LOGGER.info("6. Notebook {} will be created ...", notebookName);
       String notebookConfigurationFile = String.format(PropertiesResolver.NOTEBOOK_CONFIGURATION_FILE_TEMPLATE, notebookTemplate);
       LOGGER.info("{} notebook configuration file: {}", notebookName, notebookConfigurationFile);

       CreateNotebookDto createNoteBookRequest =
               JsonMapperDto.readNode(
                       Paths.get(PropertiesResolver.getClusterConfFileLocation(), notebookConfigurationFile).toString(),
                       CreateNotebookDto.class);

       createNoteBookRequest.setName(notebookName);

       Response responseCreateNotebook = new HttpRequest().webApiPut(ssnExpEnvURL, ContentType.JSON,
                   createNoteBookRequest, token);
       LOGGER.info(" {}:  responseCreateNotebook.getBody() is {}", notebookName, responseCreateNotebook.getBody().asString());
       Assert.assertEquals(responseCreateNotebook.statusCode(), HttpStatusCode.OK, "Notebook " + notebookName + " was not created");

       String gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, notebookName, "creating", ConfigPropertyValue.getTimeoutNotebookCreate());
       if (!gettingStatus.contains("running")) {
           LOGGER.error("Notebook {} is in state {}", notebookName, gettingStatus);
           throw new Exception("Notebook " + notebookName + " has not been created. Notebook status is " + gettingStatus);
       }
       LOGGER.info("   Notebook {} has been created", notebookName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getNotebookInstanceName(notebookName), AmazonInstanceState.RUNNING);
       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "create"), NamingHelper.getSsnIp());

       LOGGER.info("   Notebook {} status has been verified", notebookName);
       //get notebook IP
       String notebookIp = AmazonHelper.getInstance(NamingHelper.getNotebookInstanceName(notebookName))
    		   .getPrivateIpAddress();
       LOGGER.info("   Notebook {} IP is {}", notebookName, notebookIp);

       return notebookIp;
   }

   private void testLibs() throws Exception {
       LOGGER.info("Install libraries  ...", notebookName);

       TestLibGroupStep testLibGroupStep = new TestLibGroupStep(ApiPath.LIB_GROUPS, token, notebookName,
               ConfigPropertyValue.getTimeoutLibGroups().getSeconds(),
               getTemplateTestLibFile("lib_groups.json"));

       testLibGroupStep.init();
       testLibGroupStep.verify();

       List<LibToSearchData> libToSearchDataList = JsonMapperDto.readListOf(getTemplateTestLibFile("lib_list.json"),
               LibToSearchData.class);

       for (LibToSearchData libToSearchData : libToSearchDataList) {
           TestLibListStep testLibListStep = new TestLibListStep(ApiPath.LIB_LIST, token, notebookName,
                   ConfigPropertyValue.getTimeoutLibList().getSeconds(), libToSearchData);

           testLibListStep.init();
           testLibListStep.verify();

           Lib lib = testLibListStep.getLibs().get(new Random().nextInt(testLibListStep.getLibs().size()));

           TestLibInstallStep testLibInstallStep = new TestLibInstallStep(ApiPath.LIB_INSTALL, ApiPath.LIB_LIST_EXPLORATORY,
                   token, notebookName, ConfigPropertyValue.getTimeoutLibInstall().getSeconds(), lib);

           testLibInstallStep.init();
           testLibInstallStep.verify();
       }
   }

   private String getTemplateTestLibFile(String fileName) {
        String absoluteFileName;
        if (PropertiesResolver.DEV_MODE) {
            absoluteFileName = Paths.get(PropertiesResolver.getNotebookTestLibLocation(), fileName).toString();
        } else {
            absoluteFileName = Paths.get(PropertiesResolver.getNotebookTestLibLocation(), notebookTemplate,
                    fileName).toString();
        }
        LOGGER.info("Absolute file name is {}", absoluteFileName);
        return absoluteFileName;
   }

   private DeployEMRDto createEMR() throws Exception {
       String gettingStatus;
       LOGGER.info("7. EMR {} will be deployed for {} ...", emrName, notebookName);
       final String ssnCompResURL = NamingHelper.getSelfServiceURL(ApiPath.COMPUTATIONAL_RES);
       LOGGER.info("  {} : SSN computational resources URL is {}", notebookName, ssnCompResURL);

       DeployEMRDto deployEMR =
               JsonMapperDto.readNode(
                       Paths.get(PropertiesResolver.getClusterConfFileLocation(), "EMR.json").toString(),
                       DeployEMRDto.class);

       //TODO: add parameter for switching from regular ec2 instances to spot instances
       /*DeployEMRDto deployEMRSpot40 =
               NodeReader.readNode(
                       Paths.get(PropertiesResolver.getClusterConfFileLocation(), "EMR_spot.json").toString(),
                       DeployEMRDto.class);*/

       deployEMR.setName(emrName);
       deployEMR.setNotebook_name(notebookName);
       LOGGER.info("{}: EMR = {}",notebookName, deployEMR);
       Response responseDeployingEMR = new HttpRequest().webApiPut(ssnCompResURL, ContentType.JSON,
               deployEMR, token);
       LOGGER.info("{}:   responseDeployingEMR.getBody() is {}",notebookName, responseDeployingEMR.getBody().asString());
       Assert.assertEquals(responseDeployingEMR.statusCode(), HttpStatusCode.OK, "EMR  " + emrName + " was not deployed");

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrName, "creating", ConfigPropertyValue.getTimeoutEMRCreate());
       if(!ConfigPropertyValue.isRunModeLocal()) {
           if (!(gettingStatus.contains("configuring") || gettingStatus.contains("running")))
               throw new Exception(notebookName + ": EMR " + emrName + " has not been deployed. EMR status is " + gettingStatus);
           LOGGER.info("{}: EMR {} has been deployed", notebookName, emrName);

           AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.RUNNING);
           Docker.checkDockerStatus(NamingHelper.getEmrContainerName(emrName, "create"), NamingHelper.getSsnIp());
       }
       LOGGER.info("{}:   Waiting until EMR has been configured ...", notebookName);

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrName, "configuring", ConfigPropertyValue.getTimeoutEMRCreate());
       if (!gettingStatus.contains("running"))
           throw new Exception(notebookName + ": EMR " + emrName + " has not been configured. EMR status is " + gettingStatus);
       LOGGER.info(" {}:  EMR {} has been configured", notebookName, emrName);

       if(!ConfigPropertyValue.isRunModeLocal()) {
           AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.RUNNING);
           Docker.checkDockerStatus(NamingHelper.getEmrContainerName(emrName, "create"), NamingHelper.getSsnIp());
       }

       LOGGER.info("{}:   Check bucket {}", notebookName, bucketName);
       AmazonHelper.printBucketGrants(bucketName);

       return deployEMR;
   }

   private void restartNotebookAndRedeployToTerminate(DeployEMRDto deployEMR) throws Exception {
	   restartNotebook();
	   final String emrNewName = redeployEMR(deployEMR);
	   terminateEMR(emrNewName);
   }
   

private void restartNotebookAndRedeployToTerminate(DeploySparkStandaloneDto dSparkStandaloneDto) throws Exception {
	// TODO Auto-generated method stub
	restartNotebook();
	   final String emrNewName = redeploySparkStandalone(dSparkStandaloneDto);
	   terminateEMR(emrNewName);
}


   private String redeploySparkStandalone(DeploySparkStandaloneDto dSparkStandaloneDto) throws Exception {
	   final String emrNewName = "New" + emrName;
       String gettingStatus;

       LOGGER.info("10. New EMR {} will be deployed for termination for notebook {} ...", emrNewName, notebookName);

       dSparkStandaloneDto.setName(emrNewName);
       dSparkStandaloneDto.setNotebook_name(notebookName);
       Response responseDeployingEMRNew = new HttpRequest().webApiPut(NamingHelper.getSelfServiceURL(ApiPath.COMPUTATIONAL_RES_STANDALONE),
                                                                      ContentType.JSON, dSparkStandaloneDto, token);
       LOGGER.info("    responseDeployingEMRNew.getBody() is {}", responseDeployingEMRNew.getBody().asString());
       Assert.assertEquals(responseDeployingEMRNew.statusCode(), HttpStatusCode.OK);

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrNewName, "creating", ConfigPropertyValue.getTimeoutEMRCreate());
       if (!(gettingStatus.contains("configuring") || gettingStatus.contains("running")))
           throw new Exception("New EMR " + emrNewName + " has not been deployed. EMR status is " + gettingStatus);
       LOGGER.info("    New EMR {} has been deployed", emrNewName);

       LOGGER.info("   Waiting until EMR {} has been configured ...", emrNewName);
       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrNewName, "configuring", ConfigPropertyValue.getTimeoutEMRCreate());
       if (!gettingStatus.contains(AmazonInstanceState.RUNNING.toString()))
           throw new Exception("EMR " + emrNewName + " has not been configured. EMR status is " + gettingStatus);
       LOGGER.info("   EMR {} has been configured", emrNewName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrNewName), AmazonInstanceState.RUNNING);
       Docker.checkDockerStatus(NamingHelper.getEmrContainerName(emrNewName, "create"), NamingHelper.getSsnIp());
       return emrNewName;
}

private void restartNotebook() throws Exception {
       LOGGER.info("9. Notebook {} will be re-started ...", notebookName);
       String requestBody = "{\"notebook_instance_name\":\"" + notebookName + "\"}";
       Response respStartNotebook = new HttpRequest().webApiPost(ssnExpEnvURL, ContentType.JSON, requestBody, token);
       LOGGER.info("    respStartNotebook.getBody() is {}", respStartNotebook.getBody().asString());
       Assert.assertEquals(respStartNotebook.statusCode(), HttpStatusCode.OK);

       String gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, notebookName, "starting", ConfigPropertyValue.getTimeoutNotebookStartup());
       if (!gettingStatus.contains(AmazonInstanceState.RUNNING.toString())){
           throw new Exception("Notebook " + notebookName + " has not been started. Notebook status is " + gettingStatus);
       }
       LOGGER.info("    Notebook {} has been started", notebookName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getNotebookInstanceName(notebookName), AmazonInstanceState.RUNNING);
       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "start"), NamingHelper.getSsnIp());
   }

   private void terminateNotebook(String notebookName) throws Exception {
       String gettingStatus;
       LOGGER.info("12. Notebook {} will be terminated ...", notebookName);
       final String ssnTerminateNotebookURL = NamingHelper.getSelfServiceURL(ApiPath.getTerminateNotebookUrl(notebookName));
       Response respTerminateNotebook = new HttpRequest().webApiDelete(ssnTerminateNotebookURL, ContentType.JSON, token);
       LOGGER.info("    respTerminateNotebook.getBody() is {}", respTerminateNotebook.getBody().asString());
       Assert.assertEquals(respTerminateNotebook.statusCode(), HttpStatusCode.OK);

       gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, notebookName, "terminating", ConfigPropertyValue.getTimeoutEMRTerminate());
       if (!gettingStatus.contains("terminated"))
           throw new Exception("Notebook" + notebookName + " has not been terminated. Notebook status is " + gettingStatus);
       AmazonHelper.checkAmazonStatus(NamingHelper.getNotebookInstanceName(notebookName), AmazonInstanceState.TERMINATED);
       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "terminate"), NamingHelper.getSsnIp());
   }

   private void terminateNotebook(DeployEMRDto deployEmr) throws Exception {
       terminateNotebook(deployEmr.getNotebook_name());

       String gettingStatus = WaitForStatus.getEmrStatus(
				new HttpRequest()
					.webApiGet(ssnProUserResURL, token)
					.getBody()
					.jsonPath(),
					deployEmr.getNotebook_name(), deployEmr.getName());
       if (!gettingStatus.contains("terminated"))
           throw new Exception("EMR has not been terminated for Notebook " + deployEmr.getNotebook_name() + ". EMR status is " + gettingStatus);
       LOGGER.info("    EMR {} has been terminated for Notebook {}", deployEmr.getName(), deployEmr.getNotebook_name());

       AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(deployEmr.getNotebook_name(), deployEmr.getName()), AmazonInstanceState.TERMINATED);
   }
   
   private void terminateNotebook(DeploySparkStandaloneDto dSparkStandalone) throws Exception {
       terminateNotebook(dSparkStandalone.getNotebook_name());

       String gettingStatus = WaitForStatus.getEmrStatus(
				new HttpRequest()
					.webApiGet(ssnProUserResURL, token)
					.getBody()
					.jsonPath(),
					dSparkStandalone.getNotebook_name(), dSparkStandalone.getName());
       if (!gettingStatus.contains("terminated"))
           throw new Exception("EMR has not been terminated for Notebook " + dSparkStandalone.getNotebook_name() + ". EMR status is " + gettingStatus);
       LOGGER.info("    EMR {} has been terminated for Notebook {}", dSparkStandalone.getName(), dSparkStandalone.getNotebook_name());

       AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(dSparkStandalone.getNotebook_name(), dSparkStandalone.getName()), AmazonInstanceState.TERMINATED);
   }

   private void terminateEMR(String emrNewName) throws Exception {
       String gettingStatus;
       LOGGER.info("    New EMR {} will be terminated for notebook {} ...", emrNewName, notebookName);
       final String ssnTerminateEMRURL = NamingHelper.getSelfServiceURL(ApiPath.getTerminateEMRUrl(notebookName, emrNewName));
       LOGGER.info("    SSN terminate EMR URL is {}", ssnTerminateEMRURL);

       Response respTerminateEMR = new HttpRequest().webApiDelete(ssnTerminateEMRURL, ContentType.JSON, token);
       LOGGER.info("    respTerminateEMR.getBody() is {}", respTerminateEMR.getBody().asString());
       Assert.assertEquals(respTerminateEMR.statusCode(), HttpStatusCode.OK);

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrNewName, "terminating", ConfigPropertyValue.getTimeoutEMRTerminate());
       if (!gettingStatus.contains("terminated"))
           throw new Exception("New EMR " + emrNewName + " has not been terminated. EMR status is " + gettingStatus);
       LOGGER.info("    New EMR {} has been terminated for notebook {}", emrNewName, notebookName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrNewName), AmazonInstanceState.TERMINATED);
       Docker.checkDockerStatus(NamingHelper.getEmrContainerName(emrNewName, "terminate"), NamingHelper.getSsnIp());
   }

   private String redeployEMR(DeployEMRDto deployEMR) throws Exception {
       final String emrNewName = "New" + emrName;
       String gettingStatus;

       LOGGER.info("10. New EMR {} will be deployed for termination for notebook {} ...", emrNewName, notebookName);

       deployEMR.setName(emrNewName);
       deployEMR.setNotebook_name(notebookName);
       Response responseDeployingEMRNew = new HttpRequest().webApiPut(NamingHelper.getSelfServiceURL(ApiPath.COMPUTATIONAL_RES),
                                                                      ContentType.JSON, deployEMR, token);
       LOGGER.info("    responseDeployingEMRNew.getBody() is {}", responseDeployingEMRNew.getBody().asString());
       Assert.assertEquals(responseDeployingEMRNew.statusCode(), HttpStatusCode.OK);

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrNewName, "creating", ConfigPropertyValue.getTimeoutEMRCreate());
       if (!(gettingStatus.contains("configuring") || gettingStatus.contains("running")))
           throw new Exception("New EMR " + emrNewName + " has not been deployed. EMR status is " + gettingStatus);
       LOGGER.info("    New EMR {} has been deployed", emrNewName);

       LOGGER.info("   Waiting until EMR {} has been configured ...", emrNewName);
       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrNewName, "configuring", ConfigPropertyValue.getTimeoutEMRCreate());
       if (!gettingStatus.contains(AmazonInstanceState.RUNNING.toString()))
           throw new Exception("EMR " + emrNewName + " has not been configured. EMR status is " + gettingStatus);
       LOGGER.info("   EMR {} has been configured", emrNewName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrNewName), AmazonInstanceState.RUNNING);
       Docker.checkDockerStatus(NamingHelper.getEmrContainerName(emrNewName, "create"), NamingHelper.getSsnIp());
       return emrNewName;
   }

   private void stopEnvironment() throws Exception {
       String gettingStatus;
       LOGGER.info("8. Notebook {} will be stopped ...", notebookName);
       final String ssnStopNotebookURL = NamingHelper.getSelfServiceURL(ApiPath.getStopNotebookUrl(notebookName));
       LOGGER.info("   SSN stop notebook URL is {}", ssnStopNotebookURL);

       Response responseStopNotebook = new HttpRequest().webApiDelete(ssnStopNotebookURL, ContentType.JSON, token);
       LOGGER.info("   responseStopNotebook.getBody() is {}", responseStopNotebook.getBody().asString());
       Assert.assertEquals(responseStopNotebook.statusCode(), HttpStatusCode.OK, "Notebook " + notebookName + " was not stopped");

       gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, notebookName, "stopping", ConfigPropertyValue.getTimeoutNotebookShutdown());
       if (!gettingStatus.contains("stopped"))
           throw new Exception("Notebook " + notebookName + " has not been stopped. Notebook status is " + gettingStatus);
       LOGGER.info("   Notebook {} has been stopped", notebookName);
       gettingStatus = WaitForStatus.getEmrStatus(
               new HttpRequest()
                       .webApiGet(ssnProUserResURL, token)
                       .getBody()
                       .jsonPath(),
               notebookName, emrName);

       if (!gettingStatus.contains("terminated"))
           throw new Exception("Computational resources has not been terminated for Notebook " + notebookName + ". EMR status is " + gettingStatus);
       LOGGER.info("   Computational resources has been terminated for notebook {}", notebookName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.TERMINATED);
       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "stop"), NamingHelper.getSsnIp());
   }
}
