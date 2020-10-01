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
import ch.qos.logback.core.ConsoleAppender;
import com.epam.datalab.exceptions.InitializationException;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Console appender for logging.
 */
@JsonTypeName("console")
@JsonClassDescription(
        "Console log appender.\n" +
                "Output log data to console. Does not have any properties.\n" +
                "  - type: console"
)
public class AppenderConsole extends AppenderBase {

    @Override
    public void configure(LoggerContext context) throws InitializationException {
        super.configure(context, "console-appender", new ConsoleAppender<ILoggingEvent>());
    }
}
