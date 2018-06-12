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
import com.epam.dlab.process.model.ProcessId;
import com.epam.dlab.process.model.ProcessInfo;
import com.epam.dlab.process.builder.ProcessInfoBuilder;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CommandExecutorMock implements ICommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutorMock.class);
    public static final String DOCKER_DLAB_DATAENGINE = "docker.dlab-dataengine:latest";
    public static final String DOCKER_DLAB_DATAENGINE_SERVICE = "docker.dlab-dataengine-service:latest";

    private CommandExecutorMockAsync execAsync = null;
    private CompletableFuture<Boolean> future;

    private CloudProvider cloudProvider;

    public CommandExecutorMock(CloudProvider cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    /**
     * Return result of execution.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public boolean getResultSync() throws InterruptedException, ExecutionException {
        return (future == null ? true : future.get());
    }

    /**
     * Return variables for substitution into Json response file.
     */
    public Map<String, String> getVariables() {
        return (execAsync == null ? null : execAsync.getParser().getVariables());
    }

    /**
     * Response file name.
     */
    public String getResponseFileName() {
        return (execAsync == null ? null : execAsync.getResponseFileName());
    }

    @Override
    public ProcessInfo executeSync(String user, String uuid, String command) {
        LOGGER.debug("Run OS command for user {} with UUID {}: {}", user, uuid, command);
        ProcessInfoBuilder builder = new ProcessInfoBuilder(new ProcessId(user, command), 1000l);
        if (command.startsWith("docker images |")) {
            List<String> list = Lists.newArrayList(
                    "docker.dlab-deeplearning:latest",
                    "docker.dlab-jupyter:latest",
                    "docker.dlab-rstudio:latest",
                    "docker.dlab-tensor:latest",
                    "docker.dlab-zeppelin:latest");

            list.addAll(getComputationalDockerImage());

            ProcessInfoBuilder.stdOut(builder, String.join("\n", list));
        }
        return builder.get();
    }

    @Override
    public void executeAsync(String user, String uuid, String command) {
        execAsync = new CommandExecutorMockAsync(user, uuid, command, cloudProvider);
        future = CompletableFuture.supplyAsync(execAsync);
    }

    private List<String> getComputationalDockerImage() {
        switch (cloudProvider) {
            case AWS:
                return Lists.newArrayList(DOCKER_DLAB_DATAENGINE_SERVICE, DOCKER_DLAB_DATAENGINE);
            case AZURE:
                return Lists.newArrayList(DOCKER_DLAB_DATAENGINE);
            case GCP:
                return Lists.newArrayList(DOCKER_DLAB_DATAENGINE_SERVICE, DOCKER_DLAB_DATAENGINE);
            default:
                throw new IllegalArgumentException("Unsupported cloud provider " + cloudProvider);
        }
    }

}
