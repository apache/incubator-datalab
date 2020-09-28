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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.epam.datalab.exceptions.InitializationException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.TimeZone;

/**
 * Abstract class provides base configuration for the log appenders.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class AppenderBase {

    /**
     * Log format pattern.
     */
    private final String logFormatPattern = "%-5p [%d{ISO8601," + TimeZone.getDefault().getID() + "}] %c: %m%n%rEx";

    /**
     * Perform configure of appender.
     *
     * @param context the context of logger.
     */
    public abstract void configure(LoggerContext context) throws InitializationException;

    /**
     * Perform the base configure of appender.
     *
     * @param context      the context of logger.
     * @param appenderName the name of appender.
     * @param appender     the class instance of appender.
     */
    public void configure(LoggerContext context, String appenderName, OutputStreamAppender<ILoggingEvent> appender) {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern(logFormatPattern);
        encoder.setContext(context);
        encoder.start();

        appender.setContext(context);
        appender.setName(appenderName);
        appender.setEncoder(encoder);
        appender.start();

        Logger logger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logger.addAppender(appender);
        logger.setAdditive(true);
    }

    /**
     * Return the name of type for appender.
     */
    @JsonIgnore
    public String getType() {
        Class<? extends AppenderBase> clazz = this.getClass();
        return (clazz.isAnnotationPresent(JsonTypeName.class) ?
                clazz.getAnnotation(JsonTypeName.class).value() : clazz.getName());
    }


    /**
     * Returns a string representation of the object.
     *
     * @param self the object to generate the string for (typically this), used only for its class name.
     */
    public ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("type", getType());
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .toString();
    }
}
