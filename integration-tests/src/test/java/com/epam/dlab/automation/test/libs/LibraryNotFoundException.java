package com.epam.dlab.automation.test.libs;

class LibraryNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	LibraryNotFoundException(String message) {
		super(message);
	}
}
