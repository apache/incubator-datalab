package com.epam.dlab.backendapi.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class CommandExecutorMock implements ICommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutorMock.class);

    @Override
    public List<String> executeSync(String command) throws IOException, InterruptedException {
        LOGGER.debug("Mocked execution sync: {}", command);
        return new ArrayList<String>();
    }

    @Override
    public void executeAsync(String command) {
        LOGGER.debug("Mocked execution Async: {}", command);
    }
}
