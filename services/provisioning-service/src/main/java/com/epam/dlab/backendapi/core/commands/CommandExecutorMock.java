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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandExecutorMock implements ICommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutorMock.class);

    private CommandExecutorMockAsync execAsync = null;

	private CompletableFuture<Boolean> future;
    
    /** Return result of execution. 
     * @throws ExecutionException 
     * @throws InterruptedException */
    public boolean getResultSync() throws InterruptedException, ExecutionException {
    	return (future == null ? true : future.get());
    }
    
    /** Return variables for substitution into Json response file. */
    public Map<String, String> getVariables() {
    	return (execAsync == null ? null : execAsync.getParser().getVariables());
    }
    
    /** Response file name. */
    public String getResponseFileName() {
    	return (execAsync == null ? null : execAsync.getResponseFileName());
    }

    @Override
    public List<String> executeSync(String user, String uuid, String command) throws IOException, InterruptedException {
        LOGGER.debug("Run OS command for user {} with UUID {}: {}", user, uuid, command);
        if (command.startsWith("docker images |")) {
        	return Arrays.asList(
        			"docker.dlab-emr:latest",
            		"docker.dlab-jupyter:latest",
            		"docker.dlab-rstudio:latest",
            		"docker.dlab-tensor:latest",
            		"docker.dlab-zeppelin:latest");
        }
        return new ArrayList<String>();
    }

    @Override
    public void executeAsync(String user, String uuid, String command) {
    	execAsync = new CommandExecutorMockAsync(user, uuid, command);
    	future = CompletableFuture.supplyAsync(execAsync);
    }
    
}
