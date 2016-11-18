/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.auth.rest;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserInfoTools {

	private final static Logger LOG = LoggerFactory.getLogger(UserInfoTools.class);

	private final static ObjectMapper om = new ObjectMapper();

	public static String toJson(UserInfo ui) {
		String json;
		try {
			json = om.writeValueAsString(ui);
			LOG.debug("UserInfo to JSON {} -> {}", ui, json);
		} catch (JsonProcessingException e) {
			LOG.error("UserInfo to JSON failed " + ui, e);
			throw new RuntimeException(e);
		}
		return json;
	}

	public static UserInfo toUserInfo(String json) {
		UserInfo ui;
		try {
			ui = om.readerFor(UserInfo.class).readValue(json);
			LOG.debug("JSON to UserInfo {} -> {}", json, ui);
		} catch (IOException e) {
			LOG.error("JSON to UserInfo failed " + json, e);
			throw new RuntimeException(e);
		}
		return ui;
	}
}
