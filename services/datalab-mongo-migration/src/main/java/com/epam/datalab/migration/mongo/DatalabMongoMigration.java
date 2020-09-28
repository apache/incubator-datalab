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

package com.epam.datalab.migration.mongo;

import com.epam.datalab.migration.DbMigration;
import com.epam.datalab.migration.exception.DatalabDbMigrationException;
import com.epam.datalab.migration.mongo.changelog.DatalabChangeLog;
import com.github.mongobee.Mongobee;
import com.github.mongobee.exception.MongobeeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatalabMongoMigration implements DbMigration {
    private static final String MONGODB_URI_FORMAT = "mongodb://%s:%s@%s:%d/%s";
    private final Mongobee runner;

    public DatalabMongoMigration(String host, int port, String user, String password, String db) {
        runner = new Mongobee(String.format(MONGODB_URI_FORMAT, user, password, host, port, db));
        runner.setDbName(db);
        runner.setChangeLogsScanPackage(DatalabChangeLog.class.getPackage().getName());
    }

    public void migrate() {
        try {
            runner.execute();
        } catch (MongobeeException e) {
            log.error("Mongo db migration failed: {}", e.getMessage());
            throw new DatalabDbMigrationException("Mongo db migration failed", e);
        }
    }
}
