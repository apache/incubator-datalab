package com.epam.dlab.automation.test;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import com.epam.dlab.automation.aws.AmazonHelper;
import com.epam.dlab.automation.aws.AmazonInstanceState;
import com.epam.dlab.automation.aws.NodeReader;
import com.epam.dlab.automation.docker.Docker;
import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.epam.dlab.automation.helper.PropertiesResolver;
import com.epam.dlab.automation.helper.TestNamingHelper;
import com.epam.dlab.automation.helper.WaitForStatus;
import com.epam.dlab.automation.http.HttpRequest;
import com.epam.dlab.automation.http.HttpStatusCode;
import com.epam.dlab.automation.model.CreateNotebookDto;
import com.epam.dlab.automation.model.DeployEMRDto;
import com.epam.dlab.automation.repository.ApiPath;
import com.epam.dlab.automation.repository.ContentType;
import com.jayway.restassured.response.Response;

public class TestCallable implements Callable<Boolean> {
    private final static Logger LOGGER = LogManager.getLogger(TestCallable.class);
    
    private final String serviceBaseName, ssnURL, ssnIp, notebook, nodePrefix, token;
    private final boolean testRestartStopped;
    private final String amazonNodePrefix, ssnExpEnvURL, ssnProUserResURL;
    private final String bucketName;
    
