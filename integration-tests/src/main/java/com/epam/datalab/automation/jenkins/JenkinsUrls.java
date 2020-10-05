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

package com.epam.datalab.automation.jenkins;

public class JenkinsUrls {
	public static final String API = "api/xml";
	public static final String LAST_BUILD = "lastBuild";
	public static final String JSON_PRETTY = "/api/json?pretty=true";
	public static final String LOG_TEXT = "/logText/progressiveText?start=0";

	private JenkinsUrls() {
	}
}
