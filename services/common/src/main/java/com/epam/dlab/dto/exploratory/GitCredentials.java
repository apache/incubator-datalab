package com.epam.dlab.dto.exploratory;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Describe GIT credentials.
 */
public class GitCredentials implements Comparable<GitCredentials> {

	@JsonProperty
	private String hostname;
	
	@JsonProperty
	private String username;
	
	@JsonProperty
	private String email;
	
	@JsonProperty
	private String login;
	
	@JsonProperty
	private String password;
	
	/** Return the name of host. */
	public String getHostname() {
		return hostname;
	}
	
	/** Set the name of host. */
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
	/** Set the name of host. */
	public GitCredentials withHostname(String hostname) {
		setHostname(hostname);
		return this;
	}

	/** Return the name of user. */
	public String getUsername() {
		return username;
	}
	
	/** Set the name of user. */
	public void setUsername(String username) {
		this.username = username;
	}
	
	/** Set the name of user. */
	public GitCredentials withUsername(String username) {
		setUsername(username);
		return this;
	}
	
	/** Return the email. */
	public String getEmail() {
		return email;
	}
	
	/** Set the email. */
	public void setEmail(String email) {
		this.email = email;
	}
	
	/** Set the email. */
	public GitCredentials withEmail(String email) {
		setEmail(email);
		return this;
	}
	
	/** Return the login. */
	public String getLogin() {
		return login;
	}
	
	/** Set the login. */
	public void setLogin(String login) {
		this.login = login;
	}
	
	/** Set the login. */
	public GitCredentials withLogin(String login) {
		setLogin(login);
		return this;
	}
	
	/** Return the password. */
	public String getPassword() {
		return password;
	}
	
	/** Set the password. */
	public void setPassword(String password) {
		this.password = password;
	}
	
	/** Set the password. */
	public GitCredentials withPassword(String password) {
		setPassword(password);
		return this;
	}
	
	@Override
    public int compareTo(GitCredentials obj) {
    	if (obj == null) {
    		return 1;
    	}

    	int result = StringUtils.compare(this.hostname, obj.hostname);
    	if (result != 0) {
    		return result;
    	}
    	result = StringUtils.compare(this.username, obj.username);
    	if (result != 0) {
    		return result;
    	}
    	result = StringUtils.compare(this.email, obj.email);
    	if (result != 0) {
    		return result;
    	}
    	result = StringUtils.compare(this.login, obj.login);
    	if (result != 0) {
    		return result;
    	}
    	return StringUtils.compare(this.password, obj.password);
      }
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof GitCredentials ?
				(this.compareTo((GitCredentials)obj) == 0) : false);

	}
}
