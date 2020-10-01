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

package com.epam.datalab.backendapi.dao;

import com.epam.datalab.backendapi.resources.dto.BackupInfoRecord;
import com.epam.datalab.dto.backup.EnvBackupDTO;
import com.epam.datalab.dto.backup.EnvBackupStatus;
import com.google.inject.Singleton;
import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.epam.datalab.backendapi.dao.MongoCollections.REQUEST_ID;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Singleton
public class BackupDAOImpl extends BaseDAO implements BackupDAO {
    @Override
    public void createOrUpdate(EnvBackupDTO dto, String user, EnvBackupStatus status) {
        final Document idField = backupId(user, dto.getId());
        final Document backupDocument = convertToBson(dto)
                .append(STATUS, status.name())
                .append(ERROR_MESSAGE, status.message())
                .append(TIMESTAMP, new Date())
                .append(ID, idField);
        updateOne(MongoCollections.BACKUPS,
                and(eq(ID, idField), eq(REQUEST_ID, dto.getId())),
                new Document(SET, backupDocument), true);
    }

    @Override
    public List<BackupInfoRecord> getBackups(String userName) {
        return find(MongoCollections.BACKUPS, eq(String.format("%s.%s", ID, USER), userName), BackupInfoRecord.class);
    }

    @Override
    public Optional<BackupInfoRecord> getBackup(String userName, String id) {
        return findOne(MongoCollections.BACKUPS, eq(ID, backupId(userName, id)), BackupInfoRecord.class);
    }

    private Document backupId(String user, String id) {
        return new Document(USER, user).append(REQUEST_ID, id);
    }

}
