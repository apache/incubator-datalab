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

package com.epam.datalab.core.parser;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Store the statistic of parser processing.
 */
public class ParserStatistics {
    /**
     * Name of parsed entry.
     */
    private final String entryName;

    /**
     * Time is milliseconds when parser has been started.
     */
    private long timeStartInMillis = 0;

    /**
     * Parsing time in milliseconds.
     */
    private long elapsedTimeInMillis = 0;

    /**
     * Number of rows read.
     */
    private long rowReaded;

    /**
     * Number of rows skipped.
     */
    private long rowSkipped;

    /**
     * Number of rows filtered.
     */
    private long rowFiltered;

    /**
     * Number of rows parsed.
     */
    private long rowParsed;

    /**
     * Number of rows write.
     */
    private long rowWritten;


    public ParserStatistics(String entryName) {
        this.entryName = entryName;
    }

    public void start() {
        timeStartInMillis = System.currentTimeMillis();
        elapsedTimeInMillis = 0;
        rowReaded = 0;
        rowSkipped = 0;
        rowFiltered = 0;
        rowParsed = 0;
        rowWritten = 0;
    }

    public void stop() {
        if (timeStartInMillis != 0) {
            elapsedTimeInMillis = System.currentTimeMillis() - timeStartInMillis;
            timeStartInMillis = 0;
        }
    }


    /**
     * Return the name of parsed entry.
     */
    public String getEntryName() {
        return entryName;
    }

    /**
     * Return the elapsed time in milliseconds of initializing, reading, filtering, parsing and writing operations.
     */
    public long getElapsedTime() {
        return (elapsedTimeInMillis != 0 ?
                elapsedTimeInMillis :
                timeStartInMillis == 0 ? 0 : System.currentTimeMillis() - timeStartInMillis);
    }

    /**
     * Return the number of rows read.
     */
    public long getRowReaded() {
        return rowReaded;
    }

    /**
     * Return the number of rows skipped.
     */
    public long getRowSkipped() {
        return rowSkipped;
    }

    /**
     * Return the number of rows filtered.
     */
    public long getRowFiltered() {
        return rowFiltered;
    }

    /**
     * Return the number of rows parsed.
     */
    public long getRowParsed() {
        return rowParsed;
    }

    /**
     * Return the number of rows write.
     */
    public long getRowWritten() {
        return rowWritten;
    }

    /**
     * Increment the number of rows read.
     */
    public void incrRowReaded() {
        rowReaded++;
    }

    /**
     * Increment the number of rows skipped.
     */
    public void incrRowSkipped() {
        rowSkipped++;
    }

    /**
     * Increment the number of rows filtered.
     */
    public void incrRowFiltered() {
        rowFiltered++;
    }

    /**
     * Increment the number of rows parsed.
     */
    public void incrRowParsed() {
        rowParsed++;
    }

    /**
     * Increment the number of rows write.
     */
    public void incrRowWritten() {
        rowWritten++;
    }


    public ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("entryName", entryName)
                .add("elapsedTime", getElapsedTime())
                .add("rowReaded", rowReaded)
                .add("rowSkipped", rowSkipped)
                .add("rowFiltered", rowFiltered)
                .add("rowParsed", rowParsed)
                .add("rowWritten", rowWritten);
    }

    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }
}
