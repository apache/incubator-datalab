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

import com.epam.datalab.automation.helper.ConfigPropertyValue;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

import static java.lang.System.err;
import static java.lang.System.out;

public class SSHConnect {
	private static final Logger LOGGER = LogManager.getLogger(SSHConnect.class);
	private static final String LOCALHOST_IP = ConfigPropertyValue.get("LOCALHOST_IP");
	private static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";

	private SSHConnect() {
	}

    public static Session getConnect(String username, String host, int port) throws JSchException {
        Session session;
        JSch jsch = new JSch();

        Properties config = new Properties();
		config.put(STRICT_HOST_KEY_CHECKING, "no");
        
        jsch.addIdentity(ConfigPropertyValue.getAccessKeyPrivFileName());
        session = jsch.getSession(username, host, port);
        session.setConfig(config);

        LOGGER.info("Connecting as {} to {}:{}", username, host, port);
        session.connect();

        LOGGER.info("Getting connected to {}:{}", host, port);
        return session;
    }

    public static Session getSession(String username, String host, int port) throws JSchException {
        Session session;
        JSch jsch = new JSch();

        Properties config = new Properties();
		config.put(STRICT_HOST_KEY_CHECKING, "no");

        jsch.addIdentity(ConfigPropertyValue.getAccessKeyPrivFileName());
        session = jsch.getSession(username, host, port);
        session.setConfig(config);
        session.connect();


        LOGGER.info("Getting connected to {}:{}", host, port);
        return session;
    }

    public static ChannelSftp getChannelSftp(Session session) throws JSchException {
        Channel channel = session.openChannel("sftp");
        channel.connect();
		return (ChannelSftp) channel;
    }

    public static Session getForwardedConnect(String username, String hostAlias, int port) throws JSchException {
        Session session;
        JSch jsch = new JSch();
        Properties config = new Properties();
		config.put(STRICT_HOST_KEY_CHECKING, "no");

        jsch.addIdentity(ConfigPropertyValue.getAccessKeyPrivFileName());
        session = jsch.getSession(username, LOCALHOST_IP, port);
        session.setConfig(config);
        session.setHostKeyAlias(hostAlias);
        session.connect();
        LOGGER.info("Getting connected to {} through {}:{}", hostAlias, LOCALHOST_IP, port);
        return session;
    }

	public static ChannelExec setCommand(Session session, String command) throws JSchException {
        LOGGER.info("Setting command: {}", command);

        ChannelExec channelExec = (ChannelExec)session.openChannel("exec");
        channelExec.setCommand(command);
        channelExec.connect();

        return channelExec;
    }

	public static AckStatus checkAck(ChannelExec channel) throws InterruptedException {
		channel.setOutputStream(out, true);
		channel.setErrStream(err, true);

        int status;
        while(channel.getExitStatus() == -1) {
            Thread.sleep(1000);
        }
        status = channel.getExitStatus();

        return new AckStatus(status, "");
    }

	public static AckStatus checkAck(ChannelSftp channel) throws InterruptedException {
		channel.setOutputStream(out, true);

        int status;
        while(channel.getExitStatus() == -1) {
            Thread.sleep(1000);
        }
        status = channel.getExitStatus();

        return new AckStatus(status, "");
    }

}
