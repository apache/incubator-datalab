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

package com.epam.datalab.backendapi.core.commands;

import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.process.builder.ProcessInfoBuilder;
import com.epam.datalab.process.model.ProcessId;
import com.epam.datalab.process.model.ProcessInfo;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CommandExecutorMock implements ICommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutorMock.class);
    public static final String DOCKER_DATALAB_DATAENGINE = "docker.datalab-dataengine:latest";
    public static final String DOCKER_DATALAB_DATAENGINE_SERVICE = "docker.datalab-dataengine-service:latest";

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
                    "docker.datalab-deeplearning:latest",
                    "docker.datalab-jupyter:latest",
                    "docker.datalab-jupyter-gpu:latest",
                    "docker.datalab-jupyterlab:latest",
                    "docker.datalab-superset:latest",
                    "docker.datalab-rstudio:latest",
                    "docker.datalab-tensor:latest",
                    "docker.datalab-zeppelin:latest",
                    "docker.datalab-tensor-rstudio:latest",
                    "docker.datalab-tensor-jupyterlab:latest");

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
                return Lists.newArrayList(DOCKER_DATALAB_DATAENGINE_SERVICE, DOCKER_DATALAB_DATAENGINE);
            case AZURE:
                return Lists.newArrayList(DOCKER_DATALAB_DATAENGINE, DOCKER_DATALAB_DATAENGINE_SERVICE);
            case GCP:
                return Lists.newArrayList(DOCKER_DATALAB_DATAENGINE_SERVICE, DOCKER_DATALAB_DATAENGINE);
            default:
                throw new IllegalArgumentException("Unsupported cloud provider " + cloudProvider);
        }
    }

}
