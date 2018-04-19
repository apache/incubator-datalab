package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.resources.dto.BackupInfoRecord;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.backup.EnvBackupStatus;
import com.google.inject.Singleton;
import org.bson.Document;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.epam.dlab.backendapi.dao.MongoCollections.REQUEST_ID;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Singleton
public class BackupDaoImpl extends BaseDAO implements BackupDao {
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
