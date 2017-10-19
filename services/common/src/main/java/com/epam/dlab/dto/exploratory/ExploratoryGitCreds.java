package com.epam.dlab.dto.exploratory;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

/** Describe GIT credentials.
 */
public class ExploratoryGitCreds implements Comparable<ExploratoryGitCreds> {

	@NotNull
	@JsonProperty
	private String hostname;
	
	@NotNull
	@JsonProperty
	private String username;
	
	@NotNull
	@JsonProperty
	private String email;
	
	@NotNull
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
	public ExploratoryGitCreds withHostname(String hostname) {
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
	public ExploratoryGitCreds withUsername(String username) {
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
	public ExploratoryGitCreds withEmail(String email) {
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
	public ExploratoryGitCreds withLogin(String login) {
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
	public ExploratoryGitCreds withPassword(String password) {
		setPassword(password);
		return this;
	}
	
	@Override
    public int compareTo(ExploratoryGitCreds obj) {
    	if (obj == null) {
    		return 1;
    	}
    	return StringUtils.compareIgnoreCase(this.hostname, obj.hostname);
      }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		return (obj instanceof ExploratoryGitCreds ?
				(this.compareTo((ExploratoryGitCreds)obj) == 0) : false);

	}

	@Override
	public int hashCode() {
		return getHostname() != null ? getHostname().hashCode() : 0;
	}

	@Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("hostname", hostname)
    			.add("username", username)
    			.add("email", email)
    			.add("login", login)
    			.add("password", "***")
    			.toString();
    }
}
