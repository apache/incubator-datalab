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

import java.util.List;

public class PythonBackupCommand extends PythonCommand {

    private static final String ARG_DELIMITER = ",";
    private static final String USER_NAME_SYSTEM_PROPERTY = "user.name";

    public PythonBackupCommand(String fileName) {
        super(fileName);
    }

    public PythonBackupCommand withConfig(List<String> configs) {
        withOption("--config", String.join(ARG_DELIMITER, configs));
        return this;
    }

    public PythonBackupCommand withKeys(List<String> keys) {
        withOption("--keys", String.join(ARG_DELIMITER, keys));
        return this;
    }

    public PythonBackupCommand withJars(List<String> jars) {
        withOption("--jars", String.join(ARG_DELIMITER, jars));
        return this;
    }

    public PythonBackupCommand withDBBackup(boolean dbBackup) {
        if (dbBackup) {
            withOption("--db");
        }
        return this;
    }

    public PythonBackupCommand withCertificates(List<String> certificates) {
        withOption("--certs", String.join(ARG_DELIMITER, certificates));
        return this;
    }

    public PythonBackupCommand withSystemUser() {
        withOption("--user", System.getProperty(USER_NAME_SYSTEM_PROPERTY));
        return this;
    }

    public PythonBackupCommand withLogsBackup(boolean logsBackup) {
        if (logsBackup) {
            withOption("--logs");
        }
        return this;
    }

    public PythonBackupCommand withRequestId(String requestId) {
        withOption("--request_id", requestId);
        return this;
    }

    public PythonBackupCommand withResponsePath(String responsePath) {
        withOption("--result_path", responsePath);
        return this;
    }
}
