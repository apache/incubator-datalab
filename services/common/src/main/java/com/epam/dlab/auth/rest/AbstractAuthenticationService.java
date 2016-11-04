/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/


package com.epam.dlab.auth.rest;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.UserCredentialDTO;

import io.dropwizard.Configuration;

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

	public UserInfo forgetAccessToken(String token) {
		return AuthorizedUsers.getInstance().removeUserInfo(token);
	}
	
	public UserInfo rememberUserInfo(String token, UserInfo user) {
		UserInfo ui = user.withToken(token);
		AuthorizedUsers.getInstance().addUserInfo(token, ui);
		return ui;
	}
	
	public boolean isAccessTokenAvailable(String token) {
		UserInfo ui = AuthorizedUsers.getInstance().getUserInfo(token);
		return ui != null;
	}
	
	public static String getRandomToken() {
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
	
}
