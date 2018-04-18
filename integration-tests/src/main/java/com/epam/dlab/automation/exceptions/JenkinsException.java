package com.epam.dlab.automation.exceptions;

public class JenkinsException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JenkinsException(String message) {
		super(message);
	}
}
