package com.epam.dlab.exceptions;

public class ResourceQuoteReachedException extends DlabException {
	public ResourceQuoteReachedException(String message) {
		super(message);
	}

	public ResourceQuoteReachedException(String message, Exception cause) {
		super(message, cause);
	}
}
