package com.epam.dlab.automation.test;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.epam.dlab.automation.aws.AmazonHelper;
import com.epam.dlab.automation.aws.AmazonInstanceState;
import com.epam.dlab.automation.aws.NodeReader;
import com.epam.dlab.automation.docker.AckStatus;
import com.epam.dlab.automation.docker.Docker;
import com.epam.dlab.automation.docker.SSHConnect;
import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.epam.dlab.automation.helper.PropertiesResolver;
import com.epam.dlab.automation.helper.TestNamingHelper;
import com.epam.dlab.automation.http.HttpRequest;
import com.epam.dlab.automation.http.HttpStatusCode;
import com.epam.dlab.automation.jenkins.JenkinsService;
import com.epam.dlab.automation.model.CreateNotebookDto;
import com.epam.dlab.automation.model.DeployEMRDto;
import com.epam.dlab.automation.model.LoginDto;
import com.epam.dlab.automation.repository.ApiPath;
import com.epam.dlab.automation.repository.ContentType;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.jcraft.jsch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static org.testng.Assert.assertTrue;

@Test(singleThreaded=true)
public class TestServices {

    private final static Logger LOGGER = LogManager.getLogger(TestServices.class);
    private final static long SSN_REQUEST_TIMEOUT = 10000;
    // This time 3 notebooks are tested in parallel - so 3 threads are used, restartNotebookAndRedeployToTerminate are a pool for future notebooks grow.
    // needed to investigate Amazon behaviour when same AIM requests set of computation resources in parallel
    // looks like running test in 1 thread mostly succeeds, running in 2 and more threads - usually fails.
    public static final int N_THREADS = 10;

    private String serviceBaseName;
    private String ssnURL;
    private String publicSsnIp;
    private String privateSsnIp;

    private String ssnIpForTest;

    @BeforeClass
    public void Setup() throws InterruptedException {

        // Load properties
        ConfigPropertyValue.getJenkinsJobURL();
    }
    
    @AfterClass
    public void Cleanup() throws InterruptedException {
    }

    @Test(priority = 0)
    public void runJenkins() throws Exception {
        LOGGER.info("Test Started");
    	testJenkinsJob();
        LOGGER.info("Test Finished");
    }


    @Test(priority = 1, dependsOnMethods = "runJenkins")
    public void runSsnLogin() throws Exception {
        LOGGER.info("Test Started");
        testLoginSsnService();
        LOGGER.info("Test Finished");
    }


    @Test(priority = 2, dependsOnMethods = "runSsnLogin")
    public void runDLabScenario() throws Exception {
        LOGGER.info("Test Started");
        testDLabScenario();
        LOGGER.info("Test Finished");
    }

    private void testJenkinsJob() throws Exception {

        /* LOGGER.info("1. Jenkins Job will be started ...");
       
        JenkinsService jenkins = new JenkinsService(ConfigPropertyValue.getJenkinsUsername(), ConfigPropertyValue.getJenkinsPassword());
        String buildNumber = jenkins.runJenkinsJob(ConfigPropertyValue.getJenkinsJobURL());
        LOGGER.info("   Jenkins Job has been completed"); */

        LOGGER.info("1. Looking for last Jenkins Job ...");
        JenkinsService jenkins = new JenkinsService();
        String buildNumber = jenkins.getJenkinsJob();
        LOGGER.info("   Jenkins Job found:");
        LOGGER.info("Build number is: {}", buildNumber);
        
        ssnURL = jenkins.getSsnURL().replaceAll(" ", "");
        serviceBaseName = jenkins.getServiceBaseName().replaceAll(" ", "");
        Assert.assertNotNull(ssnURL, "Jenkins URL was not generated");
        Assert.assertNotNull(serviceBaseName, "Service BaseName was not generated");
        LOGGER.info("JenkinsURL is: " + ssnURL);
        LOGGER.info("ServiceBaseName is: " + serviceBaseName);

    }
    
