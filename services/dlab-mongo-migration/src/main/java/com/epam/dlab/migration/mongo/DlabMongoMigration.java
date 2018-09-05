/*
 * **************************************************************************
 *
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ***************************************************************************
 */

package com.epam.dlab.migration.mongo;

import com.epam.dlab.migration.DbMigration;
import com.epam.dlab.migration.exception.DlabDbMigrationException;
import com.epam.dlab.migration.mongo.changelog.DlabChangeLog;
import com.github.mongobee.Mongobee;
import com.github.mongobee.exception.MongobeeException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DlabMongoMigration implements DbMigration {
	private static final String MONGODB_URI_FORMAT = "mongodb://%s:%s@%s:%d/%s";
	private final Mongobee runner;

	public DlabMongoMigration(String host, int port, String user, String password, String db) {
		runner = new Mongobee(String.format(MONGODB_URI_FORMAT, user, password, host, port, db));
		runner.setDbName(db);
		runner.setChangeLogsScanPackage(DlabChangeLog.class.getPackage().getName());
	}

	public void migrate() {
		try {
			runner.execute();
		} catch (MongobeeException e) {
			log.error("Mongo db migration failed: {}", e.getMessage());
			throw new DlabDbMigrationException("Mongo db migration failed", e);
		}
	}
}
