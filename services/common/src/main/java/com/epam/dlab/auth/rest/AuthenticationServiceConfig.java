/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.auth.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthenticationServiceConfig {
	@JsonProperty
	private String host = "localhost";
	@JsonProperty
	private int port = 0;
	@JsonProperty
	private String type = "http";
	@JsonProperty
	private String username = "";
	@JsonProperty
	private String password = "";

	@JsonProperty
	private String loginFormUrl = "/?";

	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(type == null || "".equals(type)) {
			sb.append("//");
		} else {
			sb.append(type).append("://");
		}
		if( username != null && ! "".equals(username)) {
			sb.append(username).append(":").append(password).append("@");
		}
		sb.append(host);
		if(port > 0) {
			sb.append(":").append(port);
		}
		return sb.toString();	}
	
	public String getAccessTokenUrl() {
		return toString()+"/validate?";
	}
	public String getAuthenticateAndRedirectUrl() {
		return toString()+"/login?";
	}
	public String getUserInfoUrl() {
		return toString()+"/user_info?";
	}
	public String getLogoutUrl() {
		return toString()+"/logout?";
	}
	public String getLoginUrl() {
		if("/?".equals(loginFormUrl)) {
			return toString()+"/?";
		} else {
			return loginFormUrl;
		}
	}
	
	public void setLoginUrl(String loginFormUrl) {
		this.loginFormUrl = loginFormUrl;
	}

}
