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
package com.epam.datalab.process.model;

import com.epam.datalab.process.ProcessConveyor;
import com.epam.datalab.process.builder.ProcessInfoBuilder;
import com.epam.datalab.util.SecurityUtils;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
public class DatalabProcess {

    private final static DatalabProcess INSTANCE = new DatalabProcess();
    private final ProcessConveyor processConveyor;
    private ExecutorService executorService = Executors.newFixedThreadPool(50 * 3);
    private Map<String, ExecutorService> perUserService = new ConcurrentHashMap<>();
    private int userMaxparallelism = 5;
    private long expirationTime = TimeUnit.HOURS.toMillis(3);

    private DatalabProcess() {
        this.processConveyor = new ProcessConveyor();
    }

    public static DatalabProcess getInstance() {
        return INSTANCE;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setMaxProcessesPerBox(int parallelism) {
        this.executorService.shutdown();
        this.executorService = Executors.newFixedThreadPool(3 * parallelism);
    }

    public void setMaxProcessesPerUser(int parallelism) {
        this.userMaxparallelism = parallelism;
        this.perUserService.forEach((k, e) -> e.shutdown());
        this.perUserService = new ConcurrentHashMap<>();
    }

    public ExecutorService getUsersExecutorService(String user) {
        perUserService.putIfAbsent(user, Executors.newFixedThreadPool(userMaxparallelism));
        return perUserService.get(user);
    }

    public CompletableFuture<ProcessInfo> start(ProcessId id, String... command) {
        log.debug("Run OS command for user {} with UUID {}: {}", id.getUser(), id.getCommand(),
                SecurityUtils.hideCreds(command));
        CompletableFuture<ProcessInfo> future = processConveyor.createBuildFuture(id, () -> new ProcessInfoBuilder(id,
                expirationTime));
        processConveyor.add(id, future, ProcessStep.FUTURE);
        processConveyor.add(id, command, ProcessStep.START);
        return future;
    }

    public CompletableFuture<ProcessInfo> start(String username, String uniqDescriptor, String... command) {
        return start(new ProcessId(username, uniqDescriptor), command);
    }

    public CompletableFuture<ProcessInfo> start(String username, String... command) {
        return start(new ProcessId(username, String.join(" ", command)), command);
    }


    public CompletableFuture<Boolean> stop(ProcessId id) {
        return processConveyor.add(id, "STOP", ProcessStep.STOP);
    }

    public CompletableFuture<Boolean> stop(String username, String command) {
        return stop(new ProcessId(username, command));
    }

    public CompletableFuture<Boolean> kill(ProcessId id) {
        return processConveyor.add(id, "KILL", ProcessStep.KILL);
    }

    public CompletableFuture<Boolean> kill(String username, String command) {
        return kill(new ProcessId(username, command));
    }

    public CompletableFuture<Boolean> failed(ProcessId id) {
        return processConveyor.add(id, "FAILED", ProcessStep.FAILED);
    }

    public CompletableFuture<Boolean> finish(ProcessId id, Integer exitStatus) {
        return processConveyor.add(id, exitStatus, ProcessStep.FINISH);
    }

    public CompletableFuture<Boolean> toStdOut(ProcessId id, String msg) {
        return processConveyor.add(id, msg, ProcessStep.STD_OUT);
    }

    public CompletableFuture<Boolean> toStdErr(ProcessId id, String msg) {
        return processConveyor.add(id, msg, ProcessStep.STD_ERR);
    }

    public CompletableFuture<Boolean> toStdErr(ProcessId id, String msg, Exception err) {
        StringWriter sw = new StringWriter();
        sw.append(msg);
        sw.append("\n");
        PrintWriter pw = new PrintWriter(sw);
        err.printStackTrace(pw);
        return processConveyor.add(id, sw.toString(), ProcessStep.STD_ERR);
    }

    public Collection<ProcessId> getActiveProcesses() {
        Collection<ProcessId> pList = new ArrayList<>();
        processConveyor.forEachKeyAndBuilder((k, b) -> pList.add(k));
        return pList;
    }

    public Collection<ProcessId> getActiveProcesses(String username) {
        return getActiveProcesses()
                .stream()
                .filter(id -> id.getUser().equals(username))
                .collect(Collectors.toList());
    }

    public Supplier<? extends ProcessInfo> getProcessInfoSupplier(ProcessId id) {
        return processConveyor.getInfoSupplier(id);
    }

    public Supplier<? extends ProcessInfo> getProcessInfoSupplier(String username, String command) {
        return getProcessInfoSupplier(new ProcessId(username, command));
    }

    public void setProcessTimeout(long time, TimeUnit unit) {
        this.expirationTime = unit.toMillis(time);
    }

    public void setProcessTimeout(Duration duration) {
        this.expirationTime = duration.toMilliseconds();
    }

}