    public TestCallable(String serviceBaseName, String ssnURL, String ssnIp, String notebook, String nodePrefix, String token, boolean testRestartStopped) {
    	this.serviceBaseName = serviceBaseName;
    	this.ssnURL = ssnURL;
    	this.ssnIp = ssnIp;
    	this.notebook = notebook;
        this.nodePrefix = nodePrefix;
        this.token = token;
        this.testRestartStopped = testRestartStopped;
        
        this.amazonNodePrefix = TestNamingHelper.getAmazonNodePrefix(serviceBaseName, nodePrefix);
        this.ssnExpEnvURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.EXP_ENVIRONMENT);
        this.ssnProUserResURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.PROVISIONED_RES);
        this.bucketName = TestNamingHelper.getBucketName(serviceBaseName);
        
        LOGGER.info("   SSN exploratory environment URL is {}", ssnExpEnvURL);
        LOGGER.info("   SSN provisioned user resources URL is {}", ssnProUserResURL);

    }

    @Override
    public Boolean call() throws Exception {

//      String notebookTemplateNamePrefix = notebook.substring(0, notebook.indexOf(".") > 0 ? notebook.indexOf(".") : notebook.length() - 1);
        String notebookTemplateName = TestNamingHelper.generateRandomValue(notebook);

//      String testNoteBookName = "NotebookAutoTest_R-Studio_20170516122058";
//      String testNoteBookName = "NotebookAutoTest_Zeppelin_2017051633454";
        String testNoteBookName = "Notebook" + notebookTemplateName;

//      String emrName = "eimrAutoTest_R-Studio_20170516125150";
//      String emrName = "eimrAutoTest_Zeppelin_2017051641947";
        String emrName = "eimr" + notebookTemplateName;

        String notebookIp = createNotebook(testNoteBookName, notebook, nodePrefix, amazonNodePrefix, token, ssnExpEnvURL, ssnProUserResURL);

        DeployEMRDto deployEMR = createEMR(testNoteBookName, emrName, nodePrefix, amazonNodePrefix, token, ssnProUserResURL);

        String emrClusterName = TestNamingHelper.getEmrClusterName(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrName);
        if (!ConfigPropertyValue.isRunModeLocal()) {
        	TestEmr test = new TestEmr(serviceBaseName, ssnIp);
        	test.run(emrName, emrClusterName, notebook);

            String notebookFilesLocation = PropertiesResolver.getPropertyByName(String.format(PropertiesResolver.NOTEBOOK_FILES_LOCATION_PROPERTY_TEMPLATE, notebook));
            test.run2(ssnIp, notebookIp, emrClusterName, new File(notebookFilesLocation), notebook);
        }


        stopEnvironment(nodePrefix, amazonNodePrefix, token, ssnProUserResURL, notebook, testNoteBookName, emrName);

        if (testRestartStopped) {
        	restartNotebookAndRedeployToTerminate(testNoteBookName, ssnExpEnvURL, ssnProUserResURL, TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.COMPUTATIONAL_RES), amazonNodePrefix, nodePrefix, emrName, token, deployEMR);
        }
        if (null != deployEMR) {
        	terminateNotebook(testNoteBookName, ssnProUserResURL, amazonNodePrefix, nodePrefix, deployEMR, token);
        }

        return true;
    }

    /**
    * @param testNoteBookName
    * @param notebook
    * @param nodePrefix
    * @param amazonNodePrefix
    * @param token
    * @param ssnExpEnvURL
    * @param ssnProUserResURL
    * @return notebook IP
    * @throws Exception
    */
   private String  createNotebook(String testNoteBookName, String notebook, String nodePrefix, String amazonNodePrefix, String token, String ssnExpEnvURL, String ssnProUserResURL) throws Exception {
       LOGGER.info("6. Notebook " +  testNoteBookName + " will be created ...");
       String notebookConfigurationFile = String.format(PropertiesResolver.NOTEBOOK_CONFIGURATION_FILE_TEMPLATE, notebook);
       LOGGER.info("{} notebook configuration file {}: {}" , notebook, notebookConfigurationFile);

       CreateNotebookDto createNoteBookRequest =
               NodeReader.readNode(
                       Paths.get(PropertiesResolver.getClusterConfFileLocation(), notebookConfigurationFile).toString(),
                       CreateNotebookDto.class);

           createNoteBookRequest.setName(testNoteBookName);

           Response responseCreateNotebook = new HttpRequest().webApiPut(ssnExpEnvURL, ContentType.JSON,
                   createNoteBookRequest, token);
           LOGGER.info(" {}:  responseCreateNotebook.getBody() is {}", testNoteBookName, responseCreateNotebook.getBody().asString());
           Assert.assertEquals(responseCreateNotebook.statusCode(), HttpStatusCode.OK, "Notebook " + notebook + " was not created");

       String gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, testNoteBookName, "creating", ConfigPropertyValue.getTimeoutNotebookCreate());
       if (!gettingStatus.contains("running")) {
           LOGGER.error("Notebook {} is in state {}", testNoteBookName, gettingStatus);
           throw new Exception("Notebook " + testNoteBookName + " has not been created. Notebook status is " + gettingStatus);
       }
       LOGGER.info("   Notebook {} has been created", testNoteBookName);

       AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-nb-" + testNoteBookName, AmazonInstanceState.RUNNING.value());
       Docker.checkDockerStatus(nodePrefix + "_create_exploratory_NotebookAutoTest", ssnIp);

       LOGGER.info("   Notebook {} status has been verified", testNoteBookName);
       //get notebook IP
       String notebookIp = AmazonHelper.getInstance(amazonNodePrefix + "-nb-" + testNoteBookName)
               .getPrivateIpAddress();
       LOGGER.info("   Notebook {} IP is {}", testNoteBookName, notebookIp);

       return notebookIp;
   }
   
   private DeployEMRDto createEMR(String testNoteBookName, String emrName, String nodePrefix, String amazonNodePrefix, String token, String ssnProUserResURL) throws Exception {
       String gettingStatus;
       LOGGER.info("7. EMR will be deployed {} ...", testNoteBookName);
       final String ssnCompResURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.COMPUTATIONAL_RES);
       LOGGER.info("  {} : SSN computational resources URL is {}", testNoteBookName, ssnCompResURL);

       DeployEMRDto deployEMR =
               NodeReader.readNode(
                       Paths.get(PropertiesResolver.getClusterConfFileLocation(), "EMR.json").toString(),
                       DeployEMRDto.class);

       //TODO: add parameter for switching from regular ec2 instances to spot instances
       /*DeployEMRDto deployEMRSpot40 =
               NodeReader.readNode(
                       Paths.get(PropertiesResolver.getClusterConfFileLocation(), "EMR_spot.json").toString(),
                       DeployEMRDto.class);*/

       deployEMR.setName(emrName);
       deployEMR.setNotebook_name(testNoteBookName);
       LOGGER.info("{}: EMR = {}",testNoteBookName, deployEMR);
       Response responseDeployingEMR = new HttpRequest().webApiPut(ssnCompResURL, ContentType.JSON,
               deployEMR, token);
       LOGGER.info("{}:   responseDeployingEMR.getBody() is {}",testNoteBookName, responseDeployingEMR.getBody().asString());
       Assert.assertEquals(responseDeployingEMR.statusCode(), HttpStatusCode.OK, "EMR  " + emrName + " was not deployed");

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, testNoteBookName, emrName, "creating", ConfigPropertyValue.getTimeoutEMRCreate());
       if(!ConfigPropertyValue.isRunModeLocal()) {
           if (!(gettingStatus.contains("configuring") || gettingStatus.contains("running")))
               throw new Exception(testNoteBookName + ": EMR " + emrName + " has not been deployed. EMR status is " + gettingStatus);
           LOGGER.info("{}: EMR {} has been deployed", testNoteBookName, emrName);

           AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrName, AmazonInstanceState.RUNNING.value());
           Docker.checkDockerStatus(nodePrefix + "_create_computational_EMRAutoTest", ssnIp);
       }
       LOGGER.info("{}:   Waiting until EMR has been configured ...", testNoteBookName);

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, testNoteBookName, emrName, "configuring", ConfigPropertyValue.getTimeoutEMRCreate());
       if (!gettingStatus.contains("running"))
           throw new Exception(testNoteBookName + ": EMR " + emrName + " has not been configured. EMR status is " + gettingStatus);
       LOGGER.info(" {}:  EMR {} has been configured", testNoteBookName, emrName);

       if(!ConfigPropertyValue.isRunModeLocal()) {
           AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrName, AmazonInstanceState.RUNNING.value());
           Docker.checkDockerStatus(nodePrefix + "_create_computational_EMRAutoTest", ssnIp);
       }

       LOGGER.info("{}:   Check bucket {}", testNoteBookName, bucketName);
       AmazonHelper.printBucketGrants(bucketName);

       return deployEMR;
   }
   
   private void restartNotebookAndRedeployToTerminate(String testNoteBookName, String ssnExpEnvURL, String ssnProUserResURL, String ssnCompResURL, String amazonNodePrefix,
           String nodePrefix, String emrName, String token,
           DeployEMRDto deployEMR) throws Exception {
	   restartNotebook(testNoteBookName, ssnExpEnvURL, ssnProUserResURL, amazonNodePrefix, nodePrefix, token);
	   final String emrNewName = redeployEMR(testNoteBookName, ssnProUserResURL, ssnCompResURL, amazonNodePrefix, nodePrefix, emrName, token, deployEMR);

	   terminateEMR(testNoteBookName, ssnProUserResURL, amazonNodePrefix, nodePrefix, token, emrNewName);
   }

   private void restartNotebook(String testNoteBookName, String ssnExpEnvURL, String ssnProUserResURL, String amazonNodePrefix, String nodePrefix, String token) throws Exception {
       LOGGER.info("9. Notebook will be re-started ...");
       String myJs = "{\"notebook_instance_name\":\"" + testNoteBookName + "\"}";
       Response respStartNotebook = new HttpRequest().webApiPost(ssnExpEnvURL, ContentType.JSON,
                                                                 myJs, token);
       LOGGER.info("    respStartNotebook.getBody() is {}", respStartNotebook.getBody().asString());
       Assert.assertEquals(respStartNotebook.statusCode(), HttpStatusCode.OK);

       String gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, testNoteBookName, "starting", ConfigPropertyValue.getTimeoutNotebookStartup());
       if (!gettingStatus.contains(AmazonInstanceState.RUNNING.value())){
           throw new Exception("Notebook " + testNoteBookName + " has not been started. Notebook status is " + gettingStatus);
       }
       LOGGER.info("    Notebook {} has been started", testNoteBookName);

       AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-nb-" + testNoteBookName, AmazonInstanceState.RUNNING.value());
       Docker.checkDockerStatus(nodePrefix + "_start_exploratory_NotebookAutoTest", ssnIp);
   }

   private void terminateNotebook(String testNoteBookName, String ssnProUserResURL, String amazonNodePrefix, String nodePrefix, DeployEMRDto deployEmr, String token) throws Exception {
       String gettingStatus;
       LOGGER.info("12. Notebook will be terminated ...");
       final String ssnTerminateNotebookURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.getTerminateNotebookUrl(testNoteBookName));
       Response respTerminateNotebook = new HttpRequest().webApiDelete(ssnTerminateNotebookURL, ContentType.JSON, token);
       LOGGER.info("    respTerminateNotebook.getBody() is {}", respTerminateNotebook.getBody().asString());
       Assert.assertEquals(respTerminateNotebook.statusCode(), HttpStatusCode.OK);

       gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, testNoteBookName, "terminating", ConfigPropertyValue.getTimeoutEMRTerminate());
       if (!gettingStatus.contains("terminated"))
           throw new Exception("Notebook" + testNoteBookName + " has not been terminated. Notebook status is " + gettingStatus);

       gettingStatus = WaitForStatus.getEmrStatus(
				new HttpRequest()
					.webApiGet(ssnProUserResURL, token)
					.getBody()
					.jsonPath(),
				testNoteBookName, deployEmr.getName());
       if (!gettingStatus.contains("terminated"))
           throw new Exception("EMR has not been terminated for Notebook " + testNoteBookName + ". EMR status is " + gettingStatus);
       LOGGER.info("    EMR has been terminated for Notebook {}", testNoteBookName);

       AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-nb-" + testNoteBookName, AmazonInstanceState.TERMINATED.value());
       AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + deployEmr, AmazonInstanceState.TERMINATED.value());

       Docker.checkDockerStatus(nodePrefix + "_terminate_exploratory_NotebookAutoTestt", ssnIp);
   }

   private void terminateEMR(String testNoteBookName, String ssnProUserResURL, String amazonNodePrefix, String nodePrefix, String token, String emrNewName) throws Exception {
       String gettingStatus;
       LOGGER.info("    New EMR will be terminated ...");
       final String ssnTerminateEMRURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.getTerminateEMRUrl(testNoteBookName, emrNewName));
       LOGGER.info("    SSN terminate EMR URL is {}", ssnTerminateEMRURL);

       Response respTerminateEMR = new HttpRequest().webApiDelete(ssnTerminateEMRURL,
                                                                  ContentType.JSON, token);
       LOGGER.info("    respTerminateEMR.getBody() is {}", respTerminateEMR.getBody().asString());
       Assert.assertEquals(respTerminateEMR.statusCode(), HttpStatusCode.OK);

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, testNoteBookName, emrNewName, "terminating", ConfigPropertyValue.getTimeoutEMRTerminate());
       if (!gettingStatus.contains("terminated"))
           throw new Exception("New EMR " + emrNewName + " has not been terminated. EMR status is " + gettingStatus);
       LOGGER.info("    New EMR {} has been terminated", emrNewName);

       AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrNewName, AmazonInstanceState.TERMINATED.value());
       Docker.checkDockerStatus(nodePrefix + "_terminate_computational_NewEMRAutoTest", ssnIp);
   }

   private String redeployEMR(String testNoteBookName, String ssnProUserResURL, String ssnCompResURL, String amazonNodePrefix, String nodePrefix, String emrName, String token, DeployEMRDto deployEMR) throws Exception {
       String gettingStatus;

       LOGGER.info("10. New EMR will be deployed for termination ...");

       final String emrNewName = "New" + emrName;
       deployEMR.setName(emrNewName);
       deployEMR.setNotebook_name(testNoteBookName);
       Response responseDeployingEMRNew = new HttpRequest().webApiPut(ssnCompResURL,
                                                                      ContentType.JSON, deployEMR, token);
       LOGGER.info("    responseDeployingEMRNew.getBody() is {}", responseDeployingEMRNew.getBody().asString());
       Assert.assertEquals(responseDeployingEMRNew.statusCode(), HttpStatusCode.OK);

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, testNoteBookName, emrNewName, "creating", ConfigPropertyValue.getTimeoutEMRCreate());
       if (!(gettingStatus.contains("configuring") || gettingStatus.contains("running")))
           throw new Exception("New EMR " + emrNewName + " has not been deployed. EMR status is " + gettingStatus);
       LOGGER.info("    New EMR {} has been deployed", emrNewName);

       LOGGER.info("   Waiting until EMR has been configured ...");
       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, testNoteBookName, emrNewName, "configuring", ConfigPropertyValue.getTimeoutEMRCreate());
       if (!gettingStatus.contains(AmazonInstanceState.RUNNING.value()))
           throw new Exception("EMR " + emrNewName + " has not been configured. EMR status is " + gettingStatus);
       LOGGER.info("   EMR {} has been configured", emrNewName);

       AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrNewName, AmazonInstanceState.RUNNING.value());
       Docker.checkDockerStatus(nodePrefix + "_create_computational_EMRAutoTest", ssnIp);
       return emrNewName;
   }
   
   private void stopEnvironment(String nodePrefix, String amazonNodePrefix, String token, String ssnProUserResURL, String notebook, String testNoteBookName, String emrName) throws Exception {
       String gettingStatus;
       LOGGER.info("8. Notebook " + notebook + " will be stopped ...");
       final String ssnStopNotebookURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.getStopNotebookUrl(testNoteBookName));
       LOGGER.info("   SSN stop notebook URL is {}", ssnStopNotebookURL);

       Response responseStopNotebook = new HttpRequest().webApiDelete(ssnStopNotebookURL,
               ContentType.JSON, token);
       LOGGER.info("   responseStopNotebook.getBody() is {}", responseStopNotebook.getBody().asString());
       Assert.assertEquals(responseStopNotebook.statusCode(), HttpStatusCode.OK, "Notebook " + notebook + " was not stopped");

       gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, testNoteBookName, "stopping", ConfigPropertyValue.getTimeoutNotebookShutdown());
       if (!gettingStatus.contains("stopped"))
           throw new Exception("Notebook " + testNoteBookName + " has not been stopped. Notebook status is " + gettingStatus);
       LOGGER.info("   Notebook {} has been stopped", testNoteBookName);
       gettingStatus = WaitForStatus.getEmrStatus(
               new HttpRequest()
                       .webApiGet(ssnProUserResURL, token)
                       .getBody()
                       .jsonPath(),
               testNoteBookName, emrName);

       if (!gettingStatus.contains("terminated"))
           throw new Exception("Computational resources has not been terminated for Notebook " + testNoteBookName + ". EMR status is " + gettingStatus);
       LOGGER.info("   Computational resources has been terminated for Notebook {}", testNoteBookName);

       AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrName, AmazonInstanceState.TERMINATED.value());
       Docker.checkDockerStatus(nodePrefix + "_stop_exploratory_NotebookAutoTest", ssnIp);
   }
}
