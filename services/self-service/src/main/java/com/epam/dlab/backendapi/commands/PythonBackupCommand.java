package com.epam.dlab.backendapi.commands;

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
}
