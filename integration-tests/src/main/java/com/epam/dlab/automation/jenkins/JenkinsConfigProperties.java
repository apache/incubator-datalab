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

public class JenkinsConfigProperties {

    static final long JENKINS_REQUEST_TIMEOUT = 5000;

	static final String AUTHORIZATION = "Authorization";
	static final String AUTHORIZATION_KEY = "Basic %s";//the replacement is decoded to base64 user:password

	static final String SUCCESS_STATUS = "true";
	static final String JENKINS_JOB_NAME_SEARCH = "/";

	static String jenkinsJobStartBody = "\"name=Access_Key_ID&value=%s" +
            "&name=Secret_Access_Key&value=%s" +
            "&name=Infrastructure_Tag&value=%s" +
            "name=OS_user&value=%s&name=Cloud_provider&value=aws&name=OS_family&value=%s&name=Action&value=create" +
            "&json=%7B%22parameter" +
            "%22%3A+%5B%7B%22name%22%3A+%22Access_Key_ID%22%2C+%22value%22%3A+%22%s" +
            "%22%7D%2C+%7B%22name%22%3A+%22Secret_Access_Key%22%2C+%22value%22%3A+%22%s" +
            "%22%7D%2C+%7B%22name%22%3A+%22Infrastructure_Tag%22%2C+%22value%22%3A+%22%s" +
            "%22%7D%2C+%7B%22name%22%3A+%22OS_user%22%2C+%22value%22%3A+%22%s" +
            "%22%7D%2C+%7B%22name%22%3A+%22Cloud_provider%22%2C+%22value%22%3A+%22aws" +
            "%22%7D%2C+%7B%22name%22%3A+%22OS_family%22%2C+%22value%22%3A+%22%s" +
            "%22%7D%2C+%7B%22name%22%3A+%22Action%22%2C+%22value%22%3A+%22create" +
            "%22%7D%5D%7D&Submit=Build";

	private JenkinsConfigProperties() {
	}
}
