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

package com.epam.datalab.migration.mongo.changelog;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@ChangeLog
@Slf4j
public class DatalabChangeLog {

    public static final String ID = "_id";

    @ChangeSet(order = "001", id = "001", author = "bhliva")
    public void migrateSchedulerFields(DB db) {
        log.info("Replacing field days_repeat with start_days_repeat and stop_days_repeat");
        final DBCollection userInstances = db.getCollection("userInstances");

        StreamSupport.stream(userInstances.find().spliterator(), false)
                .forEach(dbObject -> updateSchedulerFieldsForExploratory(userInstances, dbObject));
        log.info("Replacing scheduler field days_repeat finished successfully");
    }

    @SuppressWarnings("unchecked")
    private void updateSchedulerFieldsForExploratory(DBCollection userInstances, DBObject dbObject) {
        updateSchedulerFields(dbObject);
        Optional.ofNullable(dbObject.get("computational_resources")).map(cr -> (List<DBObject>) cr)
                .ifPresent(computationalResources -> computationalResources.forEach(this::updateSchedulerFields));
        userInstances.update(new BasicDBObject(ID, dbObject.get(ID)), dbObject);
    }

    private void updateSchedulerFields(DBObject dbObject) {
        final Object schedulerData = dbObject.get("scheduler_data");
        if (schedulerData != null) {
            final Object daysRepeat = ((DBObject) schedulerData).removeField("days_repeat");
            ((DBObject) schedulerData).put("start_days_repeat", daysRepeat);
            ((DBObject) schedulerData).put("stop_days_repeat", daysRepeat);
        }
    }
}
