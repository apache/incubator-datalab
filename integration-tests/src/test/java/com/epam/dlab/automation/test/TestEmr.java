package com.epam.dlab.automation.test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.epam.dlab.automation.helper.WaitForStatus;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
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
import com.epam.dlab.automation.helper.WaitForStatus;
import com.epam.dlab.automation.http.HttpRequest;
import com.epam.dlab.automation.http.HttpStatusCode;
import com.epam.dlab.automation.jenkins.JenkinsService;
import com.epam.dlab.automation.model.CreateNotebookDto;
import com.epam.dlab.automation.model.DeployEMRDto;
import com.epam.dlab.automation.model.LoginDto;
import com.epam.dlab.automation.repository.ApiPath;
import com.epam.dlab.automation.repository.ContentType;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
public class TestEmr {
    private final static Logger LOGGER = LogManager.getLogger(WaitForStatus.class);
    
    private final String serviceBaseName;
    private final String ssnIp;
    
    public TestEmr(String serviceBaseName, String ssnIp) {
    	this.serviceBaseName = serviceBaseName;
    	this.ssnIp = ssnIp;
	}
    
    public void run(String emrName, String clusterName, String notebook) throws Exception {
        Session ssnSession = null;
        ChannelSftp channelSftp = null;
        try {
            LOGGER.info("{}: Copying test data copy scripts {} to SSN {}...", notebook, ConfigPropertyValue.getS3TestsTemplateBucketName(), ssnIp);
            ssnSession = SSHConnect.getSession(ConfigPropertyValue.getClusterOsUser(), ssnIp, 22);
            channelSftp = SSHConnect.getChannelSftp(ssnSession);

            copyFileToSSN(channelSftp, PropertiesResolver.getNotebookTestDataCopyScriptLocation());
            executePythonScript2(ssnSession, clusterName, new File(PropertiesResolver.getNotebookTestDataCopyScriptLocation()).getName(), notebook);
        } finally {
	        if(channelSftp != null && !channelSftp.isConnected()) {
	            channelSftp.disconnect();
	        }
	        if(ssnSession != null && !ssnSession.isConnected()) {
	            ssnSession.disconnect();
	        }
        }
    }
    
    //TODO refactor two methods and make one
    private void executePythonScript2(Session ssnSession, String clusterName, String notebookTestFile, String notebook) throws JSchException, IOException, InterruptedException {
        String command;
        AckStatus status;

        command = String.format(ScpCommands.runPythonCommand2,
                    String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), notebookTestFile),
                    TestNamingHelper.getBucketName(serviceBaseName), ConfigPropertyValue.getS3TestsTemplateBucketName());
        LOGGER.info("{}: Executing command {}...", notebook, command);

        ChannelExec runScript = SSHConnect.setCommand(ssnSession, command);
        status = SSHConnect.checkAck(runScript);
        LOGGER.info("{}: Script execution status message {} and code {}", notebook, status.getMessage(), status.getStatus());
        assertTrue(status.isOk(), notebook + ": The python script execution wasn`t successful on : " + clusterName);

        LOGGER.info("{}: Python script executed successfully ", notebook);
    }

    private void executePythonScript(String Ip, String cluster_name, String notebookTestFile, int assignedPort, String notebook) throws JSchException, IOException, InterruptedException {
        String command;
        AckStatus status;
        Session session = SSHConnect.getForwardedConnect(ConfigPropertyValue.getClusterOsUser(), Ip, assignedPort);

        try {
            command = String.format(ScpCommands.runPythonCommand,
                    "/tmp/" +  notebookTestFile,
                    TestNamingHelper.getBucketName(serviceBaseName),
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

    public void run2(String ssnIP, String noteBookIp, String clusterName, File notebookDirectory, String notebook)
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



}
