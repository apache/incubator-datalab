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

import com.epam.datalab.automation.docker.AckStatus;
import com.epam.datalab.automation.docker.SSHConnect;
import com.epam.datalab.automation.helper.CloudHelper;
import com.epam.datalab.automation.helper.ConfigPropertyValue;
import com.epam.datalab.automation.helper.NamingHelper;
import com.epam.datalab.automation.helper.PropertiesResolver;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

class TestDataEngineService {
    private final static Logger LOGGER = LogManager.getLogger(TestDataEngineService.class);
    
    private final static String COMMAND_COPY_TO_NOTEBOOK;
    private final static String COMMAND_RUN_PYTHON;
    private final static String COMMAND_RUN_PYTHON2;

    static {
        COMMAND_COPY_TO_NOTEBOOK = "scp -r -i %s -o 'StrictHostKeyChecking no' ~/%s %s@%s:/tmp/%s";
        COMMAND_RUN_PYTHON = CloudHelper.getPythonTestingScript();
        COMMAND_RUN_PYTHON2 = CloudHelper.getPythonTestingScript2();
    }


	void run(String notebookName, String notebookTemplate, String clusterName) throws Exception {
        Session ssnSession = null;
        try {
            LOGGER.info("{}: Copying test data copy scripts {} to SSN {}...",
            		notebookName, NamingHelper.getStorageName(), NamingHelper.getSsnIp());
            ssnSession = SSHConnect.getSession(ConfigPropertyValue.getClusterOsUser(), NamingHelper.getSsnIp(), 22);
            copyFileToSSN(ssnSession, PropertiesResolver.getNotebookTestDataCopyScriptLocation(), "");
			executePythonScript2(ssnSession, clusterName,
					new File(PropertiesResolver.getNotebookTestDataCopyScriptLocation()).getName(),
					notebookName, notebookTemplate);
        } finally {
            if (ssnSession != null && ssnSession.isConnected()) {
	            ssnSession.disconnect();
	        }
        }
    }
    
    //TODO refactor two methods and make one
	private void executePythonScript2(Session ssnSession, String clusterName, String notebookTestFile,
									  String notebookName, String notebookTemplate) throws JSchException,
			InterruptedException {
        String command;
        AckStatus status;

        command = String.format(COMMAND_RUN_PYTHON2, ConfigPropertyValue.getClusterOsUser(), notebookTestFile,
				NamingHelper.getStorageName(), notebookTemplate);
        LOGGER.info("{}: Executing command {}...", notebookName, command);

        ChannelExec runScript = SSHConnect.setCommand(ssnSession, command);
        status = SSHConnect.checkAck(runScript);
        LOGGER.info("{}: Script execution status message {} and code {}", notebookName, status.getMessage(), status.getStatus());
        assertTrue(status.isOk(), notebookName + ": The python script execution wasn`t successful on : " + clusterName);

        LOGGER.info("{}: Python script executed successfully ", notebookName);
    }

