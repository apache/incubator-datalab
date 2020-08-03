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

package com.epam.dlab.backendapi.util;

import java.util.List;

public class CSVFormatter {
	public static final char SEPARATOR = ',';

	private CSVFormatter() {
	}


	public static String formatLine(List<String> values, char separator) {
		boolean first = true;
		StringBuilder builder = new StringBuilder();
		for (String value : values) {
			if (!first) {
				builder.append(separator);
			}
			builder.append(followCsvStandard(value));
			first = false;
        }
        return builder.append(System.lineSeparator()).toString();
    }

    public static String formatLine(List<String> values, char separator, char customQuote) {
        boolean first = true;
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (!first) {
                builder.append(separator);
            }
            builder.append(customQuote).append(followCsvStandard(value)).append(customQuote);
            first = false;
        }
        return builder.append(System.lineSeparator()).toString();
    }

    private static String followCsvStandard(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;

    }

}
