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

public enum DockerAction {
	DESCRIBE,
	CREATE,
	RECREATE,
	START,
	CONFIGURE,
	RUN,
	STOP,
	TERMINATE,
	LIB_LIST,
	LIB_INSTALL,
	GIT_CREDS,
	CREATE_IMAGE,
    TERMINATE_IMAGE,
	STATUS,
    REUPLOAD_KEY,
    RECONFIGURE_SPARK,
    CHECK_INACTIVITY;

    public static DockerAction of(String action) {
        if (action != null) {
            for (DockerAction uis : DockerAction.values()) {
                if (action.equalsIgnoreCase(uis.toString())) {
                    return uis;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
