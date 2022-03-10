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

package com.epam.datalab.automation.docker;

import com.epam.datalab.automation.exceptions.DockerException;
import com.epam.datalab.automation.helper.ConfigPropertyValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Docker {
    private static final Logger LOGGER = LogManager.getLogger(Docker.class);
    
    private static final String GET_CONTAINERS = "echo -e \"GET /containers/json?all=1 HTTP/1.0\\r\\n\" | nc -U /var/run/docker.sock";
    
    private static final String DOCKER_STATUS_EXIT0 = "Exited (0)";

    private Docker(){}

    public static void checkDockerStatus(String containerName, String ip)
			throws IOException, InterruptedException, JSchException {
        
        LOGGER.info("Check docker status for instance {} and container {}", ip, containerName);
        if (ConfigPropertyValue.isRunModeLocal()) {
        	LOGGER.info("  check skipped for run in local mode");
        	return;
        }

        Session session = SSHConnect.getConnect(ConfigPropertyValue.getClusterOsUser(), ip, 22);
        ChannelExec getResult = SSHConnect.setCommand(session, GET_CONTAINERS);
        InputStream in = getResult.getInputStream();
        List<DockerContainer> dockerContainerList = getDockerContainerList(in);
        AckStatus status = SSHConnect.checkAck(getResult);
        Assert.assertTrue(status.isOk());
        
        DockerContainer dockerContainer = getDockerContainer(dockerContainerList, containerName);
        LOGGER.debug("Docker container for {} has id {} and status {}", containerName, dockerContainer.getId(), dockerContainer.getStatus());
        Assert.assertEquals(dockerContainer.getStatus().contains(DOCKER_STATUS_EXIT0), true, "Status of container is not Exited (0)");
        LOGGER.info("Docker container {} has status {}", containerName, DOCKER_STATUS_EXIT0);
    }

    private static List<DockerContainer> getDockerContainerList(InputStream in) throws IOException {
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));         
        String line;
        List<DockerContainer> dockerContainerList = null;

        TypeReference<List<DockerContainer>> typeRef = new TypeReference<List<DockerContainer>>() { };
        ObjectMapper mapper = new ObjectMapper();

		List<String> result = new ArrayList<>();
        while ((line = reader.readLine()) != null) {      
             result.add(line);
             if (line.contains("Id")) {
            	 LOGGER.trace("Add docker container: {}", line);
                 dockerContainerList = mapper.readValue(line, typeRef);
             }       
        }
        
        return dockerContainerList;
    }

	private static DockerContainer getDockerContainer(List<DockerContainer> dockerContainerList, String
			containerName) {
		for (DockerContainer dockerContainer : dockerContainerList) {
			String name = dockerContainer.getNames().get(0);
			if (name.contains(containerName)) {
				return dockerContainer;
			}
		}
        
        final String msg = "Docker container for " + containerName + " not found";
        LOGGER.error(msg);
		StringBuilder containers = new StringBuilder("Container list:");
		for (DockerContainer dockerContainer : dockerContainerList) {
			containers.append(System.lineSeparator()).append(dockerContainer.getNames().get(0));
		}
		LOGGER.debug(containers.toString());

		throw new DockerException("Docker container for " + containerName + " not found");
    }
}
