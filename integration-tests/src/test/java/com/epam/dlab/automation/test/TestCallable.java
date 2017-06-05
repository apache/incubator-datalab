package com.epam.dlab.automation.test;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

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
import com.epam.dlab.automation.model.JsonMapperDto;
import com.jayway.restassured.response.Response;

public class TestCallable implements Callable<Boolean> {
    private final static Logger LOGGER = LogManager.getLogger(TestCallable.class);
    
    private final String notebookTemplate;
    private final boolean testRestartStopped;
    private final String token, ssnExpEnvURL, ssnProUserResURL;
    private final String bucketName;
    private final String notebookName, emrName;
    
    public TestCallable(String notebookTemplate, boolean testRestartStopped) {
    	this.notebookTemplate = notebookTemplate;
        this.testRestartStopped = testRestartStopped;
        
        this.token = NamingHelper.getSsnToken();
        this.ssnExpEnvURL = NamingHelper.getSelfServiceURL(ApiPath.EXP_ENVIRONMENT);
        this.ssnProUserResURL = NamingHelper.getSelfServiceURL(ApiPath.PROVISIONED_RES);
        this.bucketName = NamingHelper.getBucketName();

        final String suffixName = NamingHelper.generateRandomValue(notebookTemplate);
        notebookName = "Notebook" + suffixName;
        emrName = "eimr" + suffixName;

        LOGGER.info("   SSN exploratory environment URL is {}", ssnExpEnvURL);
        LOGGER.info("   SSN provisioned user resources URL is {}", ssnProUserResURL);
    }

    @Override
    public Boolean call() throws Exception {
        final String notebookIp = createNotebook();
        final DeployEMRDto deployEMR = createEMR();

        final String emrClusterName = NamingHelper.getEmrClusterName(NamingHelper.getEmrInstanceName(notebookName, emrName));
        if (!ConfigPropertyValue.isRunModeLocal()) {
        	TestEmr test = new TestEmr();
        	test.run(notebookName, emrClusterName);

            String notebookFilesLocation = PropertiesResolver.getPropertyByName(String.format(PropertiesResolver.NOTEBOOK_FILES_LOCATION_PROPERTY_TEMPLATE, notebookTemplate));
            test.run2(NamingHelper.getSsnIp(), notebookIp, emrClusterName, new File(notebookFilesLocation), notebookName);
        }

        stopEnvironment();

        if (testRestartStopped) {
        	restartNotebookAndRedeployToTerminate(deployEMR);
        }
        if (deployEMR != null) {
        	terminateNotebook(deployEMR);
        }

        return true;
    }

