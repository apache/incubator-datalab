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

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.epam.dlab.automation.docker.AckStatus;
import com.epam.dlab.automation.docker.SSHConnect;
import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.epam.dlab.automation.helper.PropertiesResolver;
import com.epam.dlab.automation.helper.NamingHelper;
import com.epam.dlab.automation.helper.WaitForStatus;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
public class TestEmr {
    private final static Logger LOGGER = LogManager.getLogger(WaitForStatus.class);
    
    private final static String COMMAND_COPY_TO_NOTEBOOK = "scp -i %s -o 'StrictHostKeyChecking no' ~/%s %s@%s:/tmp/";
    private final static String COMMAND_RUN_PYTHON = "/usr/bin/python %s --region %s --bucket %s --cluster_name %s --os_user %s";
    private final static String COMMAND_RUN_PYTHON2 = "/usr/bin/python /home/%s/%s --region %s --bucket %s --templates_bucket %s";
    
    
    public void run(String notebookName, String clusterName) throws Exception {
        Session ssnSession = null;
        try {
            LOGGER.info("{}: Copying test data copy scripts {} to SSN {}...",
            		notebookName, ConfigPropertyValue.getS3TestsTemplateBucketName(), NamingHelper.getSsnIp());
            ssnSession = SSHConnect.getSession(ConfigPropertyValue.getClusterOsUser(), NamingHelper.getSsnIp(), 22);
            copyFileToSSN(ssnSession, PropertiesResolver.getNotebookTestDataCopyScriptLocation());
            executePythonScript2(ssnSession, clusterName, new File(PropertiesResolver.getNotebookTestDataCopyScriptLocation()).getName(), notebookName);
        } finally {
	        if(ssnSession != null && !ssnSession.isConnected()) {
	            ssnSession.disconnect();
	        }
        }
    }
    
    //TODO refactor two methods and make one
    private void executePythonScript2(Session ssnSession, String clusterName, String notebookTestFile, String notebookName) throws JSchException, IOException, InterruptedException {
        String command;
        AckStatus status;

        command = String.format(COMMAND_RUN_PYTHON2, ConfigPropertyValue.getClusterOsUser(), notebookTestFile,
        			ConfigPropertyValue.getAwsRegion(), NamingHelper.getBucketName(), ConfigPropertyValue.getS3TestsTemplateBucketName());
        LOGGER.info("{}: Executing command {}...", notebookName, command);

        ChannelExec runScript = SSHConnect.setCommand(ssnSession, command);
        status = SSHConnect.checkAck(runScript);
        LOGGER.info("{}: Script execution status message {} and code {}", notebookName, status.getMessage(), status.getStatus());
        assertTrue(status.isOk(), notebookName + ": The python script execution wasn`t successful on : " + clusterName);

        LOGGER.info("{}: Python script executed successfully ", notebookName);
    }

    private void executePythonScript(String Ip, String cluster_name, String notebookTestFile, int assignedPort, String notebookName) throws JSchException, IOException, InterruptedException {
        String command;
        AckStatus status;
        Session session = SSHConnect.getForwardedConnect(ConfigPropertyValue.getClusterOsUser(), Ip, assignedPort);

        try {
            command = String.format(COMMAND_RUN_PYTHON,
                    "/tmp/" +  notebookTestFile,
                    ConfigPropertyValue.getAwsRegion(),
                    NamingHelper.getBucketName(),
                    cluster_name,
                    ConfigPropertyValue.getClusterOsUser());
            LOGGER.info(String.format("{}: Executing command %s...", command), notebookName);

            ChannelExec runScript = SSHConnect.setCommand(session, command);
            status = SSHConnect.checkAck(runScript);
            LOGGER.info("{}: Script execution status message {} and status code {}", notebookName, status.getMessage(), status.getStatus());
            assertTrue(status.isOk(), notebookName + ": The python script execution wasn`t successful on " + cluster_name);

            LOGGER.info("{}: Python script executed successfully ", notebookName);
        }
        finally {
            if(session != null && session.isConnected()) {
                LOGGER.info("{}: Closing notebook session", notebookName);
                session.disconnect();
            }
        }
    }

    public void run2(String ssnIP, String noteBookIp, String clusterName, File notebookDirectory, String notebookName)
            throws JSchException, IOException, InterruptedException {
    	LOGGER.info("Python tests for {} will be started ...", notebookDirectory);
    	if (ConfigPropertyValue.isRunModeLocal()) {
    		LOGGER.info("  tests are skipped");
    		return;
    	}

        assertTrue(notebookDirectory.exists(), notebookName + ": Checking notebook directory " + notebookDirectory);
        assertTrue(notebookDirectory.isDirectory());

    	String [] files = notebookDirectory.list();
    	assertTrue(files.length == 1, "The python script location " + notebookDirectory + " found more more then 1 file, expected 1 *.py file, but found multiple files: " + Arrays.toString(files));
        assertTrue(files[0].endsWith(".py"), "The python script was not found");
        // it is assumed there should be 1 python file.
        String notebookTestFile = files[0];

        Session ssnSession = SSHConnect.getSession(ConfigPropertyValue.getClusterOsUser(), ssnIP, 22);
        try {
            LOGGER.info("{}: Copying files to SSN {}...", notebookName, ssnIP);
        	copyFileToSSN(ssnSession, Paths.get(notebookDirectory.getAbsolutePath(), notebookTestFile).toString());
        	
        	LOGGER.info("{}: Copying files to Notebook {}...", notebookName, noteBookIp);
            for (String filename : files) {
            	copyFileToNotebook(ssnSession, filename, noteBookIp);
    		}

            LOGGER.info("{}: Port forwarding from ssn {} to notebook {}...", notebookName, ssnIP, noteBookIp);
            int assignedPort = ssnSession.setPortForwardingL(0, noteBookIp, 22);
            LOGGER.info("{}: Port forwarded localhost:{} -> {}:22", notebookName, assignedPort, noteBookIp);

            executePythonScript(noteBookIp, clusterName, notebookTestFile, assignedPort, notebookName);
        }
        finally {
            if(ssnSession != null && ssnSession.isConnected()) {
                LOGGER.info("{}: Closing ssn session", notebookName);
                ssnSession.disconnect();
            }
        }
    }

    
    private void copyFileToSSN(Session ssnSession, String filenameWithPath) throws IOException, InterruptedException, JSchException {
        LOGGER.info("Copying {}...", filenameWithPath);
        File file = new File(filenameWithPath);
        assertTrue(file.exists());

        ChannelSftp channelSftp = null;
        FileInputStream src = new FileInputStream(file);
        try {
        	channelSftp = SSHConnect.getChannelSftp(ssnSession);
        	channelSftp.put(src, String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), file.getName()));
        } catch (SftpException e) {
            LOGGER.error(e);
            assertTrue(false);
        } finally {
            if(channelSftp != null && !channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }

    }
    
    private void copyFileToNotebook(Session session, String filename, String ip) throws JSchException, IOException, InterruptedException {
    	String command = String.format(COMMAND_COPY_TO_NOTEBOOK,
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
