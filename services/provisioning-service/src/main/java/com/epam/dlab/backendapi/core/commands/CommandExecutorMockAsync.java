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

package com.epam.dlab.backendapi.core.commands;

import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.dto.status.EnvResourceList;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.util.ServiceUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CommandExecutorMockAsync implements Supplier<Boolean> {
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutorMockAsync.class);
	private static final String JSON_FILE_ENDING = ".json";

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
			.setSerializationInclusion(JsonInclude.Include.NON_NULL);

	private String user;
	private String uuid;
	private String command;

	private CommandParserMock parser = new CommandParserMock();
	private String responseFileName;

	private CloudProvider cloudProvider;

	public CommandExecutorMockAsync(String user, String uuid, String command, CloudProvider cloudProvider) {
		this.user = user;
		this.uuid = uuid;
		this.command = command;
		this.cloudProvider = cloudProvider;
	}

	@Override
	public Boolean get() {
		try {
			run();
		} catch (Exception e) {
			LOGGER.error("Command with UUID {} fails: {}", uuid, e.getLocalizedMessage(), e);
			return false;
		}
		return true;
	}


	/**
	 * Return parser of command line.
	 */
	public CommandParserMock getParser() {
		return parser;
	}

	/**
	 * Return variables for substitution into Json response file.
	 */
	public Map<String, String> getVariables() {
		return parser.getVariables();
	}

	/**
	 * Response file name.
	 */
	public String getResponseFileName() {
		return responseFileName;
	}

	public void run() {
		LOGGER.debug("Run OS command for user {} with UUID {}: {}", user, uuid, command);

		responseFileName = null;
		parser = new CommandParserMock(command, uuid);
		LOGGER.debug("Parser is {}", parser);
		DockerAction action = DockerAction.of(parser.getAction());
		LOGGER.debug("Action is {}", action);

		if (parser.isDockerCommand()) {
			if (action == null) {
				throw new DlabException("Docker action not defined");
			}

			sleep(500);

			try {
				switch (action) {
					case DESCRIBE:
						describe();
						break;
					case CREATE:
					case START:
					case STOP:
					case TERMINATE:
					case GIT_CREDS:
					case CREATE_IMAGE:
						action(user, action);
						break;
					case CONFIGURE:
						sleep(1000);
						action(user, action);
						break;
					case STATUS:
						parser.getVariables().put("list_resources", getResponseStatus(true));
						action(user, action);
						break;
					case LIB_LIST:
						action(user, action);
						copyFile(String.format("mock_response/%s/notebook_lib_list_pkgs.json", cloudProvider.getName
                                        ()),
								"notebook_lib_list_pkgs.json", parser.getResponsePath());
						break;
					case LIB_INSTALL:
						parser.getVariables().put("lib_install", getResponseLibInstall(true));
						action(user, action);
						break;
					default:
						break;
				}
			} catch (Exception e) {
				String msg = "Cannot execute command for user " + user + " with UUID " + uuid + ". " +
						e.getLocalizedMessage();
				LOGGER.error(msg, e);
				throw new DlabException(msg, e);
			}
		} else {
			final String scriptName = StringUtils.substringBefore(Paths.get(parser.getCommand()).getFileName()
                    .toString(), ".");
			String templateFileName = "mock_response/" + cloudProvider.getName() + '/' + scriptName + JSON_FILE_ENDING;
			responseFileName = getAbsolutePath(parser.getResponsePath(), scriptName + user + "_" + parser.getRequestId
                    () + JSON_FILE_ENDING);
			setResponse(templateFileName, responseFileName);
		}

	}

	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			LOGGER.error("InterruptedException occurred: {}", e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	private static void copyFile(String sourceFilePath, String destinationFileName, String destinationFolder) throws
            IOException {
		File to = new File(getAbsolutePath(destinationFolder, destinationFileName));

		try (InputStream inputStream = CommandExecutorMockAsync.class.getClassLoader().getResourceAsStream
                (sourceFilePath);
			 OutputStream outputStream = new FileOutputStream(to)) {
			ByteStreams.copy(inputStream, outputStream);
		}

		LOGGER.debug("File {} copied to {}", sourceFilePath, to);
	}

	/**
	 * Return absolute path to the file or folder.
	 *
	 * @param first part of path.
	 * @param more  next path components.
	 */
	private static String getAbsolutePath(String first, String... more) {
		return Paths.get(first, more).toAbsolutePath().toString();
	}

	/**
	 * Tests the directory exists.
	 *
	 * @param dir the name of directory.
	 * @return <b>true</b> if the directory exists otherwise return <b>false</b>.
	 */
	private boolean dirExists(String dir) {
		File file = new File(dir);
		return (file.exists() && file.isDirectory());
	}

	/**
	 * Find and return the directory "infrastructure-provisioning/src".
	 *
	 * @throws FileNotFoundException
	 */
	private String findTemplatesDir() throws FileNotFoundException {
		String dir = System.getProperty("docker.dir");

		if (dir != null) {
			dir = getAbsolutePath(dir);
			if (dirExists(dir)) {
				return dir;
			}
			throw new FileNotFoundException("Directory \"" + dir + "\" not found. " +
					"Please set JVM argument -Ddocker.dir to the " +
					"\".../infrastructure-provisioning/src/general/files/" + cloudProvider.getName() + "\" directory");
		}
		dir = getAbsolutePath(
				".",
				"../../infrastructure-provisioning/src/general/files/" + cloudProvider.getName());
		if (dirExists(dir)) {
			return dir;
		}
		dir = getAbsolutePath(
				ServiceUtils.getUserDir(),
				"../../infrastructure-provisioning/src/general/files/" + cloudProvider.getName());
		if (dirExists(dir)) {
			return dir;
		}
		throw new FileNotFoundException("Directory \"" + dir + "\" not found. " +
				"Please set the value docker.dir property to the " +
				"\".../infrastructure-provisioning/src/general/files/" + cloudProvider.getName() + "\" directory");
	}

	/**
	 * Describe action.
	 */
	private void describe() {
		String templateFileName;
		try {
			templateFileName = getAbsolutePath(findTemplatesDir(), parser.getImageType() + "_description.json");
		} catch (FileNotFoundException e) {
			throw new DlabException("Cannot describe image " + parser.getImageType() + ". " + e.getLocalizedMessage(),
                    e);
		}
		responseFileName = getAbsolutePath(parser.getResponsePath(), parser.getRequestId() + JSON_FILE_ENDING);

		LOGGER.debug("Create response file from {} to {}", templateFileName, responseFileName);
		File fileResponse = new File(responseFileName);
		File fileTemplate = new File(templateFileName);
		try {
			if (!fileTemplate.exists()) {
				throw new FileNotFoundException("File \"" + fileTemplate + "\" not found.");
			}
			if (!fileTemplate.canRead()) {
				throw new IOException("Cannot read file \"" + fileTemplate + "\".");
			}
			Files.createParentDirs(fileResponse);
			Files.copy(fileTemplate, fileResponse);
		} catch (IOException e) {
			throw new DlabException("Can't create response file " + responseFileName + ": " + e.getLocalizedMessage(),
                    e);
		}
	}

	/**
	 * Perform docker action.
	 *
	 * @param user   the name of user.
	 * @param action docker action.
	 */
	private void action(String user, DockerAction action) {
		String resourceType = parser.getResourceType();

		String prefixFileName = (Lists.newArrayList("edge", "dataengine", "dataengine-service").contains
                (resourceType) ?
				resourceType : "notebook") + "_";
		String templateFileName = "mock_response/" + cloudProvider.getName() + '/' + prefixFileName + action.toString
                () + JSON_FILE_ENDING;
		responseFileName = getAbsolutePath(parser.getResponsePath(), prefixFileName + user + "_" + parser.getRequestId
                () + JSON_FILE_ENDING);
		setResponse(templateFileName, responseFileName);
	}

	/**
	 * Return the section of resource statuses for docker action status.
	 */
	private String getResponseStatus(boolean noUpdate) {
		if (noUpdate) {
			return "{}";
		}
		EnvResourceList resourceList;
		try {
			JsonNode json = MAPPER.readTree(parser.getJson());
			json = json.get("edge_list_resources");
			resourceList = MAPPER.readValue(json.toString(), EnvResourceList.class);
		} catch (IOException e) {
			throw new DlabException("Can't parse json content: " + e.getLocalizedMessage(), e);
		}

		if (resourceList.getHostList() != null) {
			for (EnvResource host : resourceList.getHostList()) {
				host.setStatus(UserInstanceStatus.RUNNING.toString());
			}
		}
		if (resourceList.getClusterList() != null) {
			for (EnvResource host : resourceList.getClusterList()) {
				host.setStatus(UserInstanceStatus.RUNNING.toString());
			}
		}

		try {
			return MAPPER.writeValueAsString(resourceList);
		} catch (JsonProcessingException e) {
			throw new DlabException("Can't generate json content: " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Return the section of resource statuses for docker action status.
	 */
	private String getResponseLibInstall(boolean isSuccess) {
		List<LibInstallDTO> list;
		try {
			JsonNode json = MAPPER.readTree(parser.getJson());
			json = json.get("libs");
			list = MAPPER.readValue(json.toString(), new TypeReference<List<LibInstallDTO>>() {
			});
		} catch (IOException e) {
			throw new DlabException("Can't parse json content: " + e.getLocalizedMessage(), e);
		}

		for (LibInstallDTO lib : list) {
			if (isSuccess) {
				lib.setStatus(LibStatus.INSTALLED.toString());
			} else {
				lib.setStatus(LibStatus.FAILED.toString());
				lib.setErrorMessage("Mock error message");
			}
		}

		try {
			return MAPPER.writeValueAsString(list);
		} catch (JsonProcessingException e) {
			throw new DlabException("Can't generate json content: " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Write response file.
	 *
	 * @param sourceFileName template file name.
	 * @param targetFileName response file name.
	 */
	private void setResponse(String sourceFileName, String targetFileName) {
		String content;
		URL url = Resources.getResource(sourceFileName);
		try {
			content = Resources.toString(url, Charsets.UTF_8);
		} catch (IOException e) {
			throw new DlabException("Can't read resource " + sourceFileName + ": " + e.getLocalizedMessage(), e);
		}

		for (String key : parser.getVariables().keySet()) {
			String value = parser.getVariables().get(key);
			content = content.replace("${" + key.toUpperCase() + "}", value);
		}

		File fileResponse = new File(responseFileName);
		try (BufferedWriter out = new BufferedWriter(new FileWriter(fileResponse))) {
			Files.createParentDirs(fileResponse);
			out.write(content);
		} catch (IOException e) {
			throw new DlabException("Can't write response file " + targetFileName + ": " + e.getLocalizedMessage(), e);
		}
		LOGGER.debug("Create response file from {} to {}", sourceFileName, targetFileName);
	}
}
