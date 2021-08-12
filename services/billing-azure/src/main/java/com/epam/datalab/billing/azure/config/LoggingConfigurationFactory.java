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

package com.epam.datalab.billing.azure.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.epam.datalab.billing.azure.logging.AppenderBase;
import com.epam.datalab.exceptions.InitializationException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.LoggerFactory;

/**
 * Configuration and factory for logging.
 */
public class LoggingConfigurationFactory {

    /**
     * Default logging level for all appenders.
     */
    @JsonProperty
    private Level level = Level.INFO;

    /**
     * List of logging levels for appenders.
     */
    @JsonIgnore
    private ImmutableMap<String, Level> loggers = ImmutableMap.of();

    /**
     * List of logging appenders.
     */
    @JsonProperty
    private ImmutableList<AppenderBase> appenders = ImmutableList.of();


    /**
     * Return the default logging level for all appenders.
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Set the default logging level for all appenders.
     */
    public void setLevel(String level) throws InitializationException {
        this.level = toLevel(level);
    }

    /**
     * Return the list of logging levels for appenders.
     */
    public ImmutableMap<String, Level> getLoggers() {
        return loggers;
    }

    /**
     * Set the list of logging levels for appenders.
     */
    @JsonProperty
    public void setLoggers(ImmutableMap<String, JsonNode> loggers) throws InitializationException {
        ImmutableMap.Builder<String, Level> levels = new ImmutableMap.Builder<>();
        for (String key : loggers.keySet()) {
            JsonNode node = loggers.get(key);
            levels.put(key, toLevel(node.asText()));
        }
        this.loggers = levels.build();
    }

    /**
     * Return the list of logging appenders.
     */
    public ImmutableList<AppenderBase> getAppenders() {
        return appenders;
    }

    /**
     * Set the list of logging appenders.
     */
    public void setAppenders(ImmutableList<AppenderBase> appenders) {
        this.appenders = appenders;
    }


    /**
     * Translate the name of logging level to {@link Level}.
     *
     * @param level the name of logging level.
     * @return logging level.
     * @throws InitializationException if given unknown logging level name.
     */
    private Level toLevel(String level) throws InitializationException {
        Level l = Level.toLevel(level, null);
        if (l == null) {
            throw new InitializationException("Unknown logging level: " + level);
        }
        return l;
    }

    /**
     * Configure logging appenders.
     *
     * @throws InitializationException
     */
    public void configure() throws InitializationException {
        if (appenders == null) {
            throw new InitializationException("Configuration property logging.appenders cannot be null.");
        }
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        for (AppenderBase appender : appenders) {
            appender.configure(context);
        }

        Logger logger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logger.setLevel(level);
        for (String name : loggers.keySet()) {
            logger = context.getLogger(name);
            logger.setLevel(loggers.get(name));
        }
    }


    /**
     * Returns a string representation of the object.
     *
     * @param self the object to generate the string for (typically this), used only for its class name.
     */
    public ToStringHelper toStringHelper(Object self) {
        return MoreObjects.toStringHelper(self)
                .add("level", level)
                .add("loggers", loggers)
                .add("appenders", appenders);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .toString();
    }
}
