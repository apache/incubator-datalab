package com.epam.dlab.automation.helper;

import com.epam.dlab.automation.docker.AckStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HelperMethods {

    private final static Logger LOGGER = LogManager.getLogger(HelperMethods.class);



    public static AckStatus executeCommand(String command) throws IOException, InterruptedException {
        LOGGER.info("Executing command: {}", command);
        Process process = Runtime.getRuntime().exec(new String[] { "bash", "-c", command });
        int status = process.waitFor();
        String message = "";
        if(status != 0) {
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
            		new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                    output.append(System.lineSeparator());
                }
            }
            message = output.toString();
        }
        return new AckStatus(status, message);
    }
    
}
