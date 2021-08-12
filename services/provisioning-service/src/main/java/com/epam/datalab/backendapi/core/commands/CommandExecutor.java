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

import com.epam.datalab.process.model.DatalabProcess;
import com.epam.datalab.process.model.ProcessId;
import com.epam.datalab.process.model.ProcessInfo;
import com.google.inject.Singleton;

@Singleton
public class CommandExecutor implements ICommandExecutor {

    public ProcessInfo executeSync(final String username, final String uuid, String command) throws Exception {
        return DatalabProcess.getInstance().start(new ProcessId(username, uuid), "bash", "-c", command).get();

    }

    public void executeAsync(final String username, final String uuid, final String command) {
        DatalabProcess.getInstance().start(new ProcessId(username, uuid), "bash", "-c", command);
    }
}