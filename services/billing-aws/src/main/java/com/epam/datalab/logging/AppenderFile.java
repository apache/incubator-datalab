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

package com.epam.datalab.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.DefaultTimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import com.epam.datalab.exceptions.InitializationException;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects.ToStringHelper;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * File appender for logging. Support rolling files and archiving.
 */
@JsonTypeName("file")
@JsonClassDescription(
        "File log appender.\n" +
                "Output log data to the file, if property archive is set to true then rolling\n" +
                "mode is enabled. If archivedLogFilenamePattern ends with .gz or .zip extension\n" +
                "then old log file will be compressed.\n" +
                "  - type: file\n" +
                "    currentLogFilename: <[path/]filename.log>  - pattern for log file naming.\n" +
                "    [archive: <true | false>]                  - rolling log files or none.\n" +
                "    [archivedLogFilenamePattern: <[path/]filename-%d{yyyy-MM-dd}.log[.gz | .zip]>]\n" +
                "                                               - pattern for naming the archive log\n" +
                "                                                 files.\n" +
                "    [archivedFileCount: <number_of_days>]      - number of archive log file history."
)
public class AppenderFile extends AppenderBase {

    /**
     * The name of current log file.
     */
    @Valid
    @NotNull
    @JsonProperty
    private String currentLogFilename;

    /**
     * Flag for archive of old files.
     */
    @Valid
    @JsonProperty
    private boolean archive = false;

    /**
     * Pattern for naming archive files. The compression mode depending on last
     * letters of the fileNamePatternStr. Patterns ending with .gz imply GZIP
     * compression, endings with '.zip' imply ZIP compression. Otherwise and by
     * default, there is no compression.
     */
    @Valid
    @JsonProperty
    private String archivedLogFilenamePattern;

    /**
     * The maximum number of archive files to keep..
     */
    @Valid
    @JsonProperty
    private int archivedFileCount = CoreConstants.UNBOUND_HISTORY;


    /**
     * Return the name of current log file.
     */
    public String getCurrentLogFilename() {
        return currentLogFilename;
    }

    /**
     * Set the name of current log file.
     */
    public void setCurrentLogFilename(String currentLogFilename) {
        this.currentLogFilename = currentLogFilename;
    }

    /**
     * Return the flag for archive of old files.
     */
    public boolean getArchive() {
        return archive;
    }

    /**
     * Set the flag for archive of old files.
     */
    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    /**
     * Return the pattern for naming archive files.
     */
    public String getArchivedLogFilenamePattern() {
        return archivedLogFilenamePattern;
    }

    /**
     * Set pattern for naming archive files. The compression mode depending on last
     * letters of the fileNamePatternStr. Patterns ending with .gz imply GZIP
     * compression, endings with '.zip' imply ZIP compression. Otherwise and by
     * default, there is no compression.
     * For example,
     * /logs/application-%d{yyyy-MM-dd}.log.gz
     */
    public void setArchivedLogFilenamePattern(String archivedLogFilenamePattern) {
        this.archivedLogFilenamePattern = archivedLogFilenamePattern;
    }

    /**
     * Return the maximum number of archive files to keep..
     */
    public int getArchivedFileCount() {
        return archivedFileCount;
    }

    /**
     * Set the maximum number of archive files to keep..
     */
    public void setArchivedFileCount(int archivedFileCount) {
        this.archivedFileCount = archivedFileCount;
    }


    @Override
    public void configure(LoggerContext context) throws InitializationException {
        if (currentLogFilename == null || currentLogFilename.trim().isEmpty()) {
            throw new InitializationException("Configuration property logging.appenders.currentLogFilename cannot be null.");
        }
        super.configure(context, "file-appender", (archive ? getRollingFileAppender(context) : getFileAppender()));
    }

    /**
     * Create and return synchronous the file appender.
     */
    private FileAppender<ILoggingEvent> getFileAppender() {
        FileAppender<ILoggingEvent> appender = new FileAppender<>();
        appender.setFile(currentLogFilename);
        appender.setAppend(true);
        return appender;
    }

    /**
     * Create and return synchronous the rolling file appender.
     *
     * @param context the context of logger.
     */
    private RollingFileAppender<ILoggingEvent> getRollingFileAppender(LoggerContext context) throws InitializationException {
        if (archivedLogFilenamePattern == null || archivedLogFilenamePattern.trim().isEmpty()) {
            throw new InitializationException("Configuration property logging.appenders.archivedLogFilenamePattern cannot be null.");
        }
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setFile(currentLogFilename);
        appender.setAppend(true);

        TimeBasedFileNamingAndTriggeringPolicy<ILoggingEvent> triggerPolicy = new DefaultTimeBasedFileNamingAndTriggeringPolicy<>();
        triggerPolicy.setContext(context);

        TimeBasedRollingPolicy<ILoggingEvent> rollPolicy = new TimeBasedRollingPolicy<>();
        rollPolicy.setContext(context);
        rollPolicy.setParent(appender);
        rollPolicy.setFileNamePattern(archivedLogFilenamePattern);
        rollPolicy.setMaxHistory(archivedFileCount);
        rollPolicy.setTimeBasedFileNamingAndTriggeringPolicy(triggerPolicy);
        rollPolicy.start();
        appender.setRollingPolicy(rollPolicy);

        return appender;
    }


    @Override
    public ToStringHelper toStringHelper(Object self) {
        return super.toStringHelper(self)
                .add("currentLogFilename", currentLogFilename)
                .add("archive", archive)
                .add("archivedLogFilenamePattern", archivedLogFilenamePattern)
                .add("archivedFileCount", archivedFileCount);
    }
}
