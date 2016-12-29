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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.UserCredentialDTO;
import io.dropwizard.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

public abstract class AbstractAuthenticationService<C extends Configuration> extends ConfigurableResource<C> {

	public final static String HTML_REDIRECT_HEAD = "<html><head><meta http-equiv=\"refresh\" content=\"0; url=%s\" /></head></html>";
	
	public final static String ACCESS_TOKEN_PARAMETER_PATTERN = "[&]?access_token=([^&]$|[^&]*)";

	public static String removeAccessTokenFromUrl(String url) {
		return url.replaceAll(ACCESS_TOKEN_PARAMETER_PATTERN, "").replace("?&", "?").replaceFirst("\\?$", "");
	}
	
	public static String addAccessTokenToUrl(String url, String token) {
		StringBuilder sb = new StringBuilder(url);
		if( ! url.contains("?")) {
			sb.append("?");
		} else {
			sb.append("&");			
		}
		sb.append("access_token=").append(token);
		return sb.toString();
	}
	
	public AbstractAuthenticationService(C config) {
		super(config);
	}

	public abstract Response login(UserCredentialDTO credential, HttpServletRequest request);
	public abstract UserInfo getUserInfo(String access_token, HttpServletRequest request);
	public abstract Response logout(String access_token);
	
	public static String getRandomToken() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
	
}
