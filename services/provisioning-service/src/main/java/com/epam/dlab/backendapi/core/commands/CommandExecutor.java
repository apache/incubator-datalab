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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandExecutor implements ICommandExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);

    @Override
    public List<String> executeSync(String command) throws IOException, InterruptedException {
        return execute(command);
    }

    @Override
    public void executeAsync(final String command) {
        CompletableFuture.runAsync(() -> execute(command));
    }

    private List<String> execute(String command) {
        try {
            LOGGER.debug("Execute command: {}", command);
            Process process = new ProcessBuilder(createCommand(command)).start();
            return readInputLines(process);
        } catch (Exception e) {
            LOGGER.error("execute command:", e);
        }
        return new ArrayList<>();
    }

    private List<String> readInputLines(Process process) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        }
        return result;
    }

    private String[] createCommand(String command) {
        return new String[]{"bash", "-c", command};
    }
}