	private void executePythonScript(String Ip, String cluster_name, String notebookTestFile, int assignedPort,
									 String notebookName) throws JSchException, InterruptedException {
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
			LOGGER.info("{}: Script execution status message {} and status code {}", notebookName, status.getMessage(),
					status.getStatus());
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

	void run2(String ssnIP, String noteBookIp, String clusterName, File notebookScenarioDirectory,
			  File notebookTemplatesDirectory, String notebookName)
            throws JSchException, IOException, InterruptedException {
		LOGGER.info("Python tests for directories {} and {} will be started ...", notebookScenarioDirectory,
				notebookTemplatesDirectory);
    	if (ConfigPropertyValue.isRunModeLocal()) {
    		LOGGER.info("  tests are skipped");
    		return;
    	}

		assertTrue(notebookScenarioDirectory.exists(), notebookName + ": Checking notebook scenario directory " +
				notebookScenarioDirectory);
        assertTrue(notebookScenarioDirectory.isDirectory());

		assertTrue(notebookTemplatesDirectory.exists(), notebookName + ": Checking notebook templates directory " +
				notebookTemplatesDirectory);
        assertTrue(notebookTemplatesDirectory.isDirectory());

        String [] templatesFiles = notebookTemplatesDirectory.list();
        assertNotNull(templatesFiles, "Notebook " + notebookName + " templates directory is empty!");

    	String [] scenarioFiles = notebookScenarioDirectory.list();
        assertNotNull(scenarioFiles, "Notebook " + notebookName + " scenario directory is empty!");

		assertEquals(1, scenarioFiles.length, "The python script location " + notebookScenarioDirectory +
				" found more more then 1 file, expected 1 *.py file, but found multiple files: " +
				Arrays.toString(scenarioFiles));
        assertTrue(scenarioFiles[0].endsWith(".py"), "The python script was not found");
        // it is assumed there should be 1 python file.
        String notebookScenarioTestFile = scenarioFiles[0];

        Session ssnSession = SSHConnect.getSession(ConfigPropertyValue.getClusterOsUser(), ssnIP, 22);
        try {
            LOGGER.info("{}: Copying scenario test file to SSN {}...", notebookName, ssnIP);
			copyFileToSSN(ssnSession, Paths.get(notebookScenarioDirectory.getAbsolutePath(),
					notebookScenarioTestFile).toString(), "");

        	LOGGER.info("{}: Copying scenario test file to Notebook {}...", notebookName, noteBookIp);
            copyFileToNotebook(ssnSession, notebookScenarioTestFile, noteBookIp, "");

            LOGGER.info("In notebook templates directory {} available following template files: {}",
                    notebookTemplatesDirectory, Arrays.toString(templatesFiles));

            if(existsInSSN(ssnSession, NamingHelper.getNotebookTestTemplatesPath(notebookName))){
				LOGGER.info("{}: Corresponding folder for notebook templates already exists in SSN {} " +
						"and will be removed ...", notebookName, ssnIP);
                removeFromSSN(ssnSession, NamingHelper.getNotebookTestTemplatesPath(notebookName).split("/")[0]);
            }

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

			if (!clusterName.equalsIgnoreCase(NamingHelper.CLUSTER_ABSENT)
					|| !NamingHelper.isClusterRequired(notebookName)) {
				LOGGER.info("{}: Port forwarding from ssn {} to notebook {}...", notebookName, ssnIP, noteBookIp);
				int assignedPort = ssnSession.setPortForwardingL(0, noteBookIp, 22);
				LOGGER.info("{}: Port forwarded localhost:{} -> {}:22", notebookName, assignedPort, noteBookIp);
				executePythonScript(noteBookIp, clusterName, notebookScenarioTestFile, assignedPort, notebookName);
			}
        }
        finally {
            if(ssnSession != null && ssnSession.isConnected()) {
                LOGGER.info("{}: Closing ssn session", notebookName);
                ssnSession.disconnect();
            }
        }
    }

    // Copies file to subfolder of home directory of SSN. If parameter 'destDirectoryInSSN' is empty string then copies
    // to home directory.
	private void copyFileToSSN(Session ssnSession, String sourceFilenameWithPath, String destDirectoryInSSN)
			throws IOException, JSchException {
        LOGGER.info("Copying {} to SSN...", sourceFilenameWithPath);
        File file = new File(sourceFilenameWithPath);
        assertTrue(file.exists(), "Source file " + sourceFilenameWithPath + " doesn't exist!");
        LOGGER.info("Source file {} exists: {}", sourceFilenameWithPath, file.exists());

        ChannelSftp channelSftp = null;
        FileInputStream src = new FileInputStream(file);
        try {
        	channelSftp = SSHConnect.getChannelSftp(ssnSession);
			channelSftp.put(src,
					String.format("/home/%s/%s%s", ConfigPropertyValue.getClusterOsUser(), destDirectoryInSSN, file
							.getName()));
        } catch (SftpException e) {
            LOGGER.error("An error occured during copying file to SSN: {}", e);
			fail("Copying file " + file.getName() + " to SSN is failed");
        } finally {
            if(channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }

    }

    // Creates a folder in home directory of SSN
    private void mkDirInSSN(Session ssnSession, String directoryName) throws JSchException {
        String newDirectoryAbsolutePath = String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), directoryName);
        LOGGER.info("Creating directory {} in SSN...", newDirectoryAbsolutePath);

        ChannelSftp channelSftp = null;
        try {
            channelSftp = SSHConnect.getChannelSftp(ssnSession);
            if(!directoryName.equals("")){
                String[] partsOfPath = directoryName.split("/");
                StringBuilder sb = new StringBuilder();
                for(String partOfPath : partsOfPath){
                    if(partOfPath.equals("")){
                        continue;
                    }
                    sb.append(partOfPath);
                    if(!existsInSSN(ssnSession, sb.toString())){
                        LOGGER.info("Creating directory {} in SSN...",
                                String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), sb.toString()));
                        channelSftp.mkdir(String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), sb.toString()));
                    }
                    sb.append("/");
                }
            }
            assertTrue(channelSftp.stat(newDirectoryAbsolutePath).isDir(), "Directory " + newDirectoryAbsolutePath +
                    " wasn't created in SSN!");
        } catch (SftpException e) {
            LOGGER.error("An error occured during creation directory in SSN: {}", e);
			fail("Creating directory " + newDirectoryAbsolutePath + " in SSN is failed");
        } finally {
            if(channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }

    }

    // Checks if file exists in home directory of SSN
    private boolean existsInSSN(Session ssnSession, String fileName) throws JSchException {
        String homeDirectoryAbsolutePath = String.format("/home/%s", ConfigPropertyValue.getClusterOsUser());
        LOGGER.info("Checking if file/directory {} exists in home directory {} of SSN...", fileName, homeDirectoryAbsolutePath);

        boolean isFileEmbeddedIntoFolder = fileName.contains("/");
        ChannelSftp channelSftp = null;
        List<String> fileNames = new ArrayList<>();
        try {
            channelSftp = SSHConnect.getChannelSftp(ssnSession);
            Vector fileDataList = channelSftp.ls(homeDirectoryAbsolutePath);
            for (Object fileData : fileDataList) {
                ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) fileData;
                fileNames.add(entry.getFilename());
            }
            if(fileNames.isEmpty()){
				LOGGER.info("Does file/directory {} exist in home directory {} of SSN: {}",
                        fileName, homeDirectoryAbsolutePath, "false");
                return false;
            }
            LOGGER.info("In home directory {} of SSN there are following files: {}",
                    homeDirectoryAbsolutePath, fileNames);
            if(!isFileEmbeddedIntoFolder){
				LOGGER.info("Does file/directory {} exist in home directory {} of SSN: {}",
                        fileName, homeDirectoryAbsolutePath, fileNames.contains(fileName));
                return fileNames.contains(fileName);
            }else{
                List<String> partsOfPath =
                        Stream.of(fileName.split("/")).filter(e -> !e.equals("")).collect(Collectors.toList());
                StringBuilder currentPath = new StringBuilder(homeDirectoryAbsolutePath);
                for(int i = 0; i < partsOfPath.size(); i++){
                    String partOfPath = partsOfPath.get(i);
                    if(fileNames.isEmpty() || !fileNames.contains(partOfPath)){
						LOGGER.info("Does file/directory {} exist in home directory {} of SSN: {}",
                                fileName, homeDirectoryAbsolutePath, "false");
                        return false;
                    }else{
                        if(i == partsOfPath.size() - 1){
							LOGGER.info("Does file/directory {} exist in home directory {} of SSN: {}",
                                    fileName, homeDirectoryAbsolutePath, "true");
                            return true;
                        }
                        currentPath.append("/").append(partOfPath);
                        fileDataList = channelSftp.ls(currentPath.toString());
                        fileNames = new ArrayList<>();
                        for (Object fileData : fileDataList) {
                            ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) fileData;
                            fileNames.add(entry.getFilename());
                        }

                    }

                }

            }

        } catch (SftpException e) {
            LOGGER.error("An error occured during obtaining list of files from home directory in SSN: {}", e);
        } finally {
            if(channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }
		LOGGER.info("Does file/directory {} exist in home directory {} of SSN: {}",
                fileName, homeDirectoryAbsolutePath, "false");
        return false;
    }

    // Removes file or directory from home directory of SSN
    private void removeFromSSN(Session ssnSession, String fileNameWithRelativePath) throws JSchException {
        String absoluteFilePath = String.format("/home/%s/%s", ConfigPropertyValue.getClusterOsUser(), fileNameWithRelativePath);

        ChannelSftp channelSftp = null;
        try {
            channelSftp = SSHConnect.getChannelSftp(ssnSession);
            boolean isDir = channelSftp.stat(absoluteFilePath).isDir();
            LOGGER.info("Is file {} a directory in SSN: {}", absoluteFilePath, isDir);
            if(isDir){
                LOGGER.info("Removing directory {} from SSN...", absoluteFilePath);
                recursiveDirectoryDelete(ssnSession, absoluteFilePath);
            }else{
                LOGGER.info("Removing file {} from SSN...", absoluteFilePath);
                channelSftp.rm(absoluteFilePath);
            }
        } catch (SftpException e) {
            LOGGER.error("An error occured during removing file {} from SSN: {}", absoluteFilePath, e);
        } finally {
            if(channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }
    }

    private void recursiveDirectoryDelete(Session ssnSession, String remoteDir) throws JSchException{
        ChannelSftp channelSftp = null;
        try{
            channelSftp = SSHConnect.getChannelSftp(ssnSession);
            boolean isDir = channelSftp.stat(remoteDir).isDir();
            if(isDir){
                Vector dirList = channelSftp.ls(remoteDir);
                for(Object fileData : dirList){
                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) fileData;
                    if(!(entry.getFilename().equals(".") || entry.getFilename().equals(".."))){
                        if(entry.getAttrs().isDir()){
                            recursiveDirectoryDelete(ssnSession, remoteDir + File.separator
                                    + entry.getFilename() + File.separator);
                        }
                        else{
                            channelSftp.rm(remoteDir + entry.getFilename());
                        }
                    }
                }
                channelSftp.cd("..");
                channelSftp.rmdir(remoteDir);
            }
        }
        catch (SftpException e){
            LOGGER.error("An error occured while deleting directory {}: {}", remoteDir, e.getMessage());
        }
        finally {
            if(channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        }
    }

	private void copyFileToNotebook(Session session, String filename, String ip, String notebookName)
			throws JSchException, InterruptedException {
    	String command = String.format(COMMAND_COPY_TO_NOTEBOOK,
    			"keys/"+ Paths.get(ConfigPropertyValue.getAccessKeyPrivFileName()).getFileName().toString(),
                filename,
                ConfigPropertyValue.getClusterOsUser(),
                ip,
                NamingHelper.getNotebookType(notebookName));

    	LOGGER.info("Copying {} to notebook...", filename);
    	LOGGER.info("  Run command: {}", command);

        ChannelExec copyResult = SSHConnect.setCommand(session, command);
        AckStatus status = SSHConnect.checkAck(copyResult);

        LOGGER.info("Copied {}: {}", filename, status.toString());
        assertTrue(status.isOk());
    }

}
