package com.epam.dlab.automation.docker;

import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Docker {
    private final static Logger LOGGER = LogManager.getLogger(Docker.class);

    public static void checkDockerStatus(String dockerImageName, String ip)
            throws IOException, JSchException, InterruptedException {
        
        LOGGER.info("Check docker status for instance {} and image {}", ip, dockerImageName);
        if (ConfigPropertyValue.isRunModeLocal()) {
        	LOGGER.info("  check is skipped");
        	return;
        }

        Session session = SSHConnect.getConnect(ConfigPropertyValue.getClusterOsUser(), ip, 22);
        ChannelExec getResult = SSHConnect.setCommand(session, DockerCommands.GET_CONTAINERS);
        InputStream in = getResult.getInputStream();
        List<DockerContainer> dockerContainerList = SSHConnect.getDockerContainerList(in);
        AckStatus status = SSHConnect.checkAck(getResult);
        Assert.assertTrue(status.isOk());
        
        DockerContainer dockerContainer = SSHConnect.getDockerContainer(dockerContainerList, dockerImageName);

        Assert.assertEquals(dockerContainer.getStatus().contains(Status.EXITED_0.value()), true, "Status of container is not Exited (0)");
        
        LOGGER.info("Docker image {} has status Exited (0)", dockerImageName);
    }

}
