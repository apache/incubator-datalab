package com.epam.dlab.automation.docker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Docker {
    private static final Logger LOGGER = LogManager.getLogger(Docker.class);
    
    private static final String GET_CONTAINERS = "echo -e \"GET /containers/json?all=1 HTTP/1.0\\r\\n\" | nc -U /var/run/docker.sock";

    public static void checkDockerStatus(String containerName, String ip)
            throws IOException, JSchException, InterruptedException {
        
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
        LOGGER.debug("Docker container for {} is {}", containerName, MoreObjects.toStringHelper(Docker.class).addValue(dockerContainer));
        
        //TODO Check exit status and remove com.epam.dlab.automation.docker.Status
        Assert.assertEquals(dockerContainer.getStatus().contains(Status.EXITED_0.value()), true, "Status of container is not Exited (0)");
        
        LOGGER.info("Docker container {} has status Exited (0)", containerName);
    }

    private static List<DockerContainer> getDockerContainerList(InputStream in) throws IOException {
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));         
        String line;
        List<DockerContainer> dockerContainerList = null;

        TypeReference<List<DockerContainer>> typeRef = new TypeReference<List<DockerContainer>>() { };
        ObjectMapper mapper = new ObjectMapper();
        
        List<String> result = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {      
             result.add(line);
             if (line.contains("Id")) {
            	 LOGGER.debug("Add docker container: {}", line);
                 dockerContainerList = mapper.readValue(line, typeRef);
             }       
        }
        
        return dockerContainerList;
    }
    
    private static DockerContainer getDockerContainer(List<DockerContainer> dockerContainerList, String containerName) {
        LOGGER.debug("Search docker container for {}", containerName);
        for(Iterator<DockerContainer> i = dockerContainerList.iterator(); i.hasNext(); ) {
        	DockerContainer dockerContainer = i.next();
        	String name = dockerContainer.getNames().get(0);
            LOGGER.debug("  compare with {}", MoreObjects.toStringHelper(Docker.class).addValue(dockerContainer));
            if(name.contains(containerName)) {
            	LOGGER.debug("  container found");
                return dockerContainer;
            }
        }
        return null;
    }
}
