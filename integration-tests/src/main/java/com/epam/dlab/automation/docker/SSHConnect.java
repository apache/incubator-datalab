package com.epam.dlab.automation.docker;

import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.epam.dlab.automation.helper.ConfigPropertyValue;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SSHConnect {
    private final static Logger LOGGER = LogManager.getLogger(SSHConnect.class);
    public static final String LOCALHOST_IP = "127.0.0.1";

    public static Session getConnect(String username, String host, int port) throws JSchException {
        Session session;
        JSch jsch = new JSch();

        Properties config = new Properties(); 
        config.put("StrictHostKeyChecking", "no");
        
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
        config.put("StrictHostKeyChecking", "no");

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
        ChannelSftp channelSftp =(ChannelSftp)channel;

        return channelSftp;
    }

    public static Session getForwardedConnect(String username, String hostAlias, int port) throws JSchException {
        Session session;
        JSch jsch = new JSch();
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");

        jsch.addIdentity(ConfigPropertyValue.getAccessKeyPrivFileName());
        session = jsch.getSession(username, LOCALHOST_IP, port);
        session.setConfig(config);
        session.setHostKeyAlias(hostAlias);
        session.connect();
        LOGGER.info("Getting connected to {} through {}:{}", hostAlias, LOCALHOST_IP, port);
        return session;
    }
    
    public static ChannelExec setCommand(Session session, String command)
            throws JSchException, IOException, InterruptedException {
        LOGGER.info("Setting command: {}", command);

        ChannelExec channelExec = (ChannelExec)session.openChannel("exec");
        channelExec.setCommand(command);
        channelExec.connect();

        return channelExec;
    }

    public static AckStatus checkAck(ChannelExec channel) throws IOException, InterruptedException {
        channel.setOutputStream(System.out, true);
    	channel.setErrStream(System.err, true);

        int status;
        while(channel.getExitStatus() == -1) {
            Thread.sleep(1000);
        }
        status = channel.getExitStatus();

        return new AckStatus(status, "");
    }

    public static AckStatus checkAck(ChannelSftp channel) throws IOException, InterruptedException {
        channel.setOutputStream(System.out, true);
//        channel.setErrStream(System.err, true);

        int status;
        while(channel.getExitStatus() == -1) {
            Thread.sleep(1000);
        }
        status = channel.getExitStatus();

        return new AckStatus(status, "");
    }

}
