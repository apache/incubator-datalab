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

package com.epam.datalab.module;

import com.epam.datalab.core.AdapterBase;
import com.epam.datalab.core.parser.CommonFormat;
import com.epam.datalab.exceptions.AdapterException;
import com.epam.datalab.model.aws.ReportLine;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.bson.Document;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * The adapter for file system.
 */
@JsonTypeName(ModuleName.ADAPTER_FILE)
@JsonClassDescription(
        "File adapter.\n" +
                "Read source or write converted data to the file.\n" +
                "  - type: " + ModuleName.ADAPTER_FILE + "\n" +
                "    [writeHeader: <true | false>]  - write header of data to the adapterOut.\n" +
                "    file: <filename>               - the name of file."
)
public class AdapterFile extends AdapterBase {

    /**
     * The name of file.
     */
    @NotNull
    @JsonProperty
    private String file;
    /**
     * Reader for adapter.
     */
    @JsonIgnore
    private BufferedReader reader;
    /**
     * Writer for adapter.
     */
    @JsonIgnore
    private BufferedWriter writer;

    /**
     * Return the name of file.
     */
    public String getFile() {
        return file;
    }

    /**
     * Set the name of file.
     */
    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public void open() throws AdapterException {
        try {
            if (getMode() == Mode.READ) {
                reader = new BufferedReader(new FileReader(file));
            } else if (getMode() == Mode.WRITE) {
                writer = new BufferedWriter(new FileWriter(file));
            } else {
                throw new AdapterException("Mode of adapter unknown or not defined. Set mode to " + Mode.READ + " or " + Mode.WRITE + ".");
            }
        } catch (Exception e) {
            throw new AdapterException("Cannot open file " + file + ". " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void close() throws AdapterException {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                throw new AdapterException("Cannot close file " + file + ". " + e.getLocalizedMessage(), e);
            } finally {
                reader = null;
            }
        }

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new AdapterException("Cannot close file " + file + ". " + e.getLocalizedMessage(), e);
            } finally {
                writer = null;
            }
        }
    }

    @Override
    public String getEntryName() {
        return getFile();
    }

    @Override
    public String readLine() throws AdapterException {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new AdapterException("Cannot read file " + file + ". " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void writeHeader(List<String> header) throws AdapterException {
        try {
            writer.write(CommonFormat.rowToString(header));
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            throw new AdapterException("Cannot write file " + file + ". " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public Document writeRow(ReportLine row) throws AdapterException {
        try {
            writer.write(CommonFormat.rowToString(row));
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            throw new AdapterException("Cannot write file " + file + ". " + e.getLocalizedMessage(), e);
        }
        return null;
    }


    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("file", file);
    }
}
