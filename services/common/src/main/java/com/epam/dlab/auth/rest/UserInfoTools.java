/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

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
