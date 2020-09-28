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

package com.epam.datalab.core;

import com.epam.datalab.exceptions.AdapterException;
import com.epam.datalab.model.aws.ReportLine;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.bson.Document;

import java.util.List;

/**
 * Abstract module for read/write adapter.
 * See description of {@link ModuleBase} how to create your own adapter.
 */
public abstract class AdapterBase extends ModuleBase {
    public enum Mode {READ, WRITE}

    ;

    /**
     * Flag the header of common format should be written to target.
     */
    @JsonProperty
    private boolean writeHeader = true;


    /**
     * The mode of adapter read or write.
     */
    @JsonIgnore
    private Mode mode;


    /**
     * Default constructor for deserialization.
     */
    public AdapterBase() {
    }

    /**
     * Instantiate adapter for reading or writing.
     *
     * @param mode the mode of adapter.
     */
    public AdapterBase(Mode mode) {
        this.mode = mode;
    }


    /**
     * Return <b>true</b> if the header of common format should be written to target.
     */
    public boolean isWriteHeader() {
        return writeHeader;
    }

    /**
     * Set flag the header of common format should be written to target.
     */
    public void setWriteHeader(boolean writeHeader) {
        this.writeHeader = writeHeader;
    }


    /**
     * Return the mode of adapter read or write.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Set the mode of adapter read or write.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }


    /**
     * Open connection.
     *
     * @throws AdapterException if cannot open connection.
     */
    public abstract void open() throws AdapterException;

    /**
     * Return <b>true</b> if adapter has the multiply entries of data.
     */
    public boolean hasMultyEntry() {
        return false;
    }

    /**
     * Return <b>true</b> if current entry has the data.
     */
    public boolean hasEntryData() {
        return true;
    }

    /**
     * Open next entry if exists and return <b>true</b> otherwise return <b>false</b>.
     *
     * @throws AdapterException if cannot open entry.
     */
    public boolean openNextEntry() throws AdapterException {
        return false;
    }

    /**
     * Close connection.
     *
     * @throws AdapterException if cannot close connection.
     */
    public abstract void close() throws AdapterException;

    /**
     * Return the current processed entry name.
     */
    public abstract String getEntryName();

    /**
     * Read the line of data from adapter and return it.
     *
     * @throws AdapterException
     */
    public abstract String readLine() throws AdapterException;

    /**
     * Write the header of data to adapter.
     *
     * @param header the header of common format.
     * @throws AdapterException
     */
    public abstract void writeHeader(List<String> header) throws AdapterException;

    /**
     * Write the row of data to adapter.
     *
     * @param row the row of common format.
     * @return
     * @throws AdapterException
     */
    public abstract Document writeRow(ReportLine row) throws AdapterException;


    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("mode", mode)
                .add("writeHeader", isWriteHeader());
    }
}
