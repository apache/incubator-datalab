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

package com.epam.datalab.backendapi.core.commands;

public class UnixCommand {
    private String command;

    public UnixCommand(String command) {
        this.command = command;
    }

    public static UnixCommand awk(String txt) {
        return new UnixCommand("awk '" + txt + "'");
    }

    public static UnixCommand sort() {
        return new UnixCommand("sort");
    }

    public static UnixCommand uniq() {
        return new UnixCommand("uniq");
    }

    public static UnixCommand grep(String searchFor, String... options) {
        StringBuilder sb = new StringBuilder("grep");
        for (String option : options) {
            sb.append(' ').append(option);
        }
        sb.append(" \"" + searchFor + "\"");
        return new UnixCommand(sb.toString());
    }

    public String getCommand() {
        return command;
    }
}