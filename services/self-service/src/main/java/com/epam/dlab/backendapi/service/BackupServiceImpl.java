package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.commands.PythonBackupCommand;
import com.epam.dlab.command.ICommandExecutor;
import com.epam.dlab.dto.EnvBackupDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.process.ProcessInfo;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

@Slf4j
@Singleton
public class BackupServiceImpl implements BackupService {

    @Inject
    private SelfServiceApplicationConfiguration configuration;

    @Inject
    private ICommandExecutor commandExecutor;

    @Override
    public void createBackup(String user, EnvBackupDTO dto) {

        try {
            PythonBackupCommand pythonBackupCommand = new PythonBackupCommand(configuration.getBackupScriptPath())
                    .withConfig(dto.getConfigFiles())
                    .withJars(dto.getJars())
                    .withKeys(dto.getKeys())
                    .withDBBackup(dto.isDatabaseBackup())
                    .withLogsBackup(dto.isLogsBackup())
                    .withSystemUser();

            final ProcessInfo processInfo = commandExecutor.executeSync(user, UUID.randomUUID().toString(), pythonBackupCommand.toCMD());
            log.trace("Creating backup: {}", processInfo.getStdOut());
            if (StringUtils.isNoneBlank(processInfo.getStdErr())) {
                log.error("Can not create backup due to: {}", processInfo.getStdErr());
                throw new DlabException("Can not create backup. Please contact DLAB administrator");
            }

        } catch (Exception e) {
            log.error("Can not create backup for user {} due to: {}", user, e.getMessage());
            throw new DlabException(String.format("Can not create backup for user %s due to: %s", user, e.getMessage()), e);
        }
    }
}