    /**
    * @param notebookName
    * @throws Exception
    */
   private String  createNotebook() throws Exception {
       LOGGER.info("6. Notebook " +  notebookName + " will be created ...");
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

       AmazonHelper.checkAmazonStatus(NamingHelper.getNotebookInstanceName(notebookName), AmazonInstanceState.RUNNING.value());
       // TODO get helper method for container name and use notebookName
       /* Name for exploratory user String.join("_", ConfigPropertyValue.getUsernameSimple(), action, "exploratory", notebookName, timestamp)*/
       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "create"), NamingHelper.getSsnIp());

       LOGGER.info("   Notebook {} status has been verified", notebookName);
       //get notebook IP
       String notebookIp = AmazonHelper.getInstance(NamingHelper.getNotebookInstanceName(notebookName))
    		   .getPrivateIpAddress();
       LOGGER.info("   Notebook {} IP is {}", notebookName, notebookIp);

       return notebookIp;
   }
   
   private DeployEMRDto createEMR() throws Exception {
       String gettingStatus;
       LOGGER.info("7. EMR will be deployed {} ...", notebookName);
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

           AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.RUNNING.value());
           Docker.checkDockerStatus(NamingHelper.getEmrContainerName(emrName, "create"), NamingHelper.getSsnIp());
       }
       LOGGER.info("{}:   Waiting until EMR has been configured ...", notebookName);

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrName, "configuring", ConfigPropertyValue.getTimeoutEMRCreate());
       if (!gettingStatus.contains("running"))
           throw new Exception(notebookName + ": EMR " + emrName + " has not been configured. EMR status is " + gettingStatus);
       LOGGER.info(" {}:  EMR {} has been configured", notebookName, emrName);

       if(!ConfigPropertyValue.isRunModeLocal()) {
           AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.RUNNING.value());
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

   private void restartNotebook() throws Exception {
       LOGGER.info("9. Notebook will be re-started ...");
       String myJs = "{\"notebook_instance_name\":\"" + notebookName + "\"}";
       Response respStartNotebook = new HttpRequest().webApiPost(ssnExpEnvURL, ContentType.JSON,
                                                                 myJs, token);
       LOGGER.info("    respStartNotebook.getBody() is {}", respStartNotebook.getBody().asString());
       Assert.assertEquals(respStartNotebook.statusCode(), HttpStatusCode.OK);

       String gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, notebookName, "starting", ConfigPropertyValue.getTimeoutNotebookStartup());
       if (!gettingStatus.contains(AmazonInstanceState.RUNNING.value())){
           throw new Exception("Notebook " + notebookName + " has not been started. Notebook status is " + gettingStatus);
       }
       LOGGER.info("    Notebook {} has been started", notebookName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getNotebookInstanceName(notebookName), AmazonInstanceState.RUNNING.value());
       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "start"), NamingHelper.getSsnIp());
   }

   private void terminateNotebook(DeployEMRDto deployEmr) throws Exception {
       String gettingStatus;
       LOGGER.info("12. Notebook will be terminated ...");
       final String ssnTerminateNotebookURL = NamingHelper.getSelfServiceURL(ApiPath.getTerminateNotebookUrl(notebookName));
       Response respTerminateNotebook = new HttpRequest().webApiDelete(ssnTerminateNotebookURL, ContentType.JSON, token);
       LOGGER.info("    respTerminateNotebook.getBody() is {}", respTerminateNotebook.getBody().asString());
       Assert.assertEquals(respTerminateNotebook.statusCode(), HttpStatusCode.OK);

       gettingStatus = WaitForStatus.notebook(ssnProUserResURL, token, notebookName, "terminating", ConfigPropertyValue.getTimeoutEMRTerminate());
       if (!gettingStatus.contains("terminated"))
           throw new Exception("Notebook" + notebookName + " has not been terminated. Notebook status is " + gettingStatus);

       gettingStatus = WaitForStatus.getEmrStatus(
				new HttpRequest()
					.webApiGet(ssnProUserResURL, token)
					.getBody()
					.jsonPath(),
				notebookName, deployEmr.getName());
       if (!gettingStatus.contains("terminated"))
           throw new Exception("EMR has not been terminated for Notebook " + notebookName + ". EMR status is " + gettingStatus);
       LOGGER.info("    EMR has been terminated for Notebook {}", notebookName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getNotebookInstanceName(notebookName), AmazonInstanceState.TERMINATED.value());
       AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.TERMINATED.value());

       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "terminate"), NamingHelper.getSsnIp());
   }

   private void terminateEMR(String emrNewName) throws Exception {
       String gettingStatus;
       LOGGER.info("    New EMR will be terminated ...");
       final String ssnTerminateEMRURL = NamingHelper.getSelfServiceURL(ApiPath.getTerminateEMRUrl(notebookName, emrNewName));
       LOGGER.info("    SSN terminate EMR URL is {}", ssnTerminateEMRURL);

       Response respTerminateEMR = new HttpRequest().webApiDelete(ssnTerminateEMRURL,
                                                                  ContentType.JSON, token);
       LOGGER.info("    respTerminateEMR.getBody() is {}", respTerminateEMR.getBody().asString());
       Assert.assertEquals(respTerminateEMR.statusCode(), HttpStatusCode.OK);

       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrNewName, "terminating", ConfigPropertyValue.getTimeoutEMRTerminate());
       if (!gettingStatus.contains("terminated"))
           throw new Exception("New EMR " + emrNewName + " has not been terminated. EMR status is " + gettingStatus);
       LOGGER.info("    New EMR {} has been terminated", emrNewName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.TERMINATED.value());
       Docker.checkDockerStatus(NamingHelper.getEmrContainerName(emrName, "terminate"), NamingHelper.getSsnIp());
   }

   private String redeployEMR(DeployEMRDto deployEMR) throws Exception {
       String gettingStatus;

       LOGGER.info("10. New EMR will be deployed for termination ...");

       final String emrNewName = "New" + emrName;
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

       LOGGER.info("   Waiting until EMR has been configured ...");
       gettingStatus = WaitForStatus.emr(ssnProUserResURL, token, notebookName, emrNewName, "configuring", ConfigPropertyValue.getTimeoutEMRCreate());
       if (!gettingStatus.contains(AmazonInstanceState.RUNNING.value()))
           throw new Exception("EMR " + emrNewName + " has not been configured. EMR status is " + gettingStatus);
       LOGGER.info("   EMR {} has been configured", emrNewName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.RUNNING.value());
       Docker.checkDockerStatus(NamingHelper.getEmrContainerName(emrName, "create"), NamingHelper.getSsnIp());
       return emrNewName;
   }
   
   private void stopEnvironment() throws Exception {
       String gettingStatus;
       LOGGER.info("8. Notebook " + notebookName + " will be stopped ...");
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
       LOGGER.info("   Computational resources has been terminated for Notebook {}", notebookName);

       AmazonHelper.checkAmazonStatus(NamingHelper.getEmrInstanceName(notebookName, emrName), AmazonInstanceState.TERMINATED.value());
       Docker.checkDockerStatus(NamingHelper.getNotebookContainerName(notebookName, "stop"), NamingHelper.getSsnIp());
   }
}