    private void testLoginSsnService() throws Exception {
    	
    	//ssnURL = "http://ec2-35-162-89-115.us-west-2.compute.amazonaws.com";

        LOGGER.info("Check status of SSN node on Amazon: {}", serviceBaseName);
        Instance ssnInstance = AmazonHelper.getInstance(serviceBaseName + "-ssn");
        publicSsnIp = ssnInstance.getPublicIpAddress();
        LOGGER.info("Public IP is: {}", publicSsnIp);
        privateSsnIp = ssnInstance.getPrivateIpAddress();
        LOGGER.info("Private IP is: {}", privateSsnIp);
        ssnIpForTest = PropertiesResolver.DEV_MODE ? publicSsnIp : privateSsnIp;
        AmazonHelper.checkAmazonStatus(serviceBaseName + "-ssn", AmazonInstanceState.RUNNING.value());
        LOGGER.info("Amazon instance state is running");
        
        LOGGER.info("2. Waiting for SSN service ...");
        Assert.assertEquals(waitForSSNService(ConfigPropertyValue.getTimeoutNotebookCreate()), true, "SSN service was not started");
        LOGGER.info("   SSN service is available");
        
        
        LOGGER.info("3. Check login");
        final String ssnLoginURL = getSnnURL(ApiPath.LOGIN);
        LOGGER.info("   SSN login URL is {}", ssnLoginURL);
        
        if (!ConfigPropertyValue.isRunModeLocal()) {
        	LoginDto notIAMUserRequestBody = new LoginDto(ConfigPropertyValue.getNotIAMUsername(), ConfigPropertyValue.getNotIAMPassword(), "");
        	Response responseNotIAMUser = new HttpRequest().webApiPost(ssnLoginURL, ContentType.JSON, notIAMUserRequestBody);
        	Assert.assertEquals(responseNotIAMUser.statusCode(), HttpStatusCode.Unauthorized, "Unauthorized user");
        	Assert.assertEquals(responseNotIAMUser.getBody().asString(), "Please contact AWS administrator to create corresponding IAM User");
        }
 		
        LoginDto notDLABUserRequestBody = new LoginDto(ConfigPropertyValue.getNotDLabUsername(), ConfigPropertyValue.getNotDLabPassword(), "");
        Response responseNotDLABUser = new HttpRequest().webApiPost(ssnLoginURL, ContentType.JSON, notDLABUserRequestBody);
        Assert.assertEquals(responseNotDLABUser.statusCode(), HttpStatusCode.Unauthorized, "Unauthorized user");
        Assert.assertEquals(responseNotDLABUser.getBody().asString(), "Username or password are not valid");
        
        if (!ConfigPropertyValue.isRunModeLocal()) {
        	LoginDto forActivateAccessKey = new LoginDto(ConfigPropertyValue.getUsername(), ".", "");
        	Response responseForActivateAccessKey = new HttpRequest().webApiPost(ssnLoginURL, ContentType.JSON, forActivateAccessKey);
        	Assert.assertEquals(responseForActivateAccessKey.statusCode(), HttpStatusCode.Unauthorized, "Unauthorized user");
        	Assert.assertEquals(responseForActivateAccessKey.getBody().asString(), "Username or password are not valid");
        }
        
        LoginDto testUserLogin = new LoginDto(ConfigPropertyValue.getUsername(), ConfigPropertyValue.getPassword(), "");
        LOGGER.info("Logging in with credentials {}:{}", ConfigPropertyValue.getUsername(), ConfigPropertyValue.getPassword());
        Response responseTestUser = new HttpRequest().webApiPost(ssnLoginURL, ContentType.JSON, testUserLogin);
        Assert.assertEquals(responseTestUser.statusCode(), HttpStatusCode.OK, "User login + " + ConfigPropertyValue.getUsername() +" was not successful");
 		
        
        LOGGER.info("4. Check logout");
        final String ssnlogoutURL = getSnnURL(ApiPath.LOGOUT);
        LOGGER.info("   SSN logout URL is {}", ssnlogoutURL);
        
        Response responseLogout = new HttpRequest().webApiPost(ssnlogoutURL, ContentType.ANY);
        LOGGER.info("responseLogout.statusCode() is {}", responseLogout.statusCode());
        Assert.assertEquals(responseLogout.statusCode(), HttpStatusCode.Unauthorized, "User log out was not successful"/*Replace to HttpStatusCode.OK when EPMCBDCCSS-938 will be fixed and merged*/);
    }

    private void testDLabScenario() throws Exception {

    	//ssnURL = "http://ec2-35-164-76-52.us-west-2.compute.amazonaws.com";
        //serviceBaseName = "autotest_jan11";
        //publicSsnIp = "35.164.76.52";

        final String nodePrefix = ConfigPropertyValue.getUsernameSimple();
        final String amazonNodePrefix = serviceBaseName + "-" + nodePrefix;

        RestAssured.baseURI = ssnURL;
        String token = ssnLoginAndKeyUpload(nodePrefix, amazonNodePrefix);

        final String ssnExpEnvURL = getSnnURL(ApiPath.EXP_ENVIRONMENT);
        LOGGER.info("   SSN exploratory environment URL is {}", ssnExpEnvURL);
        final String ssnProUserResURL = getSnnURL(ApiPath.PROVISIONED_RES);
        LOGGER.info("   SSN provisioned user resources URL is {}", ssnProUserResURL);

        runTestsInNotebooks(nodePrefix, amazonNodePrefix, token, ssnExpEnvURL, ssnProUserResURL);

    }

