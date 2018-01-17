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

import com.epam.dlab.automation.helper.CloudHelper;
import com.jcraft.jsch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.epam.dlab.automation.docker.AckStatus;
import com.epam.dlab.automation.docker.SSHConnect;
import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.epam.dlab.automation.helper.PropertiesResolver;
import com.epam.dlab.automation.helper.NamingHelper;

public class TestDataEngineService {
    private final static Logger LOGGER = LogManager.getLogger(TestDataEngineService.class);
    
    private final static String COMMAND_COPY_TO_NOTEBOOK;
    private final static String COMMAND_RUN_PYTHON;
    private final static String COMMAND_RUN_PYTHON2;

    static {
        COMMAND_COPY_TO_NOTEBOOK = "scp -r -i %s -o 'StrictHostKeyChecking no' ~/%s %s@%s:/tmp/%s";
        COMMAND_RUN_PYTHON = CloudHelper.getPythonTestingScript();
        COMMAND_RUN_PYTHON2 = CloudHelper.getPythonTestingScript2();
    }
    
    
    public void run(String notebookName, String clusterName) throws Exception {
        Session ssnSession = null;
        try {
            LOGGER.info("{}: Copying test data copy scripts {} to SSN {}...",
            		notebookName, NamingHelper.getStorageName(), NamingHelper.getSsnIp());
            ssnSession = SSHConnect.getSession(ConfigPropertyValue.getClusterOsUser(), NamingHelper.getSsnIp(), 22);
            copyFileToSSN(ssnSession, PropertiesResolver.getNotebookTestDataCopyScriptLocation(), "");
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
        			NamingHelper.getStorageName());
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
                    NamingHelper.getStorageName(),
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

    public void run2(String ssnIP, String noteBookIp, String clusterName, File notebookScenarioDirectory, File notebookTemplatesDirectory, String notebookName)
            throws JSchException, IOException, InterruptedException {
    	LOGGER.info("Python tests for directories {} and {} will be started ...", notebookScenarioDirectory, notebookTemplatesDirectory);
    	if (ConfigPropertyValue.isRunModeLocal()) {
    		LOGGER.info("  tests are skipped");
    		return;
    	}

        assertTrue(notebookScenarioDirectory.exists(), notebookName + ": Checking notebook scenario directory " + notebookScenarioDirectory);
        assertTrue(notebookScenarioDirectory.isDirectory());

        assertTrue(notebookTemplatesDirectory.exists(), notebookName + ": Checking notebook templates directory " + notebookTemplatesDirectory);
        assertTrue(notebookTemplatesDirectory.isDirectory());

        String [] templatesFiles = notebookTemplatesDirectory.list();

    	String [] scenarioFiles = notebookScenarioDirectory.list();
    	assertTrue(scenarioFiles.length == 1, "The python script location " + notebookScenarioDirectory +
                " found more more then 1 file, expected 1 *.py file, but found multiple files: " + Arrays.toString(scenarioFiles));
        assertTrue(scenarioFiles[0].endsWith(".py"), "The python script was not found");
        // it is assumed there should be 1 python file.
        String notebookScenarioTestFile = scenarioFiles[0];

        Session ssnSession = SSHConnect.getSession(ConfigPropertyValue.getClusterOsUser(), ssnIP, 22);
        try {
            LOGGER.info("{}: Copying scenario test file to SSN {}...", notebookName, ssnIP);
        	copyFileToSSN(ssnSession, Paths.get(notebookScenarioDirectory.getAbsolutePath(), notebookScenarioTestFile).toString(), "");
        	
        	LOGGER.info("{}: Copying scenario test file to Notebook {}...", notebookName, noteBookIp);
            copyFileToNotebook(ssnSession, notebookScenarioTestFile, noteBookIp, "");

            LOGGER.info("In notebook templates directory {} available following template files: {}",
                    notebookTemplatesDirectory, Arrays.toString(templatesFiles));

            LOGGER.info("{}: Creating subfolder in home directory in SSN for copying templates {}...", notebookName, ssnIP);
            mkDirInSSN(ssnSession, NamingHelper.getNotebookTestTemplatesPath(notebookName));

            LOGGER.info("{}: Copying templates to SSN {}...", notebookName, ssnIP);
            for(String filename : templatesFiles){
                copyFileToSSN(ssnSession, Paths.get(notebookTemplatesDirectory.getAbsolutePath(), filename).toString(),
                        NamingHelper.getNotebookTestTemplatesPath(notebookName));
            }

            LOGGER.info("{}: Copying templates to Notebook {}...", notebookName, noteBookIp);
            copyFileToNotebook(ssnSession, NamingHelper.getNotebookTestTemplatesPath(notebookName),
                        noteBookIp, notebookName);

            LOGGER.info("{}: Port forwarding from ssn {} to notebook {}...", notebookName, ssnIP, noteBookIp);
            int assignedPort = ssnSession.setPortForwardingL(0, noteBookIp, 22);
            LOGGER.info("{}: Port forwarded localhost:{} -> {}:22", notebookName, assignedPort, noteBookIp);

            executePythonScript(noteBookIp, clusterName, notebookScenarioTestFile, assignedPort, notebookName);
        }
        finally {
            if(ssnSession != null && ssnSession.isConnected()) {
                LOGGER.info("{}: Closing ssn session", notebookName);
                ssnSession.disconnect();
            }
        }
    }

    
    private void copyFileToSSN(Session ssnSession, String filenameWithPath, String directoryInRootSSN) throws IOException, JSchException {
        LOGGER.info("Copying {}...", filenameWithPath);
        File file = new File(filenameWithPath);
        assertTrue(file.exists(), "File " + filenameWithPath + " doesn't exist!");
        LOGGER.info("File {} exists {}", filenameWithPath, file.exists());

        ChannelSftp channelSftp = null;
        FileInputStream src = new FileInputStream(file);
        try {
        	channelSftp = SSHConnect.getChannelSftp(ssnSession);
        	channelSftp.put(src, String.format("/home/%s/%s%s", ConfigPropertyValue.getClusterOsUser(), directoryInRootSSN, file.getName()));
        } catch (SftpException e) {
            LOGGER.error("An error occured during copying file to SSN: {}", e);
            assertTrue(false, "Copying file " + file.getName() + " to SSN is failed");
        } finally {
            if(channelSftp != null && !channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }

    }

    private void mkDirInSSN(Session ssnSession, String directoryName) throws JSchException {
        String newDirectoryAbsolutePath = String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), directoryName);
        LOGGER.info("Creating directory {} in SSN ...", newDirectoryAbsolutePath);

        ChannelSftp channelSftp = null;
        try {
            channelSftp = SSHConnect.getChannelSftp(ssnSession);
            if(!directoryName.equals("")){
                LOGGER.info("Additional info about path {}: value = {}",
                        newDirectoryAbsolutePath,
                        channelSftp.readlink(newDirectoryAbsolutePath));
                String[] partsOfPath = directoryName.split("/");
                StringBuilder sb = new StringBuilder();
                for(String partOfPath : partsOfPath){
                    if(partOfPath.equals("")){
                        continue;
                    }
                    sb.append(partOfPath);
                    LOGGER.info("Additional info about path {}: value = {}",
                            String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), sb.toString()),
                            channelSftp.readlink(String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), sb.toString())));
                    LOGGER.info("Creating directory {} in SSN ...",
                            String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), sb.toString()));
                    channelSftp.mkdir(String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), sb.toString()));
                    sb.append("/");
                }
            }
            assertTrue(channelSftp.stat(newDirectoryAbsolutePath).isDir(), "Directory " + newDirectoryAbsolutePath +
                    " wasn't created in SSN!");
        } catch (SftpException e) {
            LOGGER.error("An error occured during creation directory in SSN: {}", e);
            assertTrue(false, "Creating directory " + newDirectoryAbsolutePath + " in SSN is failed");
        } finally {
            if(channelSftp != null && !channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }

    }
    
    private void copyFileToNotebook(Session session, String filename, String ip, String notebookName) throws JSchException, IOException, InterruptedException {
    	String command = String.format(COMMAND_COPY_TO_NOTEBOOK,
    			"keys/"+ Paths.get(ConfigPropertyValue.getAccessKeyPrivFileName()).getFileName().toString(),
                filename,
                ConfigPropertyValue.getClusterOsUser(),
                ip,
                NamingHelper.getNotebookType(notebookName));

    	LOGGER.info("Copying {}...", filename);
    	LOGGER.info("  Run command: {}", command);

        ChannelExec copyResult = SSHConnect.setCommand(session, command);
        AckStatus status = SSHConnect.checkAck(copyResult);

        LOGGER.info("Copied {}: {}", filename, status.toString());
        assertTrue(status.isOk());
    }

}
