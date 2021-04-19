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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ProcessInfo {

    private final ProcessId id;
    private final String[] command;
    private final ProcessStatus status;
    private final String stdOut;
    private final String stdErr;
    private final int exitCode;
    private final long startTimeStamp;
    private final long infoTimeStamp;
    private final int pid;

    private final Collection<ProcessInfo> rejectedCommands;

    public ProcessInfo(ProcessId id, ProcessStatus status, String[] command, String stdOut, String stdErr, int exitCode,
                       long startTimeStamp, long infoTimeStamp, Collection<ProcessInfo> rejected, int pid) {
        this.id = id;
        this.status = status;
        this.command = command;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
        this.exitCode = exitCode;
        this.startTimeStamp = startTimeStamp;
        this.infoTimeStamp = infoTimeStamp;
        this.pid = pid;

        if (rejected != null && rejected.size() > 0) {
            Collection<ProcessInfo> r = new ArrayList<>();
            for (ProcessInfo info : rejected) {
                if (info != null) {
                    r.add(info);
                }
            }
            this.rejectedCommands = Collections.unmodifiableCollection(r);
        } else {
            this.rejectedCommands = null;
        }

    }

    public String getCommand() {
        return String.join(" ", command);
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public String getStdOut() {
        return stdOut;
    }

    public String getStdErr() {
        return stdErr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    public long getInfoTimeStamp() {
        return infoTimeStamp;
    }

    public ProcessId getId() {
        return id;
    }

    public int getPid() {
        return pid;
    }

    public Collection<ProcessInfo> getRejectedCommands() {
        return Collections.unmodifiableCollection(rejectedCommands);
    }

    @Override
    public String toString() {
        return "ProcessInfo{" +
                "id='" + id + '\'' +
                ", command='" + getCommand() + '\'' +
                ", pid=" + pid +
                ", status=" + status +
                ", stdOut='" + stdOut + '\'' +
                ", stdErr='" + stdErr + '\'' +
                ", exitCode=" + exitCode +
                ", startTimeStamp=" + startTimeStamp +
                ", infoTimeStamp=" + infoTimeStamp +
                ", rejectedCommands=" + rejectedCommands +
                '}';
    }
}