    private String ssnLoginAndKeyUpload(String nodePrefix, String amazonNodePrefix) throws Exception {

        LoginDto testUserRequestBody = new LoginDto(ConfigPropertyValue.getUsername(), ConfigPropertyValue.getPassword(), "");

        LOGGER.info("5. Login as {} ...", ConfigPropertyValue.getUsername());

        final String ssnLoginURL = getSnnURL(ApiPath.LOGIN);
        LOGGER.info("   SSN login URL is {}", ssnLoginURL);

        final String ssnUploadKeyURL = getSnnURL(ApiPath.UPLOAD_KEY);
        LOGGER.info("   SSN upload key URL is {}", ssnUploadKeyURL);

        Response responseTestUser = new HttpRequest().webApiPost(ssnLoginURL, ContentType.JSON, testUserRequestBody);
        Assert.assertEquals(HttpStatusCode.OK, responseTestUser.getStatusCode(), "Failed to login");
        String token = responseTestUser.getBody().asString();
        LOGGER.info("   Logged in. Obtained token: {}", token);


        LOGGER.info("5.a Checking for user Key...");
        Response respCheckKey = new HttpRequest().webApiGet(ssnUploadKeyURL, token);

        if(respCheckKey.getStatusCode() == HttpStatusCode.NotFound) {
            LOGGER.info("5.b Upload Key will be started ...");

            Response respUploadKey = new HttpRequest().webApiPost(ssnUploadKeyURL, ContentType.FORMDATA, token);
            LOGGER.info("   respUploadKey.getBody() is {}", respUploadKey.getBody().asString());

            Assert.assertEquals(respUploadKey.statusCode(), HttpStatusCode.OK, "The key uploading was not successful");
            int responseCodeAccessKey = waitWhileUploadKeyStatus(ssnUploadKeyURL, token, HttpStatusCode.Accepted, ConfigPropertyValue.getTimeoutUploadKey());
            LOGGER.info("   Upload Key has been completed");
            LOGGER.info("responseAccessKey.statusCode() is {}", responseCodeAccessKey);
            Assert.assertEquals(responseCodeAccessKey, HttpStatusCode.OK, "The key uploading was not successful");
        } else if (respCheckKey.getStatusCode() == HttpStatusCode.OK){
            LOGGER.info("   Key has been uploaded already");
        } else {
            Assert.assertEquals(200, respCheckKey.getStatusCode(), "Failed to check User Key.");
        }

        Docker.checkDockerStatus(nodePrefix + "_create_edge_", ssnIpForTest);
        AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-edge", AmazonInstanceState.RUNNING.value());


        final String ssnExpEnvURL = getSnnURL(ApiPath.EXP_ENVIRONMENT);
        LOGGER.info("   SSN exploratory environment URL is {}", ssnExpEnvURL);
        final String ssnProUserResURL = getSnnURL(ApiPath.PROVISIONED_RES);
        LOGGER.info("   SSN provisioned user resources URL is {}", ssnProUserResURL);

        return token;
    }

    private void runTestsInNotebooks(String nodePrefix, String amazonNodePrefix, String token, String ssnExpEnvURL, String ssnProUserResURL) throws Exception {
        List<String> notebooks = Arrays.asList(ConfigPropertyValue.getNotebooks().split(","));
        LOGGER.info("Testing the following notebooks: {}", ConfigPropertyValue.getNotebooks());
        ExecutorService executor =  Executors.newFixedThreadPool(ConfigPropertyValue.getExecutionThreads() > 0 ? ConfigPropertyValue.getExecutionThreads(): N_THREADS);
        List<FutureTask<Boolean>> futureTasks = new ArrayList<>();

        boolean testRestartStopped = true;
        for (String notebook: notebooks) {
            FutureTask<Boolean> runScenarioTask = new FutureTask<>(new PythonTestCallable(notebook, nodePrefix, amazonNodePrefix, token, ssnExpEnvURL, ssnProUserResURL, testRestartStopped));
            testRestartStopped = false;
            futureTasks.add(runScenarioTask);
            executor.execute(runScenarioTask);
        }

        while (true) {
            boolean done = true;
            done = allScenariosDone(futureTasks);
            if (done) {
                verifyResults(futureTasks);
                executor.shutdown();
                return;
            } else {
                Thread.sleep(1000 * 60);
            }
        }
    }

    private void verifyResults(List<FutureTask<Boolean>> futureTasks) throws InterruptedException, ExecutionException {
        List<Exception> resExceptions = new ArrayList<>();
        for (FutureTask<Boolean> ft : futureTasks) {
            try {
                ft.get();
            } catch (Exception exception) {
                resExceptions.add(exception);
            }
        }

        if(resExceptions.size() > 0)  {
            for(Exception exception: resExceptions) {
                LOGGER.error("{} :\n {} ", exception, exception.getStackTrace());
                exception.printStackTrace();
            }
            assertTrue(false, "There were failed tests with one or more notebooks, see stacktrace above." );
        }
    }

    private boolean allScenariosDone(List<FutureTask<Boolean>> futureTasks) {
        boolean done = true;
        for (FutureTask<Boolean> ft : futureTasks) {
            if(!ft.isDone()) {
                done = ft.isDone();
            }
        }
        return done;
    }

