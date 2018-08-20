/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.epam.dlab.dto.computational;

import java.util.Arrays;

public enum CheckInactivityClusterStatus {

	COMPLETED("N/A"), FAILED("N/A");

	private String message;

	CheckInactivityClusterStatus(String message) {
		this.message = message;
	}

	public CheckInactivityClusterStatus withErrorMessage(String message) {
		this.message = message;
		return this;
	}

	public String message() {
		return message;
	}

	public static CheckInactivityClusterStatus fromValue(String value) {
		return Arrays.stream(values())
				.filter(v -> v.name().equalsIgnoreCase(value))
				.findAny()
				.orElseThrow(() ->
						new IllegalArgumentException("Wrong value for CheckInactivityClusterStatus: " + value));
	}
}
