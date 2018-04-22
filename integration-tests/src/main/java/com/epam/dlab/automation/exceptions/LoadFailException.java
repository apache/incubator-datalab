package com.epam.dlab.automation.exceptions;

public class LoadFailException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LoadFailException(String message, Exception cause) {
		super(message, cause);
	}
}
