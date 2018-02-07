package com.epam.dlab.dto.backup;

import com.epam.dlab.dto.StatusBaseDTO;
import com.google.common.base.MoreObjects;
import lombok.Getter;

@Getter
public class EnvBackupStatusDTO extends StatusBaseDTO<EnvBackupStatusDTO> {

    private EnvBackupDTO envBackupDTO;
    private EnvBackupStatus envBackupStatus;


    public EnvBackupStatusDTO withEnvBackupDTO(EnvBackupDTO envBackupDTO) {
        this.envBackupDTO = envBackupDTO;
        return this;
    }

    public EnvBackupStatusDTO withStatus(EnvBackupStatus status) {
        this.envBackupStatus = status;
        return withStatus(status.name());
    }

    public EnvBackupStatusDTO withBackupFile(String backupFile) {
        if (envBackupDTO != null) {
            envBackupDTO.setBackupFile(backupFile);
        }
        return this;
    }

    @Override
    public MoreObjects.ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("envBackupStatus", envBackupStatus)
                .add("envBackupDTO", envBackupDTO);
    }
}
