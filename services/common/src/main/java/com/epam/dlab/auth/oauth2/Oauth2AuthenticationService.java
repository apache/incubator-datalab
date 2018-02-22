package com.epam.dlab.auth.oauth2;

public interface Oauth2AuthenticationService {

	/**
	 * @return redirected ulr
	 */
	String getRedirectedUrl();

	/**
	 * Authorize user using oauth authorization code
	 *
	 * @param code  authorization code
	 * @param state state
	 * @return token
	 */
	String authorize(String code, String state);

}
