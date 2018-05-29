/*
Copyright 2016 EPAM Systems, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.epam.dlab.process.model;

import com.epam.dlab.process.ProcessConveyor;
import com.epam.dlab.process.builder.ProcessInfoBuilder;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DlabProcess {

    private final static Logger LOG = LoggerFactory.getLogger(DlabProcess.class);

    private final static DlabProcess INSTANCE = new DlabProcess();

    private ExecutorService executorService = Executors.newFixedThreadPool(50*3);
    private Map<String,ExecutorService> perUserService = new ConcurrentHashMap<>();
    private int userMaxparallelism = 5;
    private long expirationTime = TimeUnit.HOURS.toMillis(3);

    public static DlabProcess getInstance() {
        return INSTANCE;
    }

    private final ProcessConveyor processConveyor;

    private DlabProcess() {
        this.processConveyor = new ProcessConveyor();
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setMaxProcessesPerBox(int parallelism) {
        this.executorService.shutdown();
        this.executorService = Executors.newFixedThreadPool(3*parallelism);
    }

    public void setMaxProcessesPerUser(int parallelism) {
        this.userMaxparallelism = parallelism;
        this.perUserService.forEach((k,e)->{e.shutdown();});
        this.perUserService = new ConcurrentHashMap<>();
    }

    public ExecutorService getUsersExecutorService(String user) {
        perUserService.putIfAbsent(user,Executors.newFixedThreadPool(userMaxparallelism));
        return perUserService.get(user);
    }

    public CompletableFuture<ProcessInfo> start(ProcessId id, String... command){
    	LOG.debug("Run OS command for user {} with UUID {}: {}", id.getUser(), id.getCommand(), command);
        CompletableFuture<ProcessInfo> future = processConveyor.createBuildFuture( id, ()-> new ProcessInfoBuilder(id,expirationTime) );
        processConveyor.add(id, future, ProcessStep.FUTURE);
        processConveyor.add(id, command, ProcessStep.START);
        return future;
    }
    public CompletableFuture<ProcessInfo> start(String username, String uniqDescriptor, String... command){
        return start(new ProcessId(username,uniqDescriptor),command);
    }
    public CompletableFuture<ProcessInfo> start(String username, String... command){
        return start(new ProcessId(username,String.join(" ",command)),command);
    }


    public CompletableFuture<Boolean> stop(ProcessId id){
        return processConveyor.add(id,"STOP",ProcessStep.STOP);
    }
    public CompletableFuture<Boolean> stop(String username, String command){ return stop(new ProcessId(username,command));}

    public CompletableFuture<Boolean> kill(ProcessId id){
        return processConveyor.add(id,"KILL",ProcessStep.KILL);
    }
    public CompletableFuture<Boolean> kill(String username, String command){ return kill(new ProcessId(username,command));}

    public CompletableFuture<Boolean> failed(ProcessId id){
        return processConveyor.add(id,"FAILED",ProcessStep.FAILED);
    }

    public CompletableFuture<Boolean> finish(ProcessId id, Integer exitStatus){
        return processConveyor.add(id,exitStatus,ProcessStep.FINISH);
    }

    public CompletableFuture<Boolean> toStdOut(ProcessId id, String msg){
        return processConveyor.add(id,msg,ProcessStep.STD_OUT);
    }

    public CompletableFuture<Boolean> toStdErr(ProcessId id, String msg){
        return processConveyor.add(id,msg,ProcessStep.STD_ERR);
    }

    public CompletableFuture<Boolean> toStdErr(ProcessId id, String msg, Exception err){
        StringWriter sw = new StringWriter();
        sw.append(msg);
        sw.append("\n");
        PrintWriter pw = new PrintWriter(sw);
        err.printStackTrace(pw);
        return processConveyor.add(id,sw.toString(),ProcessStep.STD_ERR);
    }

    public Collection<ProcessId> getActiveProcesses() {
        Collection<ProcessId> pList = new ArrayList<>();
        processConveyor.forEachKeyAndBuilder( (k,b)-> pList.add(k) );
        return pList;
    }
    public Collection<ProcessId> getActiveProcesses(String username){
        return getActiveProcesses().stream().filter((id)->id.getUser().equals(username)).collect(Collectors.toList());
    }

    public Supplier<? extends ProcessInfo> getProcessInfoSupplier(ProcessId id) {
        return processConveyor.getInfoSupplier(id);
    }
    public Supplier<? extends ProcessInfo> getProcessInfoSupplier(String username, String command){
        return getProcessInfoSupplier(new ProcessId(username,command));
    }

    public void setProcessTimeout(long time, TimeUnit unit) {
        this.expirationTime = unit.toMillis(time);
    }
    public void setProcessTimeout(Duration duration) {
        this.expirationTime = duration.toMilliseconds();
    }

}
