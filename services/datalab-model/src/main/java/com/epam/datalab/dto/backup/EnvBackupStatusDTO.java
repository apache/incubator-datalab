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

package com.epam.datalab.dto.backup;

import com.epam.datalab.dto.StatusBaseDTO;
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
