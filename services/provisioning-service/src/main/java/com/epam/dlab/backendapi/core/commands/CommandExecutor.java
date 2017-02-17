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

import com.epam.dlab.backendapi.core.ICommandExecutor;
import com.epam.dlab.process.DlabProcess;
import com.epam.dlab.process.ProcessId;
import com.epam.dlab.process.ProcessInfo;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class CommandExecutor implements ICommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);

    public List<String> executeSync(final String username, final String uuid, String command) throws Exception {
        CompletableFuture<ProcessInfo> f = DlabProcess.getInstance().start(new ProcessId(username,uuid), "bash","-c",command);
        ProcessInfo pi = f.get();
        return Arrays.asList(pi.getStdOut().split("\n"));
    }

    public void executeAsync(final String username, final String uuid, final String command) {
        DlabProcess.getInstance().start(new ProcessId(username,uuid), "bash","-c",command);
    }

}
