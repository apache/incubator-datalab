package com.epam.dlab.automation.test;

import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.amazonaws.services.ec2.model.Instance;
import com.epam.dlab.automation.aws.AmazonHelper;
import com.epam.dlab.automation.aws.AmazonInstanceState;
import com.epam.dlab.automation.docker.Docker;
import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.epam.dlab.automation.helper.PropertiesResolver;
import com.epam.dlab.automation.helper.TestNamingHelper;
import com.epam.dlab.automation.helper.WaitForStatus;
import com.epam.dlab.automation.http.HttpRequest;
import com.epam.dlab.automation.http.HttpStatusCode;
import com.epam.dlab.automation.jenkins.JenkinsService;
import com.epam.dlab.automation.model.LoginDto;
import com.epam.dlab.automation.repository.ApiPath;
import com.epam.dlab.automation.repository.ContentType;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;

@Test(singleThreaded=true)
public class TestServices {

    private final static Logger LOGGER = LogManager.getLogger(TestServices.class);
    // This time 3 notebooks are tested in parallel - so 3 threads are used, restartNotebookAndRedeployToTerminate are a pool for future notebooks grow.
    // needed to investigate Amazon behaviour when same AIM requests set of computation resources in parallel
    // looks like running test in 1 thread mostly succeeds, running in 2 and more threads - usually fails.
    public static final int N_THREADS = 10;

    private String serviceBaseName;
    private String ssnURL;
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
        String publicSsnIp = ssnInstance.getPublicIpAddress();
        LOGGER.info("Public IP is: {}", publicSsnIp);
        String privateSsnIp = ssnInstance.getPrivateIpAddress();
        LOGGER.info("Private IP is: {}", privateSsnIp);
        ssnIpForTest = PropertiesResolver.DEV_MODE ? publicSsnIp : privateSsnIp;
        AmazonHelper.checkAmazonStatus(serviceBaseName + "-ssn", AmazonInstanceState.RUNNING.value());
        LOGGER.info("Amazon instance state is running");
        
        LOGGER.info("2. Waiting for SSN service ...");
        Assert.assertEquals(WaitForStatus.selfService(ssnURL, ConfigPropertyValue.getTimeoutNotebookCreate()), true, "SSN service was not started");
        LOGGER.info("   SSN service is available");
        
        
        LOGGER.info("3. Check login");
        final String ssnLoginURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.LOGIN);
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
        final String ssnlogoutURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.LOGOUT);
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
        RestAssured.baseURI = ssnURL;
        String token = ssnLoginAndKeyUpload(nodePrefix, TestNamingHelper.getAmazonNodePrefix(serviceBaseName, nodePrefix));

        runTestsInNotebooks(nodePrefix, token);
    }

    private String ssnLoginAndKeyUpload(String nodePrefix, String amazonNodePrefix) throws Exception {
        LoginDto testUserRequestBody = new LoginDto(ConfigPropertyValue.getUsername(), ConfigPropertyValue.getPassword(), "");

        LOGGER.info("5. Login as {} ...", ConfigPropertyValue.getUsername());

        final String ssnLoginURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.LOGIN);
        LOGGER.info("   SSN login URL is {}", ssnLoginURL);

        final String ssnUploadKeyURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.UPLOAD_KEY);
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
            int responseCodeAccessKey = WaitForStatus.uploadKey(ssnUploadKeyURL, token, HttpStatusCode.Accepted, ConfigPropertyValue.getTimeoutUploadKey());
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


        final String ssnExpEnvURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.EXP_ENVIRONMENT);
        LOGGER.info("   SSN exploratory environment URL is {}", ssnExpEnvURL);
        final String ssnProUserResURL = TestNamingHelper.getSelfServiceURL(ssnURL, ApiPath.PROVISIONED_RES);
        LOGGER.info("   SSN provisioned user resources URL is {}", ssnProUserResURL);

        return token;
    }

    private void runTestsInNotebooks(String nodePrefix, String token) throws Exception {
        List<String> notebooks = Arrays.asList(ConfigPropertyValue.getNotebooks().split(","));
        LOGGER.info("Testing the following notebooks: {}", ConfigPropertyValue.getNotebooks());
        ExecutorService executor =  Executors.newFixedThreadPool(ConfigPropertyValue.getExecutionThreads() > 0 ? ConfigPropertyValue.getExecutionThreads(): N_THREADS);
        List<FutureTask<Boolean>> futureTasks = new ArrayList<>();

        boolean testRestartStopped = true;
        for (String notebook: notebooks) {
            FutureTask<Boolean> runScenarioTask = new FutureTask<>(new TestCallable(serviceBaseName, ssnURL, ssnIpForTest, notebook, nodePrefix, token, testRestartStopped));
            testRestartStopped = false;
            futureTasks.add(runScenarioTask);
            executor.execute(runScenarioTask);
        }

        final long checkThreadTimeout = ConfigPropertyValue.isRunModeLocal() ? 1000 : 5000;
        while (true) {
            boolean done = true;
            done = allScenariosDone(futureTasks);
            if (done) {
                verifyResults(futureTasks);
                executor.shutdown();
                return;
            } else {
                Thread.sleep(checkThreadTimeout);
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

        if(resExceptions.size() > 0) {
            for(Exception exception: resExceptions) {
                LOGGER.error("{} :\n {} ", exception, exception.getStackTrace());
                exception.printStackTrace();
            }
            assertTrue(false, "There were failed tests with " +  resExceptions.size() + " from " + futureTasks.size() + " notebooks, see stacktrace above." );
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
}