    private void stopEnvironment(String nodePrefix, String amazonNodePrefix, String token, String ssnProUserResURL, String notebook, String testNoteBookName, String emrName) throws Exception {
        String gettingStatus;
        LOGGER.info("8. Notebook " + notebook + " will be stopped ...");
        final String ssnStopNotebookURL = getSnnURL(ApiPath.getStopNotebookUrl(testNoteBookName));
        LOGGER.info("   SSN stop notebook URL is {}", ssnStopNotebookURL);

        Response responseStopNotebook = new HttpRequest().webApiDelete(ssnStopNotebookURL,
                ContentType.JSON, token);
        LOGGER.info("   responseStopNotebook.getBody() is {}", responseStopNotebook.getBody().asString());
        Assert.assertEquals(responseStopNotebook.statusCode(), HttpStatusCode.OK, "Notebook " + notebook + " was not stopped");

        gettingStatus = waitWhileNotebookStatus(ssnProUserResURL, token, testNoteBookName, "stopping", ConfigPropertyValue.getTimeoutNotebookShutdown());
        if (!gettingStatus.contains("stopped"))
            throw new Exception("Notebook " + testNoteBookName + " has not been stopped. Notebook status is " + gettingStatus);
        LOGGER.info("   Notebook {} has been stopped", testNoteBookName);
        gettingStatus = getEmrStatus(
                new HttpRequest()
                        .webApiGet(ssnProUserResURL, token)
                        .getBody()
                        .jsonPath(),
                testNoteBookName, emrName);

        if (!gettingStatus.contains("terminated"))
            throw new Exception("Computational resources has not been terminated for Notebook " + testNoteBookName + ". EMR status is " + gettingStatus);
        LOGGER.info("   Computational resources has been terminated for Notebook {}", testNoteBookName);

        AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrName, AmazonInstanceState.TERMINATED.value());
        Docker.checkDockerStatus(nodePrefix + "_stop_exploratory_NotebookAutoTest", ssnIpForTest);
    }

    /**
     *
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

        String gettingStatus = waitWhileNotebookStatus(ssnProUserResURL, token, testNoteBookName, "creating", ConfigPropertyValue.getTimeoutNotebookCreate());
        if (!gettingStatus.contains("running")) {
            LOGGER.error("Notebook {} is in state {}", testNoteBookName, gettingStatus);
            throw new Exception("Notebook " + testNoteBookName + " has not been created. Notebook status is " + gettingStatus);
        }
        LOGGER.info("   Notebook {} has been created", testNoteBookName);

        AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-nb-" + testNoteBookName, AmazonInstanceState.RUNNING.value());
        Docker.checkDockerStatus(nodePrefix + "_create_exploratory_NotebookAutoTest", ssnIpForTest);

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
        final String ssnCompResURL = getSnnURL(ApiPath.COMPUTATIONAL_RES);
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

        gettingStatus = waitWhileEmrStatus(ssnProUserResURL, token, testNoteBookName, emrName, "creating", ConfigPropertyValue.getTimeoutEMRCreate());
        if(!ConfigPropertyValue.isRunModeLocal()) {
            if (!(gettingStatus.contains("configuring") || gettingStatus.contains("running")))
                throw new Exception(testNoteBookName + ": EMR " + emrName + " has not been deployed. EMR status is " + gettingStatus);
            LOGGER.info("{}: EMR {} has been deployed", testNoteBookName, emrName);

            AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrName, AmazonInstanceState.RUNNING.value());
            Docker.checkDockerStatus(nodePrefix + "_create_computational_EMRAutoTest", ssnIpForTest);
        }
        LOGGER.info("{}:   Waiting until EMR has been configured ...", testNoteBookName);

        gettingStatus = waitWhileEmrStatus(ssnProUserResURL, token, testNoteBookName, emrName, "configuring", ConfigPropertyValue.getTimeoutEMRCreate());
        if (!gettingStatus.contains("running"))
            throw new Exception(testNoteBookName + ": EMR " + emrName + " has not been configured. EMR status is " + gettingStatus);
        LOGGER.info(" {}:  EMR {} has been configured", testNoteBookName, emrName);

        if(!ConfigPropertyValue.isRunModeLocal()) {
            AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrName, AmazonInstanceState.RUNNING.value());
            Docker.checkDockerStatus(nodePrefix + "_create_computational_EMRAutoTest", ssnIpForTest);
        }

        LOGGER.info("{}:   Check bucket {}", testNoteBookName, getBucketName());
        AmazonHelper.printBucketGrants(getBucketName());

        return deployEMR;
    }

    private void copyTestDataIntoTestBucket(String emrName, String clusterName, String notebook) throws Exception {
        Session ssnSession = null;
        ChannelSftp channelSftp = null;
        try {
            LOGGER.info("{}: Copying test data copy scripts {} to SSN {}...", notebook, ConfigPropertyValue.getS3TestsTemplateBucketName(), ssnIpForTest);
            ssnSession = SSHConnect.getSession(ConfigPropertyValue.getClusterOsUser(), ssnIpForTest, 22);
            channelSftp = SSHConnect.getChannelSftp(ssnSession);

            copyFileToSSN(channelSftp, PropertiesResolver.getNotebookTestDataCopyScriptLocation());

            executePythonScript2(ssnSession, clusterName, new File(PropertiesResolver.getNotebookTestDataCopyScriptLocation()).getName(), notebook);

        }  finally {
        if(channelSftp != null && !channelSftp.isConnected()) {
            channelSftp.disconnect();
        }
        if(ssnSession != null && !ssnSession.isConnected()) {
            ssnSession.disconnect();
        }
    }
    }

    private String getBucketName() {
    	return String.format("%s-%s-bucket", serviceBaseName, ConfigPropertyValue.getUsernameSimple()).replace('_', '-').toLowerCase();
    }
    
    private String getEmrClusterName(String emrName) throws Exception {
        Instance instance = AmazonHelper.getInstance(emrName);
        for (Tag tag : instance.getTags()) {
			if (tag.getKey().equals("Name")) {
		        return tag.getValue();
			}
		}
        throw new Exception("Could not detect cluster name for EMR " + emrName);
    }

    private void copyFileToSSN(ChannelSftp channel, String filenameWithPath) throws IOException, InterruptedException, JSchException {
        LOGGER.info("Copying {}...", filenameWithPath);
        File file = new File(filenameWithPath);
        assertTrue(file.exists());

        FileInputStream src = new FileInputStream(file);

        try {

            channel.put(src, String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), file.getName()));

        } catch (SftpException e) {
            LOGGER.error(e);
            assertTrue(false);
        }
    }
    
    private void copyFileToNotebook(Session session, String filename, String ip) throws JSchException, IOException, InterruptedException {

    	String command = String.format(ScpCommands.copyToNotebookCommand,
    			"keys/"+ Paths.get(ConfigPropertyValue.getAccessKeyPrivFileName()).getFileName().toString(),
                filename,
                ConfigPropertyValue.getClusterOsUser(),
                ip);

    	LOGGER.info("Copying {}...", filename);
    	LOGGER.info("  Run command: {}", command);

        ChannelExec copyResult = SSHConnect.setCommand(session, command);
        AckStatus status = SSHConnect.checkAck(copyResult);

        LOGGER.info("Copied {}: {}", filename, status.toString());
        assertTrue(status.isOk());
    }

    private void testPythonScripts(String ssnIP, String noteBookIp, String clusterName, File notebookDirectory, String notebook)
            throws JSchException, IOException, InterruptedException {
    	LOGGER.info("Python tests for {} will be started ...", notebookDirectory);
    	if (ConfigPropertyValue.isRunModeLocal()) {
    		LOGGER.info("  tests are skipped");
    		return;
    	}

        assertTrue(notebookDirectory.exists(), notebook + ": Checking notebook directory " + notebookDirectory);
        assertTrue(notebookDirectory.isDirectory());

    	String [] files = notebookDirectory.list();
    	assertTrue(files.length == 1, "The python script location " + notebookDirectory + " found more more then 1 file, expected 1 *.py file, but found multiple files: " + Arrays.toString(files));
        assertTrue(files[0].endsWith(".py"), "The python script was not found");
        // it is assumed there should be 1 python file.
        String notebookTestFile = files[0];

        Session ssnSession = null;
        ChannelSftp channelSftp = null;
        try {
            LOGGER.info("{}: Copying files to SSN {}...", notebook, ssnIP);
            ssnSession = SSHConnect.getSession(ConfigPropertyValue.getClusterOsUser(), ssnIP, 22);
            channelSftp = SSHConnect.getChannelSftp(ssnSession);

            copyFileToSSN(channelSftp, Paths.get(notebookDirectory.getAbsolutePath(), notebookTestFile).toString());
        } finally {
            if(channelSftp != null && !channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }

        LOGGER.info("{}: Copying files to Notebook {}...", notebook, noteBookIp);

        try {
            for (String filename : files) {
            	copyFileToNotebook(ssnSession, filename, noteBookIp);
    		}

            LOGGER.info("{}: Port forwarding from ssn {} to notebook {}...", notebook, ssnIP, noteBookIp);
            int assignedPort = ssnSession.setPortForwardingL(0, noteBookIp, 22);
            LOGGER.info("{}: Port forwarded localhost:{} -> {}:22", notebook, assignedPort, noteBookIp);

            executePythonScript(noteBookIp, clusterName, notebookTestFile, assignedPort, notebook);
        }
        finally {
            if(ssnSession != null && ssnSession.isConnected()) {
                LOGGER.info("{}: Closing ssn session", notebook);
                ssnSession.disconnect();
            }
        }
    }

    private void executePythonScript(String Ip, String cluster_name, String notebookTestFile, int assignedPort, String notebook) throws JSchException, IOException, InterruptedException {
        String command;
        AckStatus status;
        Session session = SSHConnect.getForwardedConnect(ConfigPropertyValue.getClusterOsUser(), Ip, assignedPort);

        try {
            command = String.format(ScpCommands.runPythonCommand,
                    "/tmp/" +  notebookTestFile,
                    getBucketName(),
                    cluster_name,
                    ConfigPropertyValue.getClusterOsUser());
            LOGGER.info(String.format("{}: Executing command %s...", command), notebook);

            ChannelExec runScript = SSHConnect.setCommand(session, command);
            status = SSHConnect.checkAck(runScript);
            LOGGER.info("{}: Script execution status message {} and status code {}", notebook, status.getMessage(), status.getStatus());
            assertTrue(status.isOk(), notebook+ ": The python script execution wasn`t successful on " + cluster_name);

            LOGGER.info("{}: Python script executed successfully ", notebook);
        }
        finally {
            if(session != null && session.isConnected()) {
                LOGGER.info("{}: Closing notebook session", notebook);
                session.disconnect();
            }
        }
    }

    private void executePythonScript2(Session ssnSession, String clusterName, String notebookTestFile, String notebook) throws JSchException, IOException, InterruptedException {
        String command;
        AckStatus status;

            command = String.format(ScpCommands.runPythonCommand2,
                    String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), notebookTestFile),
                    getBucketName(), ConfigPropertyValue.getS3TestsTemplateBucketName());
            LOGGER.info("{}: Executing command {}...",notebook, command);

            ChannelExec runScript = SSHConnect.setCommand(ssnSession, command);
            status = SSHConnect.checkAck(runScript);
            LOGGER.info("{}: Script execution status message {} and code {}", notebook, status.getMessage(), status.getStatus());
            assertTrue(status.isOk(), notebook + ": The python script execution wasn`t successful on : " + clusterName);

            LOGGER.info("{}: Python script executed successfully ", notebook);
    }

    private String getSnnURL(String path) {
        return ssnURL + path;
    }

    private boolean waitForSSNService(Duration duration) throws InterruptedException {
        HttpRequest request = new HttpRequest();
        int actualStatus;
        long timeout = duration.toMillis();
        long expiredTime = System.currentTimeMillis() + timeout;

        while ((actualStatus = request.webApiGet(ssnURL, ContentType.TEXT).statusCode()) != HttpStatusCode.OK) {
            if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
                break;
            }
            Thread.sleep(SSN_REQUEST_TIMEOUT);
        }

        if (actualStatus != HttpStatusCode.OK) {
            LOGGER.info("ERROR: Timeout has been expired for SSN available. Timeout was {}", duration);
            return false;
        } else {
    		LOGGER.info("Current status code for SSN is {}", actualStatus);
    	}
        
        return true;
    }

    private int waitWhileUploadKeyStatus(String url, String token, int status, Duration duration)
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
            Thread.sleep(SSN_REQUEST_TIMEOUT);
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

    private String getNotebookStatus(JsonPath json, String notebookName) {
    	List<Map<String, String>> notebooks = json
				.param("name", notebookName)
				.getList("findAll { notebook -> notebook.exploratory_name == name }");
        if (notebooks == null || notebooks.size() != 1) {
        	return "";
        }
        Map<String, String> notebook = notebooks.get(0);
        String status = notebook.get("status");
        return (status == null ? "" : status);
    }

    private String waitWhileNotebookStatus(String url, String token, String notebookName, String status, Duration duration)
            throws InterruptedException {
    	LOGGER.info("Waiting until status {} with URL {} with token {} for notebook {}",status, url, token, notebookName);
        HttpRequest request = new HttpRequest();
        String actualStatus;
        long timeout = duration.toMillis();
        long expiredTime = System.currentTimeMillis() + timeout;

        while ((actualStatus = getNotebookStatus(request.webApiGet(url, token)
        											.getBody()
        											.jsonPath(), notebookName)).equals(status)) {
            if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
                break;
            }
            Thread.sleep(SSN_REQUEST_TIMEOUT);
        }

        if (actualStatus.contains(status)) {
            LOGGER.info("ERROR: {}: Timeout has been expired for request.", notebookName);
            LOGGER.info("  {}: URL is {}", notebookName, url);
            LOGGER.info("  {}: token is {}", notebookName, token);
            LOGGER.info("  {}: status is {}", notebookName, status);
            LOGGER.info("  {}: timeout is {}", notebookName, duration);
        } else {
        	LOGGER.info("{}: Current state for Notebook {} is {}", notebookName, notebookName, actualStatus );
        }
        
        return actualStatus;
    }
    
    private String getEmrStatus(JsonPath json, String notebookName, String computationalName) {
    	List<Map<String, List<Map<String, String>>>> notebooks = json
				.param("name", notebookName)
				.getList("findAll { notebook -> notebook.exploratory_name == name }");
        if (notebooks == null || notebooks.size() != 1) {
        	return "";
        }
        List<Map<String, String>> resources = notebooks.get(0)
        		.get("computational_resources");
        for (Map<String, String> resource : resources) {
            String comp = resource.get("computational_name");
            if (comp != null && comp.equals(computationalName)) {
            	return resource.get("status");
            }
		}
		return "";
    }
    
    private String waitWhileEmrStatus(String url, String token, String notebookName, String computationalName, String status, Duration duration)
            throws InterruptedException {
    	LOGGER.info("{}: Waiting until status {} with URL {} with token {} for computational {} on notebook ", notebookName, status, url, token, computationalName, notebookName);
        HttpRequest request = new HttpRequest();
        String actualStatus;
        long timeout = duration.toMillis();
        long expiredTime = System.currentTimeMillis() + timeout;

        while ((actualStatus = getEmrStatus(request.webApiGet(url, token)
        											.getBody()
        											.jsonPath(), notebookName, computationalName)).equals(status)) {
            if (timeout != 0 && expiredTime < System.currentTimeMillis()) {
                break;
            }
            Thread.sleep(SSN_REQUEST_TIMEOUT);
        }

        if (actualStatus.contains(status)) {
            LOGGER.info("ERROR: Timeout has been expired for request.");
            LOGGER.info("  URL is {}",  url);
            LOGGER.info("  token is {}", token);
            LOGGER.info("  status is {}", status);
            LOGGER.info("  timeout is {}", duration);
        } else {
        	LOGGER.info("{}: Current state for EMR {} on notebook {} is ", notebookName, computationalName, notebookName, actualStatus);
        }
        
        return actualStatus;
    }

    private void restartNotebookAndRedeployToTerminate(String testNoteBookName, String ssnExpEnvURL, String ssnProUserResURL, String ssnCompResURL, String amazonNodePrefix,
                                                       String nodePrefix, String emrName, String token,
                                                       DeployEMRDto deployEMR) throws Exception {
        restartNotebook(testNoteBookName, ssnExpEnvURL, ssnProUserResURL, amazonNodePrefix, nodePrefix, token);
        final String emrNewName = redeployEMR(testNoteBookName, ssnProUserResURL, ssnCompResURL, amazonNodePrefix, nodePrefix, emrName, token, deployEMR);

        terminateEMR(testNoteBookName, ssnProUserResURL, amazonNodePrefix, nodePrefix, token, emrNewName);
    }

    private void terminateNotebook(String testNoteBookName, String ssnProUserResURL, String amazonNodePrefix, String nodePrefix, DeployEMRDto deployEmr, String token) throws Exception {
        String gettingStatus;
        LOGGER.info("12. Notebook will be terminated ...");
        final String ssnTerminateNotebookURL = getSnnURL(ApiPath.getTerminateNotebookUrl(testNoteBookName));
        Response respTerminateNotebook = new HttpRequest().webApiDelete(ssnTerminateNotebookURL, ContentType.JSON, token);
        LOGGER.info("    respTerminateNotebook.getBody() is {}", respTerminateNotebook.getBody().asString());
        Assert.assertEquals(respTerminateNotebook.statusCode(), HttpStatusCode.OK);

        gettingStatus = waitWhileNotebookStatus(ssnProUserResURL, token, testNoteBookName, "terminating", ConfigPropertyValue.getTimeoutEMRTerminate());
        if (!gettingStatus.contains("terminated"))
            throw new Exception("Notebook" + testNoteBookName + " has not been terminated. Notebook status is " + gettingStatus);

        gettingStatus = getEmrStatus(
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

        Docker.checkDockerStatus(nodePrefix + "_terminate_exploratory_NotebookAutoTestt", ssnIpForTest);
    }

    private void terminateEMR(String testNoteBookName, String ssnProUserResURL, String amazonNodePrefix, String nodePrefix, String token, String emrNewName) throws Exception {
        String gettingStatus;
        LOGGER.info("    New EMR will be terminated ...");
        final String ssnTerminateEMRURL = getSnnURL(ApiPath.getTerminateEMRUrl(testNoteBookName, emrNewName));
        LOGGER.info("    SSN terminate EMR URL is {}", ssnTerminateEMRURL);

        Response respTerminateEMR = new HttpRequest().webApiDelete(ssnTerminateEMRURL,
                                                                   ContentType.JSON, token);
        LOGGER.info("    respTerminateEMR.getBody() is {}", respTerminateEMR.getBody().asString());
        Assert.assertEquals(respTerminateEMR.statusCode(), HttpStatusCode.OK);

        gettingStatus = waitWhileEmrStatus(ssnProUserResURL, token, testNoteBookName, emrNewName, "terminating", ConfigPropertyValue.getTimeoutEMRTerminate());
        if (!gettingStatus.contains("terminated"))
            throw new Exception("New EMR " + emrNewName + " has not been terminated. EMR status is " + gettingStatus);
        LOGGER.info("    New EMR {} has been terminated", emrNewName);

        AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrNewName, AmazonInstanceState.TERMINATED.value());
        Docker.checkDockerStatus(nodePrefix + "_terminate_computational_NewEMRAutoTest", ssnIpForTest);
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

        gettingStatus = waitWhileEmrStatus(ssnProUserResURL, token, testNoteBookName, emrNewName, "creating", ConfigPropertyValue.getTimeoutEMRCreate());
        if (!(gettingStatus.contains("configuring") || gettingStatus.contains("running")))
            throw new Exception("New EMR " + emrNewName + " has not been deployed. EMR status is " + gettingStatus);
        LOGGER.info("    New EMR {} has been deployed", emrNewName);

        LOGGER.info("   Waiting until EMR has been configured ...");
        gettingStatus = waitWhileEmrStatus(ssnProUserResURL, token, testNoteBookName, emrNewName, "configuring", ConfigPropertyValue.getTimeoutEMRCreate());
        if (!gettingStatus.contains(AmazonInstanceState.RUNNING.value()))
            throw new Exception("EMR " + emrNewName + " has not been configured. EMR status is " + gettingStatus);
        LOGGER.info("   EMR {} has been configured", emrNewName);

        AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrNewName, AmazonInstanceState.RUNNING.value());
        Docker.checkDockerStatus(nodePrefix + "_create_computational_EMRAutoTest", ssnIpForTest);
        return emrNewName;
    }

    private void restartNotebook(String testNoteBookName, String ssnExpEnvURL, String ssnProUserResURL, String amazonNodePrefix, String nodePrefix, String token) throws Exception {
        LOGGER.info("9. Notebook will be re-started ...");
        String myJs = "{\"notebook_instance_name\":\"" + testNoteBookName + "\"}";
        Response respStartNotebook = new HttpRequest().webApiPost(ssnExpEnvURL, ContentType.JSON,
                                                                  myJs, token);
        LOGGER.info("    respStartNotebook.getBody() is {}", respStartNotebook.getBody().asString());
        Assert.assertEquals(respStartNotebook.statusCode(), HttpStatusCode.OK);

        String gettingStatus = waitWhileNotebookStatus(ssnProUserResURL, token, testNoteBookName, "starting", ConfigPropertyValue.getTimeoutNotebookStartup());
        if (!gettingStatus.contains(AmazonInstanceState.RUNNING.value())){
            throw new Exception("Notebook " + testNoteBookName + " has not been started. Notebook status is " + gettingStatus);
        }
        LOGGER.info("    Notebook {} has been started", testNoteBookName);

        AmazonHelper.checkAmazonStatus(amazonNodePrefix + "-nb-" + testNoteBookName, AmazonInstanceState.RUNNING.value());
        Docker.checkDockerStatus(nodePrefix + "_start_exploratory_NotebookAutoTest", ssnIpForTest);
    }

    class PythonTestCallable implements Callable<Boolean> {

        private String notebook, nodePrefix, amazonNodePrefix, token, ssnExpEnvURL, ssnProUserResURL;
        private boolean testRestartStopped;

        public PythonTestCallable(String notebook, String nodePrefix, String amazonNodePrefix, String token, String ssnExpEnvURL, String ssnProUserResURL, boolean testRestartStopped) {
            this.notebook = notebook;
            this.nodePrefix = nodePrefix;
            this.amazonNodePrefix = amazonNodePrefix;
            this.token = token;
            this.ssnExpEnvURL = ssnExpEnvURL;
            this.ssnProUserResURL = ssnProUserResURL;
            this.testRestartStopped = testRestartStopped;
        }

        @Override
        public Boolean call() throws Exception {

//            String notebookTemplateNamePrefix = notebook.substring(0, notebook.indexOf(".") > 0 ? notebook.indexOf(".") : notebook.length() - 1);
            String notebookTemplateName = TestNamingHelper.generateRandomValue(notebook);

//          String testNoteBookName = "NotebookAutoTest_R-Studio_20170516122058";
//          String testNoteBookName = "NotebookAutoTest_Zeppelin_2017051633454";
            String testNoteBookName = "Notebook" + notebookTemplateName;

//          String emrName = "eimrAutoTest_R-Studio_20170516125150";
//          String emrName = "eimrAutoTest_Zeppelin_2017051641947";
            String emrName = "eimr" + notebookTemplateName;

            String notebookIp = createNotebook(testNoteBookName, notebook, nodePrefix, amazonNodePrefix, token, ssnExpEnvURL, ssnProUserResURL);

            DeployEMRDto deployEMR = createEMR(testNoteBookName, emrName, nodePrefix, amazonNodePrefix, token, ssnProUserResURL);

            String emrClusterName = getEmrClusterName(amazonNodePrefix + "-emr-" + testNoteBookName + "-" + emrName);
            if (!ConfigPropertyValue.isRunModeLocal()) {
                copyTestDataIntoTestBucket(emrName, emrClusterName, notebook);
            }

            String notebookFilesLocation = PropertiesResolver.getPropertyByName(String.format(PropertiesResolver.NOTEBOOK_FILES_LOCATION_PROPERTY_TEMPLATE, notebook));
            testPythonScripts(ssnIpForTest, notebookIp, emrClusterName,
                    new File(notebookFilesLocation), notebook);

            stopEnvironment(nodePrefix, amazonNodePrefix, token, ssnProUserResURL, notebook, testNoteBookName, emrName);

            if (testRestartStopped) {
                restartNotebookAndRedeployToTerminate(testNoteBookName, ssnExpEnvURL, ssnProUserResURL, getSnnURL(ApiPath.COMPUTATIONAL_RES), amazonNodePrefix, nodePrefix, emrName, token, deployEMR);
            }
            if (null != deployEMR) {
                terminateNotebook(testNoteBookName, ssnProUserResURL, amazonNodePrefix, nodePrefix, deployEMR, token);
            }

            return true;
        }
    }

}
