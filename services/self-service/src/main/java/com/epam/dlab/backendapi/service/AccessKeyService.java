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

package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import org.keycloak.common.util.Base64Url;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

public interface AccessKeyService {

	KeyLoadStatus getUserKeyStatus(String user);

	String uploadKey(UserInfo user, String keyContent, boolean isPrimaryUploading);

	String recoverEdge(UserInfo userInfo);

	String generateKey(UserInfo userInfo, boolean createEdge);

	public static void main(String[] args) throws IOException {

		final String token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJNUC15QVpENFdJRzloanp3R0RqQjdCeW9aNGpaV05QTjJ3X25uS1BkTnQ4In0.eyJqdGkiOiJiY2ExNGIwNC00MmQ3LTRhMzUtOTlmMC05MzhiOTkzNGYyZmQiLCJleHAiOjE1NjI4MzU1OTQsIm5iZiI6MCwiaWF0IjoxNTYyODM1Mjk0LCJpc3MiOiJodHRwOi8vNTIuMTEuNDUuMTE6ODA4MC9hdXRoL3JlYWxtcy9ETEFCX2JobGl2YSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiJjOWViNTQ2MS04NWZjLTQ5ZTItYmZhNy05ODk1NGU0MTIyMGQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJzc3MiLCJhdXRoX3RpbWUiOjE1NjI4MzUyNjEsInNlc3Npb25fc3RhdGUiOiJiNTkyYTk5YS1lZWE2LTQ5OTMtYmRjNS1jYjlmN2E5ZmIyZjkiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsInByZWZlcnJlZF91c2VybmFtZSI6InRlc3QifQ.TOB5L1b5VV-jcYeZ5GwmJDCj5Vlm6yFhZS2WpkFdQb2FQ2VYF4ajsUfXPuDDxBFMv9Fi7fd2XRP7lb4tvXEMIYt8gZvc6OMLqUnGYUwYR4yrPOfHeApT4Ch5E9ePrnbuWJxOkgy2HeCGQO1TGHkN_2-ymWmsEDbKyW3Al2rMsgUJoeeMAe3FdKcqsPx9B2_mDaHnjz-TWEqStTudRed3v8F6x08urxTZ45xg4mKM_mdW5K5anLZbB4g4EIbZbztIbRPE2ItJbsxYidkiGQ6W6JJhg7PxKw46cl4V3nNeq7S_OPJ8diCxBmgpLKCtGm1ZE3GHA5EV6cA8yZIlm7BLmg";
		String[] parts = token.split("\\.");
		if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("Parsing error");

		byte[] bytes = Base64Url.decode(parts[1]);
		final IDToken idToken = JsonSerialization.readValue(bytes, IDToken.class);
		System.out.println(idToken.getPreferredUsername());
	}

}
